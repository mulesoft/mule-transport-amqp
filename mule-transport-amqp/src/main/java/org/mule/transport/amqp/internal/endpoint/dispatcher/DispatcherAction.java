/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.dispatcher;

import com.rabbitmq.client.Channel;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AmqpMessage;

import java.io.IOException;

public abstract class DispatcherAction
{

    public abstract AmqpMessage run(final AmqpConnector amqpConnector,
        final Channel channel,
        final String exchange,
        final String routingKey,
        final AmqpMessage amqpMessage,
        final long timeout) throws IOException, InterruptedException;

}
