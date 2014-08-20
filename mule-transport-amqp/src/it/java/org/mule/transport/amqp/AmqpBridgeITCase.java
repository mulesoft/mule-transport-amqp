/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.UUID;

import com.rabbitmq.client.QueueingConsumer.Delivery;

public class AmqpBridgeITCase extends AbstractAmqpITCase
{
    public AmqpBridgeITCase() throws Exception
    {
        super();
        // create/delete the required pre-existing exchanges and queues
        setupExchangeAndQueue("amqpOneWayBridge");
        setupExchangeAndQueue("amqpOneWayBridgeTarget");
        setupExchangeAndQueue("amqpRequestResponseBridge");
        setupExchangeAndQueue("amqpThrottledBridge");
        setupExchangeAndQueue("amqpThrottledBridgeTarget");
        setupExchangeAndQueue("amqpTransactedBridge");
    }

    @Override
    protected String getConfigResources()
    {
        return "bridge-tests-config.xml";
    }

    @Test
    public void testOneWayBridge() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpOneWayBridge", "amqpOneWayBridgeTarget-queue");
    }

    @Test
    public void testRequestResponseBridge() throws Exception
    {
        sendTestMessageAndAssertValidReceivedMessage("amqpRequestResponseBridge");
    }

    @Test
    public void testThrottledBridge() throws Exception
    {
        for (int i = 0; i < 10; i++)
        {
            final String payload = RandomStringUtils.randomAlphanumeric(20);
            publishMessageWithAmqp(payload.getBytes(), "amqpThrottledBridge");
        }

        final FunctionalTestComponent functionalTestComponent = getFunctionalTestComponent("amqpThrottledBridgeTarget");

        int attempts = 0;
        while (attempts++ < getTestTimeoutSecs() * 2)
        {
            if (functionalTestComponent.getReceivedMessagesCount() == 10) return;
            Thread.sleep(500L);
        }
        fail("Not all messages made it through the throttled bridge");
    }

    @Test
    public void testTransactedBridge() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpTransactedBridge",
            "amqpOneWayBridgeTarget-queue");
    }

    private void dispatchTestMessageAndAssertValidReceivedMessage(final String flowName,
                                                                  final String targetQueueName)
        throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final String correlationId = publishMessageWithAmqp(payload.getBytes(), flowName);

        final Delivery dispatchedMessage = consumeMessageWithAmqp(targetQueueName,
            getTestTimeoutSecs() * 1000L);

        assertNotNull(dispatchedMessage);
        assertEquals(payload, new String(dispatchedMessage.getBody()));
        assertEquals(correlationId, dispatchedMessage.getProperties().getCorrelationId());
    }

    private void sendTestMessageAndAssertValidReceivedMessage(final String flowName) throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final String correlationId = UUID.getUUID();

        final Delivery result = sendMessageWithAmqp(correlationId, payload.getBytes(), flowName,
            getTestTimeoutSecs() * 1000L);

        assertNotNull(result);
        assertEquals(payload + "-response", new String(result.getBody()));
        assertEquals(correlationId, result.getProperties().getCorrelationId());
    }
}
