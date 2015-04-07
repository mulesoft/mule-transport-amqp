/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector;

import junit.framework.Assert;

import org.junit.Test;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.amqp.internal.domain.AmqpMuleMessageFactoryTestCase;
import org.mule.transport.amqp.internal.domain.AckMode;

public class AmqpConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    public Connector createConnector() throws Exception
    {
        final AmqpConnector connector = new AmqpConnector(muleContext);
        connector.setName("Test");
        return connector;
    }

    @Override
    public String getTestEndpointURI()
    {
        return "amqp://target-exchange/target-queue";
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return AmqpMuleMessageFactoryTestCase.getTestMessage();
    }

    @Test
    public void testProperties() throws Exception
    {
        int requestHeartBeat = 1234;
        final AmqpConnector amqpConnector = (AmqpConnector) getConnector();
        amqpConnector.setAckMode(AckMode.MULE_AUTO);
        amqpConnector.setRequestedHeartbeat(requestHeartBeat);
        Assert.assertEquals(AckMode.MULE_AUTO, amqpConnector.getAckMode());
        Assert.assertEquals(requestHeartBeat, amqpConnector.getRequestedHeartbeat());
    }

    @Override
    @Test
    public void testConnectorLifecycle() throws Exception
    {
        // Deactivated because we don't want to start the connector in unit tests
    }
}
