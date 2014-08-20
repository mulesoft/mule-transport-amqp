/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.endpoint.receiver;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.StartException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.amqp.internal.client.UrlEndpointURIParser;
import org.mule.transport.amqp.internal.connector.ChannelHandler;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.connector.connection.InboundConnection;

import com.rabbitmq.client.Channel;
import org.mule.util.StringUtils;

/**
 * The <code>MessageReceiver</code> subscribes to a queue and dispatches received messages to
 * Mule.
 */
public class MessageReceiver extends AbstractMessageReceiver
{
    public static final String CONSUMER_TAG = "consumerTag";

    protected final AmqpConnector amqpConnector;
    protected volatile InboundConnection inboundConnection;
    protected volatile String consumerTag;

    public MessageReceiver(final Connector connector,
                           final FlowConstruct flowConstruct,
                           final InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        this.amqpConnector = (AmqpConnector) connector;
    }

    @Override
    public void doStart() throws MuleException
    {
        inboundConnection = amqpConnector.connect(this);

        if (logger.isDebugEnabled())
        {
            logger.debug("Connected queue: " + getQueueName() + " on channel: " + getChannel());
        }

        try
        {
            if (endpoint.getTransactionConfig().isTransacted())
            {
                getChannel().txSelect();
            }

            consumerTag = getChannel().basicConsume(getQueueName(), amqpConnector.getAckMode().isAutoAck(),
                getClientConsumerTag(), amqpConnector.isNoLocal(), amqpConnector.isExclusiveConsumers(),
                null, new MessageReceiverConsumer(this, getChannel()));

            logger.info("Started subscription: " + consumerTag + " on "
                        + (endpoint.getTransactionConfig().isTransacted() ? "transacted " : "") + "channel: "
                        + getChannel());
        }
        catch (final Exception e)
        {
            throw new StartException(
                    MessageFactory.createStaticMessage("Error when subscribing to queue: "
                          + getQueueName() + " on channel: " + getChannel()), e, this);
        }
    }

    @Override
    public void doStop()
    {
        Channel channel = null;

        try
        {
            channel = getChannel();
            if (channel == null)
            {
                return;
            }

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

            ChannelHandler.closeChannel(channel);
        }
        catch (final Exception e)
        {
            logger.warn(
                MessageFactory.createStaticMessage("Failed to cancel subscription: " + consumerTag
                                                   + " on channel: " + channel), e);
        }
        finally
        {
            inboundConnection = null;
        }
    }

    protected void restart(final boolean cancelSubscription)
    {
        if (!cancelSubscription)
        {
            // the subscription is considered already dead and won't be cancelled properly
            consumerTag = null;
        }

        try
        {
            doStop();
            doStart();
        }
        catch (final Exception e)
        {
            logger.error("Failed to restart: " + this, e);
        }
    }

    protected Channel getChannel()
    {
        return inboundConnection == null ? null : inboundConnection.getChannel();
    }

    protected String getQueueName()
    {
        return inboundConnection == null ? null : inboundConnection.getQueue();
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
