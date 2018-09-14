/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.dispatcher;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AmqpMessage;

import com.rabbitmq.client.Channel;

public class DispatcherActionDispatch extends DispatcherAction
{
    protected transient Log logger = LogFactory.getLog(getClass());

    public AmqpMessage run(final AmqpConnector amqpConnector,
                                final Channel channel,
                                final String exchange,
                                final String routingKey,
                                final AmqpMessage amqpMessage,
                                final long timeout) throws IOException
    {
        if (amqpConnector.isBlocked() && amqpConnector.isFailOnBlockedBroker())
        {
            logger.error("A blocking notification was received from the broker. Cannot publish message till unblocked." + amqpConnector.getBlockedReason());
            throw new AmqpBlockedBrokerException(amqpConnector.getBlockedReason());
        }
        channel.basicPublish(exchange, routingKey, amqpConnector.isMandatory(),
                amqpConnector.isImmediate(), amqpMessage.getProperties(), amqpMessage.getBody());

        return null;
    }
}
