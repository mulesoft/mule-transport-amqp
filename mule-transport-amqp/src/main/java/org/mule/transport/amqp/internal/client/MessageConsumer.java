/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.RpcServer.RpcConsumer;

import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.mule.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MessageConsumer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    public AmqpMessage consumeMessage(final Channel channel,
                                      final String queue,
                                      final boolean autoAck,
                                      final long timeout) throws IOException, InterruptedException
    {
        final long startTime = System.currentTimeMillis();

        // try first with a basic get to potentially quickly retrieve a pending message
        final GetResponse getResponse = channel.basicGet(queue, autoAck);

        // if timeout is zero or if a message has been fetched don't go any further
        if ((timeout == 0) || (getResponse != null))
        {
            return getResponse == null ? null : new AmqpMessage(null, getResponse.getEnvelope(),
                    getResponse.getProps(), getResponse.getBody());
        }

        // account for the time taken to perform the basic get
        final long elapsedTime = System.currentTimeMillis() - startTime;
        final long actualTimeOut = timeout - elapsedTime;
        if (actualTimeOut < 0)
        {
            return null;
        }

        final QueueingConsumer consumer = new SingleMessageQueueingConsumer(channel);

        // false -> no AMQP-level autoAck with the SingleMessageQueueingConsumer
        final String consumerTag = channel.basicConsume(queue, false, consumer);
        try
        {
            final Delivery delivery = consumer.nextDelivery(actualTimeOut);

            if (delivery == null)
            {
                return null;
            }
            else
            {
                if (autoAck)
                {
                    // ack only if auto-ack was requested, otherwise it's up to the caller to ack
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }

                return new AmqpMessage(consumerTag, delivery.getEnvelope(), delivery.getProperties(),
                        delivery.getBody());
            }
        }
        finally
        {
            try
            {
                channel.basicCancel(consumerTag);
            }
            catch(IOException e)
            {
                /**
                 * The broker could decide to cancel a subscription on certain situations
                 */
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Subscription to channel with consumerTag " + StringUtils.defaultString(consumerTag) +
                            " could not be closed.", e);
                }
            }
        }
    }
}
