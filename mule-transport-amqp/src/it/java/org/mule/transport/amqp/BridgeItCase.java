/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelCleanupRule;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;
import org.mule.util.UUID;

import com.rabbitmq.client.Delivery;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Test;

public class BridgeItCase extends AbstractItCase
{
    @ClassRule
    public static AmqpModelRule modelRule = new AmqpModelRule("bridge-tests-model.json");

    @ClassRule
    public static AmqpModelCleanupRule modelCleanupRule = new AmqpModelCleanupRule(
            new String[] {},
            new String[] {"amqpRequestResponseBridgeTarget-exchange"});

    @Override
    protected String getConfigResources()
    {
        return "bridge-tests-config.xml";
    }

    @Test
    public void testOneWayBridge() throws Exception
    {
        String flowName = "amqpOneWayBridge";
        dispatchTestMessageAndAssertValidReceivedMessage("amqpOneWayBridgeTarget-queue",
            nameFactory.getExchangeName(flowName));
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
            String payload = RandomStringUtils.randomAlphanumeric(20);
            amqpTestClient.publishMessageWithAmqp(payload.getBytes(),
                nameFactory.getExchangeName("amqpThrottledBridge"));
        }

        FunctionalTestComponent functionalTestComponent = 
            getFunctionalTestComponent("amqpThrottledBridgeTarget");

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
        dispatchTestMessageAndAssertValidReceivedMessage("amqpTransactedBridgeTarget-queue",
            "amqpTransactedBridge-exchange");
    }

    private void dispatchTestMessageAndAssertValidReceivedMessage(String targetQueueName, String exchangeName)
            throws Exception
    {
        String payload = RandomStringUtils.randomAlphanumeric(20);
        String correlationId = amqpTestClient.publishMessageWithAmqp(payload.getBytes(), exchangeName);

        Delivery dispatchedMessage = amqpTestClient.consumeMessageWithAmqp(
                targetQueueName, getTestTimeoutSecs() * 1000L);

        assertThat(dispatchedMessage, is(notNullValue()));
        assertThat(new String(dispatchedMessage.getBody()), equalTo(payload));
        assertThat(new String(dispatchedMessage.getProperties().getCorrelationId()), equalTo(correlationId));
    }

    private void sendTestMessageAndAssertValidReceivedMessage(final String flowName) throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final String correlationId = UUID.getUUID();

        final Delivery result = amqpTestClient.sendMessageWithAmqp(correlationId, payload.getBytes(),
            flowName + "-exchange", getTestTimeoutSecs() * 1000L);

        assertThat(result, is(notNullValue()));
        assertThat(new String(result.getBody()), equalTo(payload + "-response"));
        assertThat(new String(result.getProperties().getCorrelationId()), equalTo(correlationId));
    }



}
