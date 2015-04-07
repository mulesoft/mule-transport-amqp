/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import com.rabbitmq.client.QueueingConsumer.Delivery;

public class AmqpTransactionalITCase extends AbstractAmqpITCase
{
    private static final String FLOW_NAME = "amqpTransactionalSupport";

    public AmqpTransactionalITCase() throws Exception
    {
        super();

        final String exchange = getExchangeName(FLOW_NAME);
        getChannel().exchangeDeclare(exchange, "direct");
        final String queue = setupQueue(FLOW_NAME);
        getChannel().queueBind(queue, exchange, "rk1");
        getChannel().queueBind(queue, exchange, "rk2");
        getChannel().queuePurge(queue);
    }

    @Override
    protected String getConfigResources()
    {
        return "transactional-tests-config.xml";
    }

    @Test
    public void testTransactionalSupport() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);

        muleContext.getClient().dispatch("vm://" + FLOW_NAME + ".in", payload, null);

        // should have received two messages in the queue
        final List<Delivery> dispatchedMessage = consumeMessagesWithAmqp(getQueueName(FLOW_NAME),
            getTestTimeoutSecs() * 1000L, 2);
        assertEquals(2, dispatchedMessage.size());
        assertEquals(payload, new String(dispatchedMessage.get(0).getBody()));
        assertEquals(payload, new String(dispatchedMessage.get(1).getBody()));
    }
}
