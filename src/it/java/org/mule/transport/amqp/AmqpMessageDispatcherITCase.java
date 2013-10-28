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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.amqp.AmqpReturnHandler.LoggingReturnListener;
import org.mule.util.UUID;

import com.rabbitmq.client.GetResponse;

public class AmqpMessageDispatcherITCase extends AbstractAmqpOutboundITCase
{
    public AmqpMessageDispatcherITCase() throws Exception
    {
        super();
        // create/delete the required pre-existing exchanges and queues
        setupExchangeAndQueue("amqpExistingExchangeService");
        setupExchangeAndQueue("amqpRedeclaredExistingExchangeService");
        deleteExchange("amqpNewExchangeService");
        deleteExchange("amqpExternalFactoryConnector");
        deleteExchange("amqpOutBoundQueue");
        deleteQueue("amqpOutBoundQueue");
        setupExchangeAndQueue("amqpMessageLevelOverrideService");
        setupDirectExchangeAndQueue("amqpMelOutboundEndpointService");
        setupExchange("amqpMandatoryDeliveryFailureNoHandler");
        setupExchange("amqpMandatoryDeliveryFailureWithHandler");
        setupExchangeAndQueue("amqpMandatoryDeliverySuccess");
        deleteExchange("amqpCustomArgumentsService");
        setupExchange("amqpCustomArgumentsService");
        setupQueue("amqpLegacyDefaultExchangeService");
        setupQueue("amqpLegacyGlobalDefaultExchangeService");
        setupQueue("amqpDefaultExchangeService");
        setupQueue("amqpGlobalDefaultExchangeService");
    }

    @Override
    protected String getConfigResources()
    {
        return "message-dispatcher-tests-config.xml";
    }

    @Test
    public void testDispatchToExistingExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpExistingExchangeService");
    }

    @Test
    public void testDispatchToRedeclaredExistingExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpRedeclaredExistingExchangeService");
    }

    @Test
    public void testDispatchToLegacyDefaultExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpLegacyDefaultExchangeService");
    }

    @Test
    public void testDispatchToLegacyGlobalDefaultExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpLegacyGlobalDefaultExchangeService");
    }

    @Test
    public void testDispatchToDefaultExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpDefaultExchangeService");
    }

    @Test
    public void testDispatchToGlobalDefaultExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpGlobalDefaultExchangeService");
    }

    @Test
    public void testMessageLevelOverrideService() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpMessageLevelOverrideService");
    }

    @Test
    public void testMelOutboundEndpointService() throws Exception
    {
        final String flowName = "amqpMelOutboundEndpointService";
        final String queueName = getQueueName(flowName);

        final String payload1 = "payload1::" + RandomStringUtils.randomAlphanumeric(20);
        final String customHeaderValue1 = dispatchTestMessage(flowName,
            Collections.singletonMap("myRoutingKey", queueName), payload1);

        final String payload2 = "payload2::" + RandomStringUtils.randomAlphanumeric(20);
        dispatchTestMessage(flowName, Collections.singletonMap("myRoutingKey", "_somewhere_else_"), payload2);

        final String payload3 = "payload3::" + RandomStringUtils.randomAlphanumeric(20);
        final String customHeaderValue3 = dispatchTestMessage(flowName,
            Collections.singletonMap("myRoutingKey", queueName), payload3);

        // we're getting more than one message from the same queue so
        // fetchAndValidateAmqpDeliveredMessage can't be used in its current implementation as it
        // consumes all the messages but only returns one
        for (int i = 0; i < 2; i++)
        {
            final GetResponse getResponse = waitUntilGetMessageWithAmqp(queueName,
                getTestTimeoutSecs() * 1000L);
            assertNotNull(getResponse);

            if (Arrays.equals(payload1.getBytes(), getResponse.getBody()))
            {
                validateAmqpDeliveredMessage(payload1, customHeaderValue1, getResponse.getBody(),
                    getResponse.getProps());
            }
            else
            {
                validateAmqpDeliveredMessage(payload3, customHeaderValue3, getResponse.getBody(),
                    getResponse.getProps());
            }

        }

        final GetResponse getNoFurtherResponse = waitUntilGetMessageWithAmqp(queueName, 1000L);
        assertNull(getNoFurtherResponse);
    }

    @Test
    public void testDispatchToNewExchange() throws Exception
    {
        final String bridgeName = "amqpNewExchangeService";
        new MuleClient(muleContext).dispatch("vm://" + bridgeName + ".in", "ignored_payload", null);

        // there is no queue bound to this new exchange, so we can only test its
        // presence
        int attempts = 0;
        while (attempts++ < getTestTimeoutSecs() * 2)
        {
            try
            {
                getChannel().exchangeDeclarePassive(getExchangeName(bridgeName));
                return;
            }
            catch (final IOException ioe)
            {
                Thread.sleep(500L);
            }
        }
        fail("Exchange not created by outbound endpoint");
    }

    @Test
    public void testOutboundQueueCreation() throws Exception
    {
        final String flowName = "amqpOutBoundQueue";
        new MuleClient(muleContext).dispatch("vm://" + flowName + ".in", "ignored_payload", null);

        // test to see if there is a message on the queue.
        int attempts = 0;
        while (attempts++ < getTestTimeoutSecs() * 2)
        {
            try
            {
                if (getChannel().basicGet(getQueueName(flowName), true).getBody() != null)
                {
                    return;
                }
            }
            catch (final IOException ioe)
            {
                Thread.sleep(500L);
            }
        }
        fail("Queue was not created or message not delivered");
    }

    @Test
    public void testExternalConnectionFactory() throws Exception
    {
        final String flowName = "amqpExternalFactoryConnector";
        new MuleClient(muleContext).dispatch("vm://" + flowName + ".in", "ignored_payload", null);

        // there is no queue bound to this new exchange, so we can only test its
        // presence
        int attempts = 0;
        while (attempts++ < getTestTimeoutSecs() * 2)
        {
            try
            {
                getChannel().exchangeDeclarePassive(getExchangeName(flowName));
                return;
            }
            catch (final IOException ioe)
            {
                Thread.sleep(500L);
            }
        }
        fail("Exchange not created by outbound endpoint when using an external connection factory");
    }

    @Test
    public void testMandatoryDeliveryFailureDefaultHandler() throws Exception
    {
        final LoggingReturnListener defaultReturnListener = (LoggingReturnListener) AmqpReturnHandler.DEFAULT_RETURN_LISTENER;
        final int initialHitCount = defaultReturnListener.getHitCount();

        final String payload = RandomStringUtils.randomAlphanumeric(20);
        new MuleClient(muleContext).dispatch("vm://amqpMandatoryDeliveryFailureNoHandler.in", payload, null);
        int attempts = 0;
        while (attempts++ < 20)
        {
            if (defaultReturnListener.getHitCount() == initialHitCount + 1) return;
            Thread.sleep(250L);
        }
        fail("Returned message never hit the default handler");
    }

    @Test
    public void testMandatoryDeliveryFailureWithHandler() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final Future<MuleMessage> futureReturnedMessage = setupFunctionTestComponentForFlow("returnedMessageProcessor");
        new MuleClient(muleContext).dispatch("vm://amqpMandatoryDeliveryFailureWithHandler.in", payload, null);
        final MuleMessage returnedMessage = futureReturnedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);
        assertNotNull(returnedMessage);
        assertEquals(payload, returnedMessage.getPayloadAsString());
    }

    @Test
    public void testMandatoryDeliverySuccess() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpMandatoryDeliverySuccess");
    }

    @Test
    public void testRequestResponse() throws Exception
    {
        final String customHeaderValue = UUID.getUUID();
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final MuleMessage response = new MuleClient(muleContext).send("vm://amqpRequestResponseService.in",
            payload, Collections.singletonMap("customHeader", customHeaderValue), getTestTimeoutSecs() * 1000);

        assertEquals(payload + "-response", response.getPayloadAsString());
        assertEquals(customHeaderValue, response.getInboundProperty("customHeader").toString());
    }

    @Test
    public void testCustomArguments() throws Exception
    {
        final Future<MuleMessage> routedMessage = setupFunctionTestComponentForFlow("amqpEndpointWithCustomArgumentsMessageProcessor");

        final String payload = RandomStringUtils.randomAlphanumeric(20);
        new MuleClient(muleContext).dispatch("vm://amqpCustomArgumentsService.in", payload, null);

        final MuleMessage muleMessage = routedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);

        assertEquals(payload, muleMessage.getPayloadAsString());
    }
}
