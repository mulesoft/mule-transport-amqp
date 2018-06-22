/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.config;

import static java.lang.System.getProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.transport.amqp.internal.connector.AmqpsConnector;

import org.junit.Test;

public class AmqpsNamespaceHandlerTestCase extends AbstractAmqpNamespaceHandlerTestCase
{
    public AmqpsNamespaceHandlerTestCase()
    {
        super();
    }
    private static final String AMQP_SSL_PROTOCOL = getProperty("amqpSslProtocol");

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

        assertEquals(TlsConfiguration.DEFAULT_SSL_TYPE, c.getSslProtocol());
        assertNull(c.getSslTrustManager());
    }

    @Test
    public void testTlsConnector() throws Exception
    {
        final AmqpsConnector c = (AmqpsConnector) muleContext.getRegistry().lookupConnector(
            "amqpsTlsConnector");

        assertEquals(AMQP_SSL_PROTOCOL, c.getSslProtocol());
        assertNull(c.getSslTrustManager());
    }

    @Test
    public void testTrustManagerConnector() throws Exception
    {
        final AmqpsConnector c = (AmqpsConnector) muleContext.getRegistry().lookupConnector(
            "amqpsTrustManagerConnector");

        assertNotNull(c.getSslTrustManager());
    }

    @Test
    public void testTlsTrustManagerConnector() throws Exception
    {
        final AmqpsConnector c = (AmqpsConnector) muleContext.getRegistry().lookupConnector(
            "amqpsTlsTrustManagerConnector");

        assertEquals(AMQP_SSL_PROTOCOL, c.getSslProtocol());
        assertNotNull(c.getSslTrustManager());
    }
}
