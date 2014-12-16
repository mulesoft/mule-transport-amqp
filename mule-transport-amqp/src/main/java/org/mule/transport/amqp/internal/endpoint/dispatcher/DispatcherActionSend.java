/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.dispatcher;

import com.rabbitmq.client.Channel;
import org.mule.transport.amqp.internal.client.AmqpDeclarer;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.mule.transport.amqp.internal.client.MessageConsumer;
import org.mule.util.StringUtils;

import java.io.IOException;

public class DispatcherActionSend extends DispatcherAction
{
    private DispatcherActionDispatch dispatcher = new DispatcherActionDispatch();

    protected MessageConsumer messageConsumer = new MessageConsumer();

    private final AmqpDeclarer declarator = new AmqpDeclarer();


    public AmqpMessage run(final AmqpConnector amqpConnector,
                           final Channel channel,
                           final String exchange,
                           final String routingKey,
                           final AmqpMessage amqpMessage,
                           final long timeout) throws IOException, InterruptedException
    {
        String replyTo = amqpMessage.getReplyTo();
        if (StringUtils.isEmpty(replyTo))
        {
            replyTo = declarator.declareTemporaryQueue(channel);
        }

        amqpMessage.setReplyTo(replyTo);
        dispatcher.run(amqpConnector, channel, exchange, routingKey, amqpMessage, timeout);

        return messageConsumer.consumeMessage(channel, replyTo, true, timeout);
    }

}
