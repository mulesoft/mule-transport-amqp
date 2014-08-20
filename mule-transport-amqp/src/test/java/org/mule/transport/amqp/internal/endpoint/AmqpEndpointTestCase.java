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

import java.net.URI;

import org.junit.Test;
import org.mule.api.endpoint.EndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.amqp.internal.client.UrlEndpointURIBuilder;

public class AmqpEndpointTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testEndpointWithExchangeAndQueue() throws Exception
    {
        final EndpointURI uri = new UrlEndpointURIBuilder().build(new URI(
            "amqp://target-exchange/target-queue"), muleContext);

        assertEquals("amqp", uri.getScheme());
        assertEquals("amqp://target-exchange/target-queue", uri.getAddress());
        // using the host as resource name could be an issue because exchange and
        // queue names accept characters that are
        // invalid in host names: ^[a-zA-Z0-9-_.:]*$
        assertEquals("target-exchange", uri.getHost());
        assertEquals("/target-queue", uri.getPath());
    }

    @Test
    public void testEndpointWithQueue() throws Exception
    {
        final EndpointURI uri = new UrlEndpointURIBuilder().build(
            new URI("amqp://amqp-queue.other.queue"), muleContext);

        assertEquals("amqp", uri.getScheme());
        assertEquals("amqp://amqp-queue.other.queue", uri.getAddress());
        assertEquals("amqp-queue.other.queue", uri.getHost());
        assertEquals("", uri.getPath());
    }
}
