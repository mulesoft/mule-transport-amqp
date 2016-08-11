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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelCleanupRule;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;
import org.mule.transport.amqp.internal.client.LoggingReturnListener;
import org.mule.transport.amqp.internal.processor.ReturnHandler;
import org.mule.util.UUID;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Test;
public class MessageDispatcherItCase extends AbstractItCase
{
	@ClassRule
	public static AmqpModelRule modelRule = new AmqpModelRule("message-dispatcher-tests-model.json");
	
	@ClassRule
	public static AmqpModelCleanupRule modelCleanupRule = new AmqpModelCleanupRule(
			new String[] {"amqpCustomArgumentsService-queue"},
			new String[] {"amqpSendTargetService-exchange"});
	
    @Override
    protected String getConfigResources()
    {
        return "message-dispatcher-tests-config.xml";
    }

    @Test
    public void testDispatchToExistingExchange() throws Exception
    {
    	String flowName = "amqpExistingExchangeService";
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(nameFactory.getVmName(flowName), 
        		nameFactory.getQueueName(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testDispatchToRedeclaredExistingExchange() throws Exception
    {
    	String flowName = "amqpRedeclaredExistingExchangeService";
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(nameFactory.getVmName(flowName), 
        		nameFactory.getQueueName(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testDispatchToLegacyDefaultExchange() throws Exception
    {
    	String flowName = "amqpLegacyDefaultExchangeService";
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(nameFactory.getVmName(flowName), 
        		nameFactory.getQueueName(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testDispatchToLegacyGlobalDefaultExchange() throws Exception
    {
    	String flowName = "amqpLegacyGlobalDefaultExchangeService";
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(nameFactory.getVmName(flowName), 
        		nameFactory.getQueueName(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testDispatchToDefaultExchange() throws Exception
    {
    	String flowName = "amqpDefaultExchangeService";
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(nameFactory.getVmName(flowName), 
        		nameFactory.getQueueName(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testDispatchToGlobalDefaultExchange() throws Exception
    {
    	String flowName = "amqpGlobalDefaultExchangeService";
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(nameFactory.getVmName(flowName), 
        		nameFactory.getQueueName(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testMessageLevelOverrideService() throws Exception
    {
    	String flowName = "amqpMessageLevelOverrideService";
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(nameFactory.getVmName(flowName), 
        		nameFactory.getQueueName(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testMelOutboundEndpointService() throws Exception
    {
         String flowName = "amqpMelOutboundEndpointService";
         String queueName = nameFactory.getQueueName(flowName);

         String payload1 = "payload1::" + RandomStringUtils.randomAlphanumeric(20);
         String customHeaderValue1 = vmTestClient.dispatchTestMessage(nameFactory.getVmName(flowName),
            Collections.singletonMap("myRoutingKey", queueName), payload1);

         String payload2 = "payload2::" + RandomStringUtils.randomAlphanumeric(20);
        	vmTestClient.dispatchTestMessage(nameFactory.getVmName(flowName), 
        		Collections.singletonMap("myRoutingKey", "_somewhere_else_"), payload2);

         String payload3 = "payload3::" + RandomStringUtils.randomAlphanumeric(20);
         String customHeaderValue3 = vmTestClient.dispatchTestMessage(nameFactory.getVmName(flowName),
            Collections.singletonMap("myRoutingKey", queueName), payload3);

        // we're getting more than one message from the same queue so
        // fetchAndValidateAmqpDeliveredMessage can't be used in its current implementation as it
        // consumes all the messages but only returns one
        for (int i = 0; i < 2; i++)
        {
            GetResponse getResponse = amqpTestClient.waitUntilGetMessageWithAmqp(queueName,
                getTestTimeoutSecs() * 1000L);
            assertThat(getResponse, is(notNullValue()));

            if (Arrays.equals(payload1.getBytes(), getResponse.getBody()))
            {
            	amqpTestClient.validateAmqpDeliveredMessage(payload1, customHeaderValue1, getResponse.getBody(),
                    getResponse.getProps());
            }
            else
            {
            	amqpTestClient.validateAmqpDeliveredMessage(payload3, customHeaderValue3, getResponse.getBody(),
                    getResponse.getProps());
            }

        }

        GetResponse getNoFurtherResponse = amqpTestClient.waitUntilGetMessageWithAmqp(queueName, 1000L);
        assertThat(getNoFurtherResponse, is(nullValue()));
    }

    @Test
    public void testDispatchToNewExchange() throws Exception
    {
        String bridgeName = "amqpNewExchangeService";
        new MuleClient(muleContext).dispatch(nameFactory.getVmName(bridgeName), "ignored_payload", null);

        // there is no queue bound to this new exchange, so we can only test its
        // presence
        int attempts = 0;
        while (attempts++ < getTestTimeoutSecs())
        {
            Channel channel = null;
            try
            {
                channel = testConnectionManager.getChannel();
                channel.exchangeDeclarePassive(nameFactory.getExchangeName(bridgeName));
                return;
            }
            catch ( IOException ioe)
            {
                Thread.sleep(500L);
            }
            finally
            {
                if (channel != null && channel.isOpen())
                {
                    testConnectionManager.disposeChannel(channel);
                }
            }
        }
        fail("Exchange not created by outbound endpoint");
    }

    @Test
    public void testOutboundQueueCreation() throws Exception
    {
        String flowName = "amqpOutBoundQueue";
        new MuleClient(muleContext).dispatch(nameFactory.getVmName(flowName), "ignored_payload", null);

        // test to see if there is a message on the queue.
        int attempts = 0;
        while (attempts++ < getTestTimeoutSecs())
        {
        	Channel channel = null;
            try
            {
            	channel = testConnectionManager.getChannel();
                if (channel.basicGet(nameFactory.getQueueName(flowName), true).getBody() != null)
                {
                    return;
                }
            }
            catch ( IOException ioe)
            {
                Thread.sleep(500L);
            }
            finally
            {
                if (channel != null && channel.isOpen())
                {
                    testConnectionManager.disposeChannel(channel);
                }
            }
        }
        fail("Queue was not created or message not delivered");
    }

    @Test
    public void testExternalConnectionFactory() throws Exception
    {
        String flowName = "amqpExternalFactoryConnector";
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(nameFactory.getVmName(flowName), 
        	nameFactory.getQueueName(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testMandatoryDeliveryFailureDefaultHandler() throws Exception
    {
        LoggingReturnListener defaultReturnListener = (LoggingReturnListener) ReturnHandler.DEFAULT_RETURN_LISTENER;
        int initialHitCount = defaultReturnListener.getHitCount();

        String payload = RandomStringUtils.randomAlphanumeric(20);
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
        String payload = RandomStringUtils.randomAlphanumeric(20);
        Future<MuleMessage> futureReturnedMessage = 
            amqpTestClient.setupFunctionTestComponentForFlow(getFunctionalTestComponent("returnedMessageProcessor"));
        new MuleClient(muleContext).dispatch("vm://amqpMandatoryDeliveryFailureWithHandler.in", payload, null);
        MuleMessage returnedMessage = futureReturnedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);
        assertThat(returnedMessage, is(notNullValue()));
        assertThat(returnedMessage.getPayloadAsString(), is(equalTo(payload)));
    }	

    @Test
    public void testMandatoryDeliverySuccess() throws Exception
    {
    	String flowName = "amqpMandatoryDeliverySuccess";
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(nameFactory.getVmName(flowName), 
        		nameFactory.getQueueName(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testRequestResponse() throws Exception
    {
        String customHeaderValue = UUID.getUUID();
        String payload = RandomStringUtils.randomAlphanumeric(20);
        MuleMessage message = new DefaultMuleMessage(payload, 
            Collections.<String, Object>singletonMap("customHeader", customHeaderValue), muleContext);
        MuleMessage response = new MuleClient(muleContext)
         	.send("vm://amqpRequestResponseService.in", message, getTestTimeoutSecs() * 1000);

        assertThat(response.getPayloadAsString(), is(equalTo(payload + "-response")));
        assertThat(response.getInboundProperty("customHeader").toString(), is(equalTo(customHeaderValue)));
    }

    @Test
    public void testCustomArguments() throws Exception
    {
        Future<MuleMessage> routedMessage = amqpTestClient.setupFunctionTestComponentForFlow(
            getFunctionalTestComponent("amqpEndpointWithCustomArgumentsMessageProcessor"));

        String payload = RandomStringUtils.randomAlphanumeric(20);
        new MuleClient(muleContext).dispatch("vm://amqpCustomArgumentsService.in", payload, null);
        MuleMessage muleMessage = routedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);

        assertThat(muleMessage.getPayloadAsString(), is(equalTo(payload)));
    }
}
