/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ReturnListener;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AmqpMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractAmqpReturnHandlerListener implements ReturnListener
{

    public void handleReturn(final int replyCode,
                             final String replyText,
                             final String exchange,
                             final String routingKey,
                             final AMQP.BasicProperties properties,
                             final byte[] body) throws IOException
    {
        final String errorMessage = String.format(
            "AMQP returned message with code: %d, reason: %s, exchange: %s, routing key: %s", replyCode,
            replyText, exchange, routingKey);

        final Map<String, Object> returnContext = new HashMap<String, Object>(4);
        returnContext.put(AmqpConnector.RETURN_REPLY_CODE, replyCode);
        returnContext.put(AmqpConnector.RETURN_REPLY_TEXT, replyText);
        returnContext.put(AmqpConnector.RETURN_EXCHANGE, exchange);
        returnContext.put(AmqpConnector.RETURN_ROUTING_KEY, routingKey);

        final AmqpMessage returnedAmqpMessage = new AmqpMessage(null, null, properties, body);

        doHandleReturn(errorMessage, returnContext, returnedAmqpMessage);
    }

    protected abstract void doHandleReturn(String errorMessage,
                                           Map<String, Object> returnContext,
                                           AmqpMessage returnedAmqpMessage);

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
