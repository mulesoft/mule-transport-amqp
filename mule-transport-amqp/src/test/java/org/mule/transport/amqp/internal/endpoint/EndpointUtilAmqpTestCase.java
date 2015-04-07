/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mule.transport.amqp.internal.connector.AmqpConnector;

public class EndpointUtilAmqpTestCase
{
    AmqpEndpointUtil endpointUtil;

    @Before
    public void prepare()
    {
        endpointUtil = new AmqpEndpointUtil();
    }

    @Test
    public void testGetQueueName()
    {
        assertEquals("queue", endpointUtil.getQueueName("amqp://exchange/amqp-queue.queue?connector=foo"));
        assertEquals("queue", endpointUtil.getQueueName("amqp://exchange/amqp-queue.queue"));
        assertEquals("queue", endpointUtil.getQueueName("amqp://amqp-queue.queue?connector=foo\""));
        assertEquals("queue", endpointUtil.getQueueName("amqp://amqp-queue.queue"));
        assertEquals("", endpointUtil.getQueueName("amqp://exchange?connector=foo\""));
        assertEquals("", endpointUtil.getQueueName("amqp://exchange"));
    }

    @Test
    public void testGetExchangeName()
    {
        assertEquals("exchange", endpointUtil.getExchangeName(
                "amqp://exchange/amqp-queue.queue?connector=foo", AmqpConnector.AMQP));
        assertEquals("exchange",
                endpointUtil.getExchangeName("amqp://exchange/amqp-queue.queue", AmqpConnector.AMQP));
        assertEquals("",
                endpointUtil.getExchangeName("amqp://amqp-queue.queue?connector=foo", AmqpConnector.AMQP));
        assertEquals("", endpointUtil.getExchangeName("amqp://amqp-queue.queue", AmqpConnector.AMQP));
        assertEquals("exchange",
                endpointUtil.getExchangeName("amqp://exchange?connector=foo", AmqpConnector.AMQP));
        assertEquals("exchange", endpointUtil.getExchangeName("amqp://exchange", AmqpConnector.AMQP));
    }
}
