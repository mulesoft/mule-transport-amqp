/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectException;
import org.mule.transport.amqp.internal.client.ChannelHandler;
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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Connects to a particular virtual host on a particular AMQP broker.
 */
public class AmqpConnector extends AbstractConnector
{
    /**
      * Default number of channels
      */
    public static final int DEFAULT_NUMBER_OF_CHANNELS = 1;

    public static final String NUMBER_OF_CHANNELS = "numberOfChannels";

    public static final String AMQP = "amqp";

    public static final String AMQP_DELIVERY_TAG = AMQP + ".delivery-tag";

    public static final String ALL_USER_HEADERS = AMQP + ".headers";

    // message properties names are consistent with AMQP spec
    // (cluster-id is deprecated and not supported here)
    public static final String MESSAGE_PROPERTY_APP_ID = "app-id";

    public static final String MESSAGE_PROPERTY_CHANNEL = AMQP + ".channel";

    public static final String MESSAGE_PROPERTY_CONTENT_ENCODING = "content-encoding";

    public static final String MESSAGE_PROPERTY_CONTENT_TYPE = "content-type";

    public static final String MESSAGE_PROPERTY_CORRELATION_ID = "correlation-id";

    public static final String MESSAGE_PROPERTY_DELIVERY_MODE = "delivery_mode";

    public static final String MESSAGE_PROPERTY_DELIVERY_TAG = "delivery-tag";

    public static final String EXCHANGE = "exchange";

    public static final String MESSAGE_PROPERTY_EXPIRATION = "expiration";

    public static final String MESSAGE_PROPERTY_MESSAGE_ID = "message-id";

    public static final String MESSAGE_PROPERTY_CLUSTER_ID = "cluster-id";

    public static final String MESSAGE_PROPERTY_PRIORITY = "priority";

    public static final String MESSAGE_PROPERTY_REDELIVER = "redelivered";

    public static final String MESSAGE_PROPERTY_REPLY_TO = "reply-to";

    public static final String MESSAGE_PROPERTY_ROUTING_KEY = "routing-key";

    public static final String MESSAGE_PROPERTY_TIMESTAMP = "timestamp";

    public static final String MESSAGE_PROPERTY_TYPE = "type";

    public static final String MESSAGE_PROPERTY_USER_ID = "user-id";

    public static final String MESSAGE_PROPERTY_NEXT_PUBLISH_SEQ_NO = "nextPublishSequenceNo";

    // technical properties not intended to be messed with directly
    public static final String MESSAGE_PROPERTY_CONSUMER_TAG = "consumer-tag";

    // Connector properties

    public static final String ENDPOINT_DEFAULT_EXCHANGE_ALIAS = "AMQP.DEFAULT.EXCHANGE";

    public static final String ENDPOINT_EXCHANGE_PREFIX = "amqp-exchange.";

    public static final String ENDPOINT_PROPERTY_ROUTING_KEY = "routingKey";

    public static final String ENDPOINT_PROPERTY_QUEUE_EXCLUSIVE = "queueExclusive";

    public static final String ENDPOINT_PROPERTY_QUEUE_AUTO_DELETE = "queueAutoDelete";

    public static final String ENDPOINT_PROPERTY_QUEUE_DURABLE = "queueDurable";

    public static final String ENDPOINT_PROPERTY_EXCHANGE_AUTO_DELETE = "exchangeAutoDelete";

    public static final String ENDPOINT_PROPERTY_EXCHANGE_DURABLE = "exchangeDurable";

    public static final String ENDPOINT_PROPERTY_EXCHANGE_TYPE = "exchangeType";

    public static final String ENDPOINT_QUEUE_PREFIX = "amqp-queue.";


    public static final String RETURN_CONTEXT_PREFIX = AMQP + ".return.";

    public static final String RETURN_ROUTING_KEY = RETURN_CONTEXT_PREFIX + ENDPOINT_PROPERTY_ROUTING_KEY;

    public static final String RETURN_EXCHANGE = RETURN_CONTEXT_PREFIX + EXCHANGE;

    public static final String RETURN_REPLY_TEXT = RETURN_CONTEXT_PREFIX + "reply-text";

    public static final String RETURN_REPLY_CODE = RETURN_CONTEXT_PREFIX + "reply-code";

    public static final String RETURN_LISTENER = AMQP + ".return.listener";

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
    private int numberOfChannels = DEFAULT_NUMBER_OF_CHANNELS;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private ChannelHandler channelHandler;

    private ExecutorService receiverExecutor;


    public AmqpConnector(final MuleContext context)
    {
        super(context);
        channelHandler = new ChannelHandler();
        receiveTransformer = new AmqpMessageToObject();
        receiveTransformer.setMuleContext(context);
    }

    @Override
    public void doInitialise() throws InitialisationException
    {
        receiverExecutor = this.getReceiverThreadingProfile().createPool("amqpReceiver");
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
        connection.close();
    }

    @Override
    public void doDispose()
    {
        connection = null;
        connectionFactory = null;
        receiverExecutor.shutdown();
        receiverExecutor = null;
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
                logger.debug("Connecting to AMQP host: " + brokerAddress.getHost() +
                        " and port: " + brokerAddress.getPort());
                connectionFactory.setHost(brokerAddress.getHost());
                connectionFactory.setPort(brokerAddress.getPort());
                connection = connectionFactory.newConnection(receiverExecutor);

                connection.addShutdownListener(new ShutdownListener()
                {
                    public void shutdownCompleted(final ShutdownSignalException sse)
                    {
                        if (sse.isInitiatedByApplication())
                        {
                            return;
                        }

                        forceReconnect("Connection shutdown detected for: " + getName(), sse);
                    }
                });
                logger.info("Connected to AMQP host: " + brokerAddress.getHost() +
                        " and port: " + brokerAddress.getPort());
                break;
            }
            catch (final SocketTimeoutException e)
            {
                logger.error("Error occurred when connecting to AMQP host: " + brokerAddress.getHost() +
                             " and port: " + brokerAddress.getPort(), e);

                lastException = new SocketTimeoutExceptionWrapper("SocketTimeoutException was triggered. Please check your network status.");
            }
            catch (final Exception e)
            {
                logger.error("Error occurred when connecting to AMQP host: " + brokerAddress.getHost() +
                        " and port: " + brokerAddress.getPort(), e);
                lastException = e;
            }
        }

        if (lastException != null)
        {
            throw lastException;
        }
    }

    public void forceReconnect(String message, Exception e) {
        // Only attempt to reconnect if the connector is in started and non stopping mode to prevent
        // stacking more than one reconnection attempts
        if (getLifecycleState().isStarted() && (!getLifecycleState().isStopping()))
        {
            muleContext.getExceptionListener()
                    .handleException(new ConnectException(MessageFactory.createStaticMessage(message), e, this));
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

    public void setDefaultReturnEndpoint(final EndpointBuilder defaultReturnEndpointBuilder)
    {
        this.defaultReturnEndpointBuilder = defaultReturnEndpointBuilder;
    }

    @Override
    public org.mule.api.transport.ReplyToHandler getReplyToHandler(final ImmutableEndpoint endpoint)
    {
        return new ReplyToHandler(this, endpoint);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Channel createOperationResource(final ImmutableEndpoint endpoint) throws MuleException
    {
        try
        {
            return channelHandler.getOrCreateChannel(endpoint);
        }
        catch (final Exception ioe)
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

    public boolean isActiveDeclarationsOnly()
    {
        return activeDeclarationsOnly;
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

    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    public void setNumberOfChannels(int numberOfChannels) {
        this.numberOfChannels = numberOfChannels;
    }

    public ChannelHandler getChannelHandler() {
        return channelHandler;
    }

    public void setChannelHandler(ChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
    }

    private class SocketTimeoutExceptionWrapper extends IOException
    {
        public SocketTimeoutExceptionWrapper(String message)
        {
            super(message);
        }
    }
}
