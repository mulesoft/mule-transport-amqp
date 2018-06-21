/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AckMode;

public class AmqpNamespaceHandlerTestCase extends AbstractAmqpNamespaceHandlerTestCase
{
    public AmqpNamespaceHandlerTestCase()
    {
        super();
    }

    @Override
    protected String getConfigResources()
    {
        return "amqp-namespace-config.xml";
    }

    @Override
    protected String getProtocol()
    {
        return AmqpConnector.AMQP;
    }

    @Test
    public void testDefaultConnector() throws Exception
    {
        final AmqpConnector c = (AmqpConnector) muleContext.getRegistry().lookupConnector("amqpDefaultConnector");
        assertNotNull(c);

        assertEquals(AckMode.AMQP_AUTO, c.getAckMode());
    }
}
