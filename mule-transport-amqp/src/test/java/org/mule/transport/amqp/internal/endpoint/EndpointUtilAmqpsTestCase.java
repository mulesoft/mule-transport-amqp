/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mule.transport.amqp.internal.connector.AmqpsConnector;

public class EndpointUtilAmqpsTestCase
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
        assertEquals("queue",
                endpointUtil.getQueueName("amqps://exchange/amqp-queue.queue?connector=foo"));
        assertEquals("queue", endpointUtil.getQueueName("amqps://exchange/amqp-queue.queue"));
        assertEquals("queue", endpointUtil.getQueueName("amqps://amqp-queue.queue?connector=foo\""));
        assertEquals("queue", endpointUtil.getQueueName("amqps://amqp-queue.queue"));
        assertEquals("", endpointUtil.getQueueName("amqps://exchange?connector=foo\""));
        assertEquals("", endpointUtil.getQueueName("amqps://exchange"));
    }

    @Test
    public void testGetExchangeName()
    {
        assertEquals("exchange", endpointUtil.getExchangeName(
                "amqps://exchange/amqp-queue.queue?connector=foo", AmqpsConnector.AMQPS));
        assertEquals("exchange",
                endpointUtil.getExchangeName("amqps://exchange/amqp-queue.queue", AmqpsConnector.AMQPS));
        assertEquals("",
                endpointUtil.getExchangeName("amqps://amqp-queue.queue?connector=foo", AmqpsConnector.AMQPS));
        assertEquals("", endpointUtil.getExchangeName("amqps://amqp-queue.queue", AmqpsConnector.AMQPS));
        assertEquals("exchange",
                endpointUtil.getExchangeName("amqps://exchange?connector=foo", AmqpsConnector.AMQPS));
        assertEquals("exchange", endpointUtil.getExchangeName("amqps://exchange", AmqpsConnector.AMQPS));
    }
}
