/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import com.rabbitmq.client.*;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class MessageReceiverConsumer extends DefaultConsumer
{
    private static final Logger logger = LoggerFactory.getLogger(MessageReceiverConsumer.class);

    private MultiChannelMessageSubReceiver messageReceiver;

    public MessageReceiverConsumer(MultiChannelMessageSubReceiver messageReceiver, final Channel channel)
    {
        super(channel);
        this.messageReceiver = messageReceiver;
    }

    @Override
    public void handleCancel(final String consumerTag) throws IOException
    {
        logger.warn("Received external cancellation of consumer tag: " + consumerTag
                + ", the message receiver will try to restart.");

        messageReceiver.restart(false);
    }

    @Override
    public void handleShutdownSignal(final String consumerTag, final ShutdownSignalException sig)
    {
        logger.warn("Received shutdown signal for consumer tag: " + consumerTag
                + ", the message receiver will try to restart.", sig);

        messageReceiver.restart(false);
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


    protected void deliverAmqpMessage(final AmqpMessage amqpMessage)
    {
        final MessageReceiverWorker work = new MessageReceiverWorker(messageReceiver,
                messageReceiver.getChannel(), amqpMessage);

        try
        {
            work.processMessages();
        }
        catch (final Exception e)
        {
            messageReceiver.getConnector().getMuleContext().getExceptionListener().handleException(e);
        }
    }
}
