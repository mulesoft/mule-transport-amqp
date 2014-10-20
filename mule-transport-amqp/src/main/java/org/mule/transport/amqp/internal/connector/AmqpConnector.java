/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
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
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectException;
import org.mule.transport.amqp.internal.client.UrlEndpointURIParser;
import org.mule.transport.amqp.internal.connector.connection.*;
import org.mule.transport.amqp.internal.client.DispatchingReturnListener;
import org.mule.transport.amqp.internal.processor.ReturnHandler;
import org.mule.transport.amqp.internal.domain.AckMode;
import org.mule.transport.amqp.internal.transformer.AmqpMessageToObject;
import org.mule.transport.amqp.internal.domain.DeliveryMode;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.pool.impl.StackObjectPool;

/**
 * Connects to a particular virtual host on a particular AMQP broker.
 */
public class AmqpConnector extends AbstractConnector
{
    public static final String AMQP = "amqp";

    /**
     * Default number of consumer threads
     */
    public static final int DEFAULT_NUM_CONSUMER_THREADS = 4;

    // message properties names are consistent with AMQP spec
    // (cluster-id is deprecated and not supported here)
    public static final String APP_ID = "app-id";

    public static final String CONTENT_ENCODING = "content-encoding";

    public static final String CONTENT_TYPE = "content-type";

    public static final String CORRELATION_ID = "correlation-id";

    public static final String DELIVERY_MODE = "delivery_mode";

    public static final String DELIVERY_TAG = "delivery-tag";

    public static final String EXCHANGE = "exchange";

    public static final String EXPIRATION = "expiration";

    public static final String MESSAGE_ID = "message-id";

    public static final String CLUSTER_ID = "cluster-id";

    public static final String PRIORITY = "priority";

    public static final String REDELIVER = "redelivered";

    public static final String REPLY_TO = "reply-to";

    public static final String ROUTING_KEY = "routing-key";

    public static final String TIMESTAMP = "timestamp";

    public static final String TYPE = "type";

    public static final String USER_ID = "user-id";

    public static final String NEXT_PUBLISH_SEQ_NO = "nextPublishSequenceNo";

    // technical properties not intended to be messed with directly
    public static final String CONSUMER_TAG = "consumer-tag";

    // Connector properties

    public static final String RETURN_CONTEXT_PREFIX = AMQP + ".return.";

    public static final String RETURN_ROUTING_KEY = RETURN_CONTEXT_PREFIX + ROUTING_KEY;

    public static final String RETURN_EXCHANGE = RETURN_CONTEXT_PREFIX + EXCHANGE;

    public static final String RETURN_REPLY_TEXT = RETURN_CONTEXT_PREFIX + "reply-text";

    public static final String RETURN_REPLY_CODE = RETURN_CONTEXT_PREFIX + "reply-code";

    public static final String RETURN_LISTENER = AMQP + ".return.listener";

    public static final String AMQP_DELIVERY_TAG = AMQP + ".delivery-tag";

    public static final String CHANNEL = AMQP + ".channel";

    public static final String ALL_USER_HEADERS = AMQP + ".headers";


    private final Transformer receiveTransformer;

    private String host;
    private int port = ConnectionFactory.DEFAULT_AMQP_PORT;
    private int requestedHeartbeat = ConnectionFactory.DEFAULT_HEARTBEAT;
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
    private boolean requestBrokerConfirms = false;
    private int numberOfConsumers = DEFAULT_NUM_CONSUMER_THREADS;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private final StackObjectPool connectorConnectionPool;
    private UrlEndpointURIParser uriParser;


    public AmqpConnector(final MuleContext context)
    {
        super(context);

        receiveTransformer = new AmqpMessageToObject();
        receiveTransformer.setMuleContext(context);

        uriParser = new UrlEndpointURIParser();

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
            connectionFactory.setRequestedHeartbeat(requestedHeartbeat);
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
            setRequestedHeartbeat(connectionFactory.getRequestedHeartbeat());
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
                ExecutorService es = Executors.newFixedThreadPool(numberOfConsumers);
                connection = connectionFactory.newConnection(es);

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
            defaultReturnListener = ReturnHandler.DEFAULT_RETURN_LISTENER;
            return;
        }

        try
        {
            final MessageProcessor defaultReturnEndpoint = defaultReturnEndpointBuilder.buildOutboundEndpoint();
            defaultReturnListener = new DispatchingReturnListener(
                Collections.singletonList(defaultReturnEndpoint), new AmqpConnectorFlowConstruct(this));
            logger.info(String.format("Configured default return endpoint: %s", defaultReturnListener));
        }
        catch (final EndpointException ee)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Failed to configure default return endpoint"), ee, this);
        }
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
                    final String queueName = uriParser.getOrCreateQueue(
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
                    final String exchange = uriParser.getOrCreateExchange(
                            connectorConnection.getChannel(), outboundEndpoint, activeDeclarationsOnly);

                    final String routingKey = uriParser.getRoutingKey(outboundEndpoint, muleEvent);

                    if (StringUtils.isNotEmpty(uriParser.getQueueName(outboundEndpoint.getAddress()))
                        || outboundEndpoint.getProperties().containsKey(UrlEndpointURIParser.QUEUE_DURABLE)
                        || outboundEndpoint.getProperties().containsKey(UrlEndpointURIParser.QUEUE_AUTO_DELETE)
                        || outboundEndpoint.getProperties().containsKey(UrlEndpointURIParser.QUEUE_EXCLUSIVE))
                    {
                        uriParser.getOrCreateQueue(connectorConnection.getChannel(), outboundEndpoint,
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

    public void setDefaultReturnEndpoint(final EndpointBuilder defaultReturnEndpointBuilder)
    {
        this.defaultReturnEndpointBuilder = defaultReturnEndpointBuilder;
    }

    @Override
    public org.mule.api.transport.ReplyToHandler getReplyToHandler(final ImmutableEndpoint endpoint)
    {
        return new ReplyToHandler(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Channel createOperationResource(final ImmutableEndpoint endpoint) throws MuleException
    {
        try
        {
            return ChannelHandler.createChannel(this);
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

    public void setRequestBrokerConfirms(boolean requestBrokerConfirms)
    {
        this.requestBrokerConfirms = requestBrokerConfirms;
    }

    public boolean isRequestBrokerConfirms()
    {
        return requestBrokerConfirms;
    }

    public int getRequestedHeartbeat()
    {
        return requestedHeartbeat;
    }

    public void setRequestedHeartbeat(final int requestedHeartbeat)
    {
        this.requestedHeartbeat = requestedHeartbeat;
    }

    public ReturnListener getDefaultReturnListener()
    {
        return defaultReturnListener;
    }

    public int getNumberOfConsumers()
    {
        return numberOfConsumers;
    }

    public void setNumberOfConsumers(int numberOfConsumers)
    {
        this.numberOfConsumers = numberOfConsumers;
    }
}
