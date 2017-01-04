/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import com.rabbitmq.client.Channel;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.StartException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.endpoint.AmqpEndpointUtil;
import org.mule.util.StringUtils;

/**
 * In Mule an endpoint corresponds to a single receiver. It's up to the receiver to do multithreaded consumption and
 * resource allocation, if needed. This class honors the <code>numberOfConcurrentTransactedReceivers</code> strictly
 * and will create exactly this number of consumers.
 */
public class MultiChannelMessageSubReceiver extends AbstractMessageReceiver
{
    public static final String CONSUMER_TAG = "consumerTag";

    protected final MultiChannelMessageReceiver parentReceiver;
    protected final AmqpConnector amqpConnector;
    protected final ImmutableEndpoint endpoint;
    protected final AmqpEndpointUtil endpointUtil;
    protected volatile String consumerTag;
    protected Channel channel;
    protected String queueName;


    public MultiChannelMessageSubReceiver(MultiChannelMessageReceiver parentReceiver) throws CreateException
    {
        super(parentReceiver.getConnector(), parentReceiver.getFlowConstruct(), parentReceiver.getEndpoint());
        this.parentReceiver = parentReceiver;
        amqpConnector = (AmqpConnector) parentReceiver.getConnector();
        endpoint = parentReceiver.getEndpoint();
        endpointUtil = new AmqpEndpointUtil();
    }

    @Override
    public void doStart() throws MuleException
    {
        try
        {
        	logger.debug("Starting subreceiver on queue: " + getQueueName() + " on channel: " + getChannel());

            super.doStart();

            channel = amqpConnector.getChannelHandler().getOrCreateChannel(endpoint);
            parentReceiver.declareEndpoint(channel);

            if (logger.isDebugEnabled())
            {
                logger.debug("Connected queue: " + getQueueName() + " on channel: " + getChannel());
            }

            if (endpoint.getTransactionConfig().isTransacted())
            {
                channel.txSelect();
            }

            queueName = parentReceiver.getQueueOrCreateTemporaryQueue(channel);

            consumerTag = channel.basicConsume(getQueueName(), amqpConnector.getAckMode().isAutoAck(),
                    getClientConsumerTag(), amqpConnector.isNoLocal(), amqpConnector.isExclusiveConsumers(),
                    null, new MessageReceiverConsumer(this, channel));

            logger.info("Started subscription: " + consumerTag + " on "
                    + (endpoint.getTransactionConfig().isTransacted() ? "transacted " : "") + "channel: "
                    + channel);
        }
        catch (final Exception e)
        {
            try
            {
                amqpConnector.stop();
            }
            catch (MuleException w)
            {
                logger.error("Unable to stop amqp connector");
            }
            throw new StartException(
                    MessageFactory.createStaticMessage("Error when subscribing to queue: "
                            + getQueueName() + " on channel: " + channel), e, this);
        }
        logger.debug("Started subreceiver on queue: " + getQueueName() + " on channel: " + getChannel());
    }

    @Override
    public void doStop()
    {
        logger.debug("Stopping subreceiver " + getQueueName() + " on channel: " + getChannel());
        try
        {
            if (channel == null)
            {
                return;
            }

            super.doStop();

            if (consumerTag != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Cancelling subscription of: " + consumerTag + " on channel: " + channel);
                }

                channel.basicCancel(consumerTag);

                logger.info("Cancelled subscription of: " + consumerTag + " on channel: " + channel);
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Disconnecting receiver for queue: " + getQueueName() + " from channel: "
                        + channel);
            }

            amqpConnector.getChannelHandler().closeChannel(channel);
        }
        catch (final Exception e)
        {
            logger.warn(
                    MessageFactory.createStaticMessage("Failed to cancel subscription: " + consumerTag
                            + " on channel: " + channel), e);
        }
        finally
        {
            logger.debug("Stopped subreceiver " + getQueueName() + " on channel: " + getChannel());

        }
    }

    /**
     * Attempts to restart this consumer only. If an error happens on retry, a full reconnection will be forced to
     * restart the cycle of declarations.
     * @param cancelSubscription defines if the subscriptions has to be canceled or not
     */
    protected void restart(final boolean cancelSubscription)
    {
        if (!cancelSubscription)
        {
            // the subscription is considered already dead and won't be cancelled properly
            consumerTag = null;
        }

        try
        {
            stop();
            start();
        }
        catch (final Exception e)
        {
            logger.error("Failed to restart receiver: " + this, e);
            amqpConnector.forceReconnect("Unresolvable receiver problem, forcing a reconnection", e);
        }
    }

    public Channel getChannel()
    {
        return channel;
    }

    protected String getQueueName()
    {
        return queueName;
    }

    protected String getClientConsumerTag()
    {
        return getConsumerTag(getEndpoint());
    }

    private String getConsumerTag(final ImmutableEndpoint endpoint)
    {
        return StringUtils.defaultString((String) endpoint.getProperty(CONSUMER_TAG));
    }

}
