/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connectable;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.MessageRequester;
import org.mule.api.transport.PropertyScope;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectException;
import org.mule.transport.amqp.AmqpConstants.AckMode;
import org.mule.transport.amqp.AmqpConstants.DeliveryMode;
import org.mule.transport.amqp.transformers.AmqpMessageToObject;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Connects to a particular virtual host on a particular AMQP broker.
 */
public class AmqpConnector extends AbstractConnector
{
    public static final String AMQP = "amqp";

    private final Transformer receiveTransformer;

    private String host;
    private int port = ConnectionFactory.DEFAULT_AMQP_PORT;
    private String[] fallbackAddresses;
    private String virtualHost;
    private String username;
    private String password;
    private DeliveryMode deliveryMode;
    private byte priority;
    private AckMode ackMode;
    private boolean activeDeclarationsOnly;
    private boolean mandatory;
    private boolean immediate;
    private ReturnListener defaultReturnListener;
    private EndpointBuilder defaultReturnEndpointBuilder;
    private int prefetchSize;
    private int prefetchCount;
    private boolean noLocal;
    private boolean exclusiveConsumers;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private final StackObjectPool connectorConnectionPool;

    /**
     * A queueing consumer that disconnects after receiving one message and actively rejects any
     * extra message received. <b>It must be used with a non auto-ack consumer</b>. It has the
     * advantage of using the consume semantics, including non-blocking time-out, without polling as
     * using basic get would do.
     */
    private static final class SingleMessageQueueingConsumer extends QueueingConsumer
    {
        private final AtomicBoolean received;

        private SingleMessageQueueingConsumer(final Channel channel)
        {
            super(channel);
            received = new AtomicBoolean();
        }

        @Override
        public void handleDelivery(final String consumerTag,
                                   final Envelope envelope,
                                   final BasicProperties properties,
                                   final byte[] body) throws IOException
        {
            if (received.get())
            {
                // extra messages could have been received before basicCancel happens -> actively
                // reject them
                getChannel().basicReject(envelope.getDeliveryTag(), true);
            }
            else
            {
                received.set(true);
                super.handleDelivery(consumerTag, envelope, properties, body);
                getChannel().basicCancel(consumerTag);
            }
        }
    }

    /**
     * A fake {@link FlowConstruct} that is used when the events need to be dispatched on behalf of
     * the connector and out of any actual Flow context.
     */
    protected static class AmqpConnectorFlowConstruct extends AbstractFlowConstruct
    {
        private final AmqpConnector connector;

        private AmqpConnectorFlowConstruct(final AmqpConnector connector)
        {
            super(connector.getName(), connector.getMuleContext());
            this.connector = connector;
        }

        @Override
        public String getConstructType()
        {
            return "Global AMQP Connector Fake Flow";
        }

        public AmqpConnector getConnector()
        {
            return connector;
        }
    }

    protected static abstract class AmqpConnection
    {
        private final Log logger = LogFactory.getLog(getClass());
        private final AmqpConnector amqpConnector;
        private final AtomicReference<Channel> channelRef = new AtomicReference<Channel>();

        private AmqpConnection(final AmqpConnector amqpConnector)
        {
            this.amqpConnector = amqpConnector;
        }

        private Channel newChannel()
        {
            try
            {
                final Channel channel = amqpConnector.createChannel();

                channel.addShutdownListener(new ShutdownListener()
                {
                    public void shutdownCompleted(final ShutdownSignalException sse)
                    {
                        if (sse.isInitiatedByApplication())
                        {
                            return;
                        }

                        // do not inform the connector of the issue as it can't
                        // decide what to do reset the channel so it would later
                        // be lazily reconnected
                        channelRef.set(null);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Terminated dead channel: " + channel, sse);
                        }
                    }
                });

                if (logger.isDebugEnabled())
                {
                    logger.debug("Shutdown listener configured on channel: " + channel);
                }

                return channel;
            }
            catch (final Exception e)
            {
                if ((!amqpConnector.isStopping()) && (amqpConnector.isStarted()))
                {
                    amqpConnector.getMuleContext()
                        .getExceptionListener()
                        .handleException(
                            new ConnectException(
                                MessageFactory.createStaticMessage("Impossible to create new channels on connection: "
                                                                   + amqpConnector.getConnection()), e,
                                amqpConnector));
                }
                return null;
            }
        }

        public AmqpConnector getAmqpConnector()
        {
            return amqpConnector;
        }

        public Channel getChannel()
        {
            Channel channel = channelRef.get();

            if (channel != null)
            {
                return channel;
            }

            channel = newChannel();

            if (channelRef.compareAndSet(null, channel))
            {
                return channel;
            }

            // race condition: use the channel created by another thread
            return getChannel();
        }

        @Override
        public String toString()
        {
            return super.toString() + ", Channel: " + getChannel();
        }
    }

    protected static class ConnectorConnection extends AmqpConnection
    {
        private ConnectorConnection(final AmqpConnector amqpConnector)
        {
            super(amqpConnector);
        }
    }

    public static class InboundConnection extends AmqpConnection
    {
        private final String queue;

        private InboundConnection(final AmqpConnector amqpConnector, final String queue)
        {
            super(amqpConnector);
            this.queue = queue;
        }

        public String getQueue()
        {
            return queue;
        }
    }

    public static class OutboundConnection extends AmqpConnection
    {
        private final String exchange;
        private final String routingKey;

        private OutboundConnection(final AmqpConnector amqpConnector,
                                   final String exchange,
                                   final String routingKey)
        {
            super(amqpConnector);
            this.exchange = exchange;
            this.routingKey = routingKey;
        }

        public String getExchange()
        {
            return AmqpEndpointUtil.isDefaultExchange(exchange) ? StringUtils.EMPTY : exchange;
        }

        public String getRoutingKey()
        {
            return routingKey;
        }

        public boolean canDispatch(final MuleEvent muleEvent, final OutboundEndpoint outboundEndpoint)
        {
            final String eventExchange = AmqpEndpointUtil.getExchangeName(outboundEndpoint, muleEvent);
            final String eventRoutingKey = AmqpEndpointUtil.getRoutingKey(muleEvent, outboundEndpoint);
            return StringUtils.equals(getExchange(), eventExchange)
                   && StringUtils.equals(getRoutingKey(), eventRoutingKey);
        }
    }

    protected static class ConnectorConnectionPoolableObjectFactory extends BasePoolableObjectFactory
    {
        private final AmqpConnector amqpConnector;

        private ConnectorConnectionPoolableObjectFactory(final AmqpConnector amqpConnector)
        {
            this.amqpConnector = amqpConnector;
        }

        @Override
        public Object makeObject() throws Exception
        {
            final ConnectorConnection connectorConnection = new ConnectorConnection(amqpConnector);
            if (amqpConnector.logger.isDebugEnabled())
            {
                amqpConnector.logger.debug("Created new: " + connectorConnection);
            }
            return connectorConnection;
        }

        @Override
        public boolean validateObject(final Object obj)
        {
            final Channel channel = ((ConnectorConnection) obj).getChannel();
            return channel != null && channel.isOpen();
        }

        @Override
        public void destroyObject(final Object obj) throws Exception
        {
            if (amqpConnector.logger.isDebugEnabled())
            {
                amqpConnector.logger.debug("Destroying " + obj);
            }

            try
            {
                final Channel channel = ((ConnectorConnection) obj).getChannel();
                if (channel != null)
                {
                    channel.close();
                }
            }
            catch (final Exception e)
            {
                amqpConnector.logger.info("Ignored exception when destroying ConnectorConnection:", e);
            }
        }
    }

    protected interface ConnectorConnectionAction<T>
    {
        public T run(ConnectorConnection connectorConnection) throws Exception;
    }

    public AmqpConnector(final MuleContext context)
    {
        super(context);

        receiveTransformer = new AmqpMessageToObject();
        receiveTransformer.setMuleContext(context);

        final int maxIdle = 1;
        final int initIdleCapacity = 0;
        connectorConnectionPool = new StackObjectPool(new ConnectorConnectionPoolableObjectFactory(this),
            maxIdle, initIdleCapacity);
    }

    @Override
    public void doInitialise() throws InitialisationException
    {
        if (connectionFactory == null)
        {
            connectionFactory = new ConnectionFactory();
            connectionFactory.setVirtualHost(virtualHost);
            connectionFactory.setUsername(username);
            connectionFactory.setPassword(password);
        }
        else
        {
            if (connectionFactory.getVirtualHost() != null)
            {
                setVirtualHost(connectionFactory.getVirtualHost());
            }
            else
            {
                connectionFactory.setVirtualHost(virtualHost);
            }
            setUsername(connectionFactory.getUsername());
            setPassword(connectionFactory.getPassword());
            setHost(connectionFactory.getHost());
            setPort(connectionFactory.getPort());
        }
    }

    @Override
    public void doConnect() throws Exception
    {
        final List<Address> brokerAddresses = new ArrayList<Address>();
        brokerAddresses.add(new Address(host, port));
        addFallbackAddresses(brokerAddresses);

        connectToFirstResponsiveBroker(brokerAddresses);

        configureDefaultReturnListener();
        // clear any connector connections that could have been created in a previous
        // connect() operation
        connectorConnectionPool.clear();
    }

    @Override
    public void doStart() throws MuleException
    {
        // NOOP
    }

    @Override
    public void doStop() throws MuleException
    {
        // NOOP
    }

    @Override
    public void doDisconnect() throws Exception
    {
        connectorConnectionPool.clear();
        connection.close();
    }

    @Override
    public void doDispose()
    {
        try
        {
            connectorConnectionPool.close();
        }
        catch (final Exception e)
        {
            logger.error("Can't close the connector connection pool", e);
        }
        connection = null;
        connectionFactory = null;
    }

    protected void addFallbackAddresses(final List<Address> brokerAddresses)
    {
        if (fallbackAddresses == null) return;

        for (final String fallbackAddress : fallbackAddresses)
        {
            final String[] fallbackAddressElements = StringUtils.splitAndTrim(fallbackAddress, ":");

            if (fallbackAddressElements.length == 2)
            {
                brokerAddresses.add(new Address(fallbackAddressElements[0],
                    NumberUtils.toInt(fallbackAddressElements[1])));
            }
            else if (fallbackAddressElements.length == 1)
            {
                brokerAddresses.add(new Address(fallbackAddressElements[0]));
            }
            else
            {
                logger.warn("Ignoring unparseable fallback address: " + fallbackAddress);
            }
        }
    }

    protected void connectToFirstResponsiveBroker(final List<Address> brokerAddresses) throws Exception
    {
        Exception lastException = null;

        for (final Address brokerAddress : brokerAddresses)
        {
            lastException = null;

            try
            {
                connectionFactory.setHost(brokerAddress.getHost());
                connectionFactory.setPort(brokerAddress.getPort());
                connection = connectionFactory.newConnection();

                connection.addShutdownListener(new ShutdownListener()
                {
                    public void shutdownCompleted(final ShutdownSignalException sse)
                    {
                        if (sse.isInitiatedByApplication())
                        {
                            return;
                        }

                        getMuleContext().getExceptionListener().handleException(
                            new ConnectException(
                                MessageFactory.createStaticMessage("Connection shutdown detected for: "
                                                                   + getName()), sse, AmqpConnector.this));
                    }
                });
                break;
            }
            catch (final Exception e)
            {
                lastException = e;
            }
        }

        if (lastException != null)
        {
            throw lastException;
        }
    }

    protected void configureDefaultReturnListener() throws InitialisationException
    {
        if (defaultReturnEndpointBuilder == null)
        {
            defaultReturnListener = AmqpReturnHandler.DEFAULT_RETURN_LISTENER;
            return;
        }

        try
        {
            final MessageProcessor defaultReturnEndpoint = defaultReturnEndpointBuilder.buildOutboundEndpoint();
            defaultReturnListener = new AmqpReturnHandler.DispatchingReturnListener(
                Collections.singletonList(defaultReturnEndpoint), new AmqpConnectorFlowConstruct(this));
            logger.info(String.format("Configured default return endpoint: %s", defaultReturnListener));
        }
        catch (final EndpointException ee)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Failed to configure default return endpoint"), ee, this);
        }
    }

    public static Long getDeliveryTagFromMessage(final MuleMessage message)
    {
        return message.getInvocationProperty(AmqpConstants.AMQP_DELIVERY_TAG,
            message.<Long> getInboundProperty(AmqpConstants.DELIVERY_TAG));
    }

    public static Channel getChannelFromMessage(final MuleMessage message)
    {
        return getChannelFromMessage(message, null);
    }

    public static Channel getChannelFromMessage(final MuleMessage message, final Channel defaultValue)
    {
        return message.getInvocationProperty(AmqpConstants.CHANNEL, defaultValue);
    }

    public InboundConnection connect(final MessageReceiver messageReceiver) throws ConnectException
    {
        return connect(messageReceiver, messageReceiver.getEndpoint());
    }

    public InboundConnection connect(final MessageRequester messageRequester) throws ConnectException
    {
        return connect(messageRequester, messageRequester.getEndpoint());
    }

    protected <T> T runConnectorConnectionAction(final ConnectorConnectionAction<T> action) throws Exception
    {
        ConnectorConnection connectorConnection = null;

        try
        {
            connectorConnection = (ConnectorConnection) connectorConnectionPool.borrowObject();
            return action.run(connectorConnection);
        }
        catch (final Exception e)
        {
            // whenever an exception occurs with a ConnectorConnection, consider the
            // object failed and invalidate it
            if (connectorConnection != null)
            {
                try
                {
                    connectorConnectionPool.invalidateObject(connectorConnection);
                }
                catch (final Exception e2)
                {
                    logger.error("Can't invalidate a borrowed connector connection", e2);
                }
            }

            throw e;
        }
        finally
        {
            if (connectorConnection != null)
            {
                try
                {
                    connectorConnectionPool.returnObject(connectorConnection);
                }
                catch (final Exception e)
                {
                    logger.error("Can't return a borrowed connector connection", e);
                }
            }
        }
    }

    protected InboundConnection connect(final Connectable connectable, final InboundEndpoint inboundEndpoint)
        throws ConnectException
    {
        try
        {
            return runConnectorConnectionAction(new ConnectorConnectionAction<InboundConnection>()
            {
                public InboundConnection run(final ConnectorConnection connectorConnection) throws Exception
                {
                    final String queueName = AmqpEndpointUtil.getOrCreateQueue(
                        connectorConnection.getChannel(), inboundEndpoint, activeDeclarationsOnly);
                    return new InboundConnection(connectorConnection.getAmqpConnector(), queueName);
                }
            });
        }
        catch (final Exception e)
        {
            throw new ConnectException(
                MessageFactory.createStaticMessage("Error when connecting inbound endpoint: "
                                                   + inboundEndpoint), e, connectable);
        }
    }

    public OutboundConnection connect(final MessageDispatcher messageDispatcher, final MuleEvent muleEvent)
        throws ConnectException
    {
        final OutboundEndpoint outboundEndpoint = messageDispatcher.getEndpoint();

        try
        {
            return runConnectorConnectionAction(new ConnectorConnectionAction<OutboundConnection>()
            {
                public OutboundConnection run(final ConnectorConnection connectorConnection) throws Exception
                {
                    final String exchange = AmqpEndpointUtil.getOrCreateExchange(
                        connectorConnection.getChannel(), outboundEndpoint, activeDeclarationsOnly);

                    final String routingKey = AmqpEndpointUtil.getRoutingKey(muleEvent, outboundEndpoint);

                    if (StringUtils.isNotEmpty(AmqpEndpointUtil.getQueueName(outboundEndpoint.getAddress()))
                        || outboundEndpoint.getProperties().containsKey(AmqpEndpointUtil.QUEUE_DURABLE)
                        || outboundEndpoint.getProperties().containsKey(AmqpEndpointUtil.QUEUE_AUTO_DELETE)
                        || outboundEndpoint.getProperties().containsKey(AmqpEndpointUtil.QUEUE_EXCLUSIVE))
                    {
                        AmqpEndpointUtil.getOrCreateQueue(connectorConnection.getChannel(), outboundEndpoint,
                            activeDeclarationsOnly, exchange, routingKey);
                    }

                    return new OutboundConnection(connectorConnection.getAmqpConnector(), exchange,
                        routingKey);
                }
            });
        }
        catch (final Exception e)
        {
            throw new ConnectException(
                MessageFactory.createStaticMessage("Error when connecting outbound endpoint: "
                                                   + outboundEndpoint), e, messageDispatcher);
        }
    }

    public AmqpMessage consumeMessage(final Channel channel,
                                      final String queue,
                                      final boolean autoAck,
                                      final long timeout) throws IOException, InterruptedException
    {
        final long startTime = System.currentTimeMillis();

        // try first with a basic get to potentially quickly retrieve a pending message
        final GetResponse getResponse = channel.basicGet(queue, autoAck);

        // if timeout is zero or if a message has been fetched don't go any further
        if ((timeout == 0) || (getResponse != null))
        {
            return getResponse == null ? null : new AmqpMessage(null, getResponse.getEnvelope(),
                getResponse.getProps(), getResponse.getBody());
        }

        // account for the time taken to perform the basic get
        final long elapsedTime = System.currentTimeMillis() - startTime;
        final long actualTimeOut = timeout - elapsedTime;
        if (actualTimeOut < 0)
        {
            return null;
        }

        final QueueingConsumer consumer = new SingleMessageQueueingConsumer(channel);

        // false -> no AMQP-level autoAck with the SingleMessageQueueingConsumer
        final String consumerTag = channel.basicConsume(queue, false, consumer);
        final Delivery delivery = consumer.nextDelivery(actualTimeOut);
        channel.basicCancel(consumerTag);

        if (delivery == null)
        {
            return null;
        }
        else
        {
            if (autoAck)
            {
                // ack only if auto-ack was requested, otherwise it's up to the caller to ack
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }

            return new AmqpMessage(consumerTag, delivery.getEnvelope(), delivery.getProperties(),
                delivery.getBody());
        }
    }

    public void ackMessageIfNecessary(final Channel channel,
                                      final AmqpMessage amqpMessage,
                                      final ImmutableEndpoint endpoint) throws IOException
    {
        if ((endpoint.getTransactionConfig().isTransacted()) || (getAckMode() != AckMode.MULE_AUTO))
        {
            return;
        }

        channel.basicAck(amqpMessage.getEnvelope().getDeliveryTag(), false);

        if (logger.isDebugEnabled())
        {
            logger.debug("Mule acknowledged message: " + amqpMessage + " on channel: " + channel);
        }
    }

    public void addInvocationPropertiesIfNecessary(final Channel channel,
                                                   final AmqpMessage amqpMessage,
                                                   final MuleMessage muleMessage)
    {
        if (getAckMode() == AckMode.MANUAL)
        {
            // in manual AckMode, the channel will be needed to ack the message
            muleMessage.setProperty(AmqpConstants.CHANNEL, channel, PropertyScope.INVOCATION);
            // so will the consumer tag (which is already added in the inbound properties
            // for the end user but that we also add here in the invocation scope for
            // internal needs)
            muleMessage.setProperty(AmqpConstants.AMQP_DELIVERY_TAG, amqpMessage.getEnvelope()
                .getDeliveryTag(), PropertyScope.INVOCATION);
        }
    }

    public Channel createChannel() throws IOException
    {
        final Channel channel = getConnection().createChannel();
        channel.addReturnListener(defaultReturnListener);
        channel.basicQos(getPrefetchSize(), getPrefetchCount(), false);

        if (logger.isDebugEnabled())
        {
            logger.debug("Created and configured new channel: " + channel);
        }

        return channel;
    }

    public void closeChannel(final Channel channel) throws ConnectException
    {
        if (channel == null)
        {
            return;
        }

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Closing channel: " + channel);
            }

            channel.close();

            if (logger.isDebugEnabled())
            {
                logger.debug("Closed channel: " + channel);
            }
        }
        catch (final Exception e)
        {
            logger.warn(MessageFactory.createStaticMessage("Failed to close channel: " + channel), e);
        }
    }

    public void setDefaultReturnEndpoint(final EndpointBuilder defaultReturnEndpointBuilder)
    {
        this.defaultReturnEndpointBuilder = defaultReturnEndpointBuilder;
    }

    @Override
    public ReplyToHandler getReplyToHandler(final ImmutableEndpoint endpoint)
    {
        return new AmqpReplyToHandler(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Channel createOperationResource(final ImmutableEndpoint endpoint) throws MuleException
    {
        try
        {
            return createChannel();
        }
        catch (final IOException ioe)
        {
            throw new DefaultMuleException(ioe);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object getOperationResourceFactory()
    {
        return this;
    }

    public Connection getConnection()
    {
        return connection;
    }

    public Transformer getReceiveTransformer()
    {
        return receiveTransformer;
    }

    public String getProtocol()
    {
        return AMQP;
    }

    public Byte getPriority()
    {
        return priority;
    }

    public void setPriority(final Byte priority)
    {
        this.priority = priority;
    }

    public AckMode getAckMode()
    {
        return ackMode;
    }

    public void setAckMode(final AckMode ackMode)
    {
        this.ackMode = ackMode;
    }

    public void setActiveDeclarationsOnly(final boolean activeDeclarationsOnly)
    {
        this.activeDeclarationsOnly = activeDeclarationsOnly;
    }

    public DeliveryMode getDeliveryMode()
    {
        return deliveryMode;
    }

    public void setDeliveryMode(final DeliveryMode deliveryMode)
    {
        this.deliveryMode = deliveryMode;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(final String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(final int port)
    {
        this.port = port;
    }

    public void setFallbackAddresses(final String[] fallbackAddresses)
    {
        this.fallbackAddresses = fallbackAddresses;
    }

    public void setVirtualHost(final String virtualHost)
    {
        this.virtualHost = virtualHost;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }

    public void setPassword(final String password)
    {
        this.password = password;
    }

    public boolean isImmediate()
    {
        return immediate;
    }

    public void setImmediate(final boolean immediate)
    {
        this.immediate = immediate;
    }

    public boolean isMandatory()
    {
        return mandatory;
    }

    public void setMandatory(final boolean mandatory)
    {
        this.mandatory = mandatory;
    }

    public int getPrefetchSize()
    {
        return prefetchSize;
    }

    public void setPrefetchSize(final int prefetchSize)
    {
        this.prefetchSize = prefetchSize;
    }

    public int getPrefetchCount()
    {
        return prefetchCount;
    }

    public void setPrefetchCount(final int prefetchCount)
    {
        this.prefetchCount = prefetchCount;
    }

    public boolean isNoLocal()
    {
        return noLocal;
    }

    public void setNoLocal(final boolean noLocal)
    {
        this.noLocal = noLocal;
    }

    public boolean isExclusiveConsumers()
    {
        return exclusiveConsumers;
    }

    public void setExclusiveConsumers(final boolean exclusiveConsumers)
    {
        this.exclusiveConsumers = exclusiveConsumers;
    }

    public void setConnectionFactory(final ConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    public ConnectionFactory getConnectionFactory()
    {
        return this.connectionFactory;
    }
}
