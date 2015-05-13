/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.ClassRule;
import org.junit.Test;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelCleanupRule;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;

import com.rabbitmq.client.QueueingConsumer.Delivery;

public class MessageReceiverItCase extends AbstractItCase
{
	@ClassRule
	public static AmqpModelRule modelRule = new AmqpModelRule("message-receiver-tests-model.json");

	@ClassRule
	public static AmqpModelCleanupRule modelCleanupRule = new AmqpModelCleanupRule(
			new String[] {"amqpReceiverTestUnboundQueueService-queue"},
			new String[] {"amqpReceiverTestCustomArgumentsService-exchange"});
	
    @Override
    protected String getConfigResources()
    {
        return "message-receiver-tests-config.xml";
    }
    
    @Test
    public void testExistingQueue() throws Exception
    {
        String flowName = "amqpReceiverTestExistingQueueService";

    	amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testUnboundQueue() throws Exception
    {
        String flowName = "amqpReceiverTestUnboundQueueService";
        
        // mule should have created the queue so let's bind it and use it
        channel.queueBind(nameFactory.getQueueName(flowName), 
        		nameFactory.getExchangeName(flowName), "");
        channel.queuePurge(nameFactory.getQueueName(flowName));
        
    	amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testServerNamedQueueExistingExchange() throws Exception
    {
        String flowName = "amqpReceiverTestServerNamedQueueExistingExchangeService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testNewQueueExistingExchange() throws Exception
    {
        String flowName = "amqpReceiverTestNewQueueExistingExchangeService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testNewQueueRedeclaredExistingExchange() throws Exception
    {
        String flowName = "amqpReceiverTestNewQueueRedeclaredExistingExchangeService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testClientConsumerTag() throws Exception
    {
        String flowName = "amqpReceiverTestClientConsumerTagService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testNewQueueNewExchange() throws Exception
    {
        String flowName = "amqpReceiverTestNewQueueNewExchangeService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testMuleAcknowledgment() throws Exception
    {
        String flowName = "amqpReceiverTestMuleAckService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testManualAcknowledgment() throws Exception
    {
        String flowName = "amqpReceiverTestManualAckService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

    @Test
    public void testManualRejection() throws Exception
    {
        String flowName = "amqpReceiverTestManualRejectService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());

        // check the message has been successfully pushed back to the queue
        Delivery delivery = amqpTestClient.consumeMessageWithAmqp(nameFactory.getQueueName(flowName), 
        	getTestTimeoutSecs());
   		assertThat(delivery, is(notNullValue()));
    }

    @Test
    public void testManualRecover() throws Exception
    {
        String flowName = "amqpReceiverTestManualRecoverService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());

        // check the message has been successfully pushed back to the queue
        Delivery delivery = amqpTestClient.consumeMessageWithAmqp(nameFactory.getQueueName(flowName), 
        	getTestTimeoutSecs());
   		assertThat(delivery, is(notNullValue()));
    }

    @Test
    public void testExclusiveConsumer() throws Exception
    {
        String flowName = "amqpReceiverTestExclusiveConsumerService";

        amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    		getFunctionalTestComponent(flowName), getTestTimeoutSecs());
    }

}
