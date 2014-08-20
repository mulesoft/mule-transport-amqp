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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mule.transport.amqp.internal.connector.AmqpsConnector;

public class AmqpsUrlEndpointURIParserTestCase
{
    UrlEndpointURIParser uriParser;

    @Before
    public void prepare()
    {
        uriParser = new UrlEndpointURIParser();
    }

    @Test
    public void testGetQueueName()
    {
        assertEquals("queue",
                uriParser.getQueueName("amqps://exchange/amqp-queue.queue?connector=foo"));
        assertEquals("queue", uriParser.getQueueName("amqps://exchange/amqp-queue.queue"));
        assertEquals("queue", uriParser.getQueueName("amqps://amqp-queue.queue?connector=foo\""));
        assertEquals("queue", uriParser.getQueueName("amqps://amqp-queue.queue"));
        assertEquals("", uriParser.getQueueName("amqps://exchange?connector=foo\""));
        assertEquals("", uriParser.getQueueName("amqps://exchange"));
    }

    @Test
    public void testGetExchangeName()
    {
        assertEquals("exchange", uriParser.getExchangeName(
                "amqps://exchange/amqp-queue.queue?connector=foo", AmqpsConnector.AMQPS));
        assertEquals("exchange",
                uriParser.getExchangeName("amqps://exchange/amqp-queue.queue", AmqpsConnector.AMQPS));
        assertEquals("",
                uriParser.getExchangeName("amqps://amqp-queue.queue?connector=foo", AmqpsConnector.AMQPS));
        assertEquals("", uriParser.getExchangeName("amqps://amqp-queue.queue", AmqpsConnector.AMQPS));
        assertEquals("exchange",
                uriParser.getExchangeName("amqps://exchange?connector=foo", AmqpsConnector.AMQPS));
        assertEquals("exchange", uriParser.getExchangeName("amqps://exchange", AmqpsConnector.AMQPS));
    }
}
