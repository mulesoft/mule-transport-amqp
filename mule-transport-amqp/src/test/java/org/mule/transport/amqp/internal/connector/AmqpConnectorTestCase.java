/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(amqpConnector.getAckMode(), equalTo(AckMode.MULE_AUTO));
        assertThat(amqpConnector.getRequestedHeartbeat(), equalTo(requestHeartBeat));
        assertThat(amqpConnector.getReceiverThreadingProfile().getMaxThreadsActive(), equalTo(amqpConnector.getPrefetchCount()));
    }

    @Override
    @Test
    public void testConnectorLifecycle() throws Exception
    {
        // Deactivated because we don't want to start the connector in unit tests
    }
}
