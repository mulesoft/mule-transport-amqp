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
import java.util.List;

import javax.resource.spi.work.WorkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.StartException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.AbstractReceiverWorker;
import org.mule.transport.amqp.AmqpConnector.InboundConnection;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * The <code>AmqpMessageReceiver</code> subscribes to a queue and dispatches received messages to
 * Mule.
 */
public class AmqpMessageReceiver extends AbstractMessageReceiver
{
    protected final AmqpConnector amqpConnector;
    protected volatile InboundConnection inboundConnection;
    protected volatile String consumerTag;

    public AmqpMessageReceiver(final Connector connector,
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
                null, new AmqpConsumer(getChannel()));

            logger.info("Started subscription: " + consumerTag + " on channel: " + getChannel());
        }
        catch (final Exception e)
        {
            throw new StartException(MessageFactory.createStaticMessage("Error when subscribing to queue: "
                                                                        + getQueueName() + " on channel: "
                                                                        + getChannel()), e, this);
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

            amqpConnector.closeChannel(channel);
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
        return AmqpEndpointUtil.getConsumerTag(getEndpoint());
    }

    private void deliverAmqpMessage(final AmqpMessage amqpMessage)
    {
        final AmqpWorker work = new AmqpWorker(getChannel(), amqpMessage);

        try
        {
            // deliver message in a different thread to free the Amqp Connector's
            // thread
            getWorkManager().scheduleWork(work);
        }
        catch (final WorkException we)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Failed to deliver: "
                                                                              + amqpMessage), we);
        }
    }

    public final class AmqpConsumer extends DefaultConsumer
    {
        public AmqpConsumer(final Channel channel)
        {
            super(channel);
        }

        @Override
        public void handleCancel(final String consumerTag) throws IOException
        {
            logger.warn("Received external cancellation of consumer tag: " + consumerTag
                        + ", the message receiver will try to restart.");

            restart(false);
        }

        @Override
        public void handleShutdownSignal(final String consumerTag, final ShutdownSignalException sig)
        {
            logger.warn("Received shutdown signal for consumer tag: " + consumerTag
                        + ", the message receiver will try to restart.", sig);

            restart(false);
        }

        @Override
        public void handleDelivery(final String consumerTag,
                                   final Envelope envelope,
                                   final AMQP.BasicProperties properties,
                                   final byte[] body) throws IOException
        {
            final AmqpMessage amqpMessage = new AmqpMessage(consumerTag, envelope, properties, body);

            if (logger.isDebugEnabled())
            {
                logger.debug("Received: " + amqpMessage + " from: " + super.getChannel());
            }

            deliverAmqpMessage(amqpMessage);
        }
    }

    private final class AmqpWorker extends AbstractReceiverWorker
    {
        private final Log logger = LogFactory.getLog(AmqpWorker.class);
        private final Channel channel;
        private final AmqpMessage amqpMessage;

        @SuppressWarnings({"unchecked", "rawtypes"})
        private AmqpWorker(final Channel channel, final AmqpMessage amqpMessage)
        {
            super(new ArrayList(1), AmqpMessageReceiver.this);
            messages.add(amqpMessage);
            this.channel = channel;
            this.amqpMessage = amqpMessage;
        }

        @Override
        protected void bindTransaction(final Transaction tx) throws TransactionException
        {
            if (channel != null)
            {
                tx.bindResource(channel.getConnection(), channel);
            }
            else
            {
                throw new TransactionException(
                    MessageFactory.createStaticMessage("Channel is null so can't bind transaction: " + tx
                                                       + " to message:" + amqpMessage));
            }
        }

        @Override
        protected void preRouteMuleMessage(final MuleMessage muleMessage) throws Exception
        {
            if ((getEndpoint().getExchangePattern() == MessageExchangePattern.REQUEST_RESPONSE)
                && (muleMessage.getReplyTo() == null))
            {
                logger.warn(String.format(
                    "Impossible to honor the request-response exchange pattern of %s for AMQP message without reply to: %s",
                    getEndpoint(), muleMessage));
            }

            amqpConnector.addInvocationPropertiesIfNecessary(channel, amqpMessage, muleMessage);
        }

        @Override
        protected void handleResults(@SuppressWarnings("rawtypes") final List messages) throws Exception
        {
            amqpConnector.ackMessageIfNecessary(channel, amqpMessage, endpoint);
        }
    }
}
