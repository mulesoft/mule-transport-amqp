/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A queueing consumer that disconnects after receiving one message and actively rejects any
 * extra message received. <b>It must be used with a non auto-ack consumer</b>. It has the
 * advantage of using the consume semantics, including non-blocking time-out, without polling as
 * using basic get would do.
 *
 * Consumer cancellation is up to the user.
 */
final class SingleMessageQueueingConsumer extends QueueingConsumer
{
    private final AtomicBoolean received;

    SingleMessageQueueingConsumer(final Channel channel)
    {
        super(channel);
        received = new AtomicBoolean();
    }

    @Override
    public void handleDelivery(final String consumerTag,
                               final Envelope envelope,
                               final AMQP.BasicProperties properties,
                               final byte[] body) throws IOException
    {
        if (received.get())
        {
            // extra messages could have been received before basicCancel happens -> actively
            // reject them
            getChannel().basicReject(envelope.getDeliveryTag(), true);
        }
        else
        {
            received.set(true);
            super.handleDelivery(consumerTag, envelope, properties, body);
        }
    }
}
