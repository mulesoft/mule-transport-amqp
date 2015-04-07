/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.mule.transport.amqp.internal.connector.AmqpsConnector;

public class AmqpsNamespaceHandlerTestCase extends AbstractAmqpNamespaceHandlerTestCase
{
    public AmqpsNamespaceHandlerTestCase()
    {
        super();
    }

    @Override
    protected String getConfigResources()
    {
        return "amqps-namespace-config.xml";
    }

    @Override
    protected String getProtocol()
    {
        return AmqpsConnector.AMQPS;
    }

    @Test
    public void testDefaultSslProtocol() throws Exception
    {
        final AmqpsConnector c = (AmqpsConnector) muleContext.getRegistry().lookupConnector(
            "amqpsDefaultSslConnector");

        assertEquals("SSLv3", c.getSslProtocol());
        assertNull(c.getSslTrustManager());
    }

    @Test
    public void testTlsConnector() throws Exception
    {
        final AmqpsConnector c = (AmqpsConnector) muleContext.getRegistry().lookupConnector(
            "amqpsTlsConnector");

        assertEquals("TLS", c.getSslProtocol());
        assertNull(c.getSslTrustManager());
    }

    @Test
    public void testTrustManagerConnector() throws Exception
    {
        final AmqpsConnector c = (AmqpsConnector) muleContext.getRegistry().lookupConnector(
            "amqpsTrustManagerConnector");

        assertEquals("SSLv3", c.getSslProtocol());
        assertNotNull(c.getSslTrustManager());
    }

    @Test
    public void testTlsTrustManagerConnector() throws Exception
    {
        final AmqpsConnector c = (AmqpsConnector) muleContext.getRegistry().lookupConnector(
            "amqpsTlsTrustManagerConnector");

        assertEquals("TLS", c.getSslProtocol());
        assertNotNull(c.getSslTrustManager());
    }
}
