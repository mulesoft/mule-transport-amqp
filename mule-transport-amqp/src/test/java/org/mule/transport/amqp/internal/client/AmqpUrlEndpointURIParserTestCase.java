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
import org.mule.transport.amqp.internal.connector.AmqpConnector;

public class AmqpUrlEndpointURIParserTestCase
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
        assertEquals("queue", uriParser.getQueueName("amqp://exchange/amqp-queue.queue?connector=foo"));
        assertEquals("queue", uriParser.getQueueName("amqp://exchange/amqp-queue.queue"));
        assertEquals("queue", uriParser.getQueueName("amqp://amqp-queue.queue?connector=foo\""));
        assertEquals("queue", uriParser.getQueueName("amqp://amqp-queue.queue"));
        assertEquals("", uriParser.getQueueName("amqp://exchange?connector=foo\""));
        assertEquals("", uriParser.getQueueName("amqp://exchange"));
    }

    @Test
    public void testGetExchangeName()
    {
        assertEquals("exchange", uriParser.getExchangeName(
                "amqp://exchange/amqp-queue.queue?connector=foo", AmqpConnector.AMQP));
        assertEquals("exchange",
                uriParser.getExchangeName("amqp://exchange/amqp-queue.queue", AmqpConnector.AMQP));
        assertEquals("",
                uriParser.getExchangeName("amqp://amqp-queue.queue?connector=foo", AmqpConnector.AMQP));
        assertEquals("", uriParser.getExchangeName("amqp://amqp-queue.queue", AmqpConnector.AMQP));
        assertEquals("exchange",
                uriParser.getExchangeName("amqp://exchange?connector=foo", AmqpConnector.AMQP));
        assertEquals("exchange", uriParser.getExchangeName("amqp://exchange", AmqpConnector.AMQP));
    }
}
