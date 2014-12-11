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

import com.rabbitmq.client.Channel;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleMessage;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractReceiverWorker;
import org.mule.transport.amqp.internal.client.MessagePropertiesHandler;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

final class MessageReceiverWorker extends AbstractReceiverWorker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageReceiverWorker.class);

    private MultiChannelMessageSubReceiver messageReceiver;
    private final Channel channel;
    private final AmqpMessage amqpMessage;

    private MessagePropertiesHandler messagePropertiesHandler = new MessagePropertiesHandler();

    @SuppressWarnings({"unchecked", "rawtypes"})
    MessageReceiverWorker(MultiChannelMessageSubReceiver messageReceiver, final Channel channel, final AmqpMessage amqpMessage)
    {
        super(new ArrayList(1), messageReceiver);
        this.messageReceiver = messageReceiver;
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
        if ((messageReceiver.getEndpoint().getExchangePattern() == MessageExchangePattern.REQUEST_RESPONSE)
            && (muleMessage.getReplyTo() == null))
        {
            LOGGER.warn(String.format(
                    "Impossible to honor the request-response exchange pattern of %s for AMQP message without reply to: %s",
                    messageReceiver.getEndpoint(), muleMessage));
        }

        messagePropertiesHandler.addInvocationProperties(channel, amqpMessage, muleMessage,
                (AmqpConnector) messageReceiver.getConnector());
    }

    @Override
    protected void handleResults(@SuppressWarnings("rawtypes") final List messages) throws Exception
    {
        messagePropertiesHandler.ackMessageIfNecessary(channel, amqpMessage, endpoint);
    }
}
