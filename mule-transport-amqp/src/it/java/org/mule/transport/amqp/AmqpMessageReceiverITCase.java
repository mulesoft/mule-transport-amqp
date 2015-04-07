/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

public class AmqpMessageReceiverITCase extends AbstractAmqpInboundITCase
{
    public AmqpMessageReceiverITCase() throws IOException, InterruptedException
    {
        super();

        // create the required pre-existing exchanges and queues
        setupExchangeAndQueue("amqpExistingQueueService");
        setupExchange("amqpUnboundQueueService");
        setupExchange("amqpServerNamedQueueExistingExchangeService");
        setupExchange("amqpNewQueueExistingExchangeService");
        setupExchange("amqpNewQueueRedeclaredExistingExchangeService");
        setupExchangeAndQueue("amqpClientConsumerTagService");
        setupExchangeAndQueue("amqpMuleAckService");
        setupExchangeAndQueue("amqpManualAckService");
        setupExchangeAndQueue("amqpManualRejectService");
        setupExchangeAndQueue("amqpManualRecoverService");
        setupExchangeAndQueue("amqpExclusiveConsumerService");
        deleteExchange("amqpCustomArgumentsService");
        setupExchange("amqpAlternateExchange");
    }

    @Override
    protected String getConfigResources()
    {
        return "message-receiver-tests-config.xml";
    }

    @Test
    public void testExistingQueue() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpExistingQueueService");
    }

    @Test
    public void testUnboundQueue() throws Exception
    {
        final String flowName = "amqpUnboundQueueService";

        // mule should have created the queue so let's bind it and use it
        getChannel().queueBind(getQueueName(flowName), getExchangeName(flowName), "");
        getChannel().queuePurge(getQueueName(flowName));

        dispatchTestMessageAndAssertValidReceivedMessage(flowName);
    }

    @Test
    public void testServerNamedQueueExistingExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpServerNamedQueueExistingExchangeService");
    }

    @Test
    public void testNewQueueExistingExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpNewQueueExistingExchangeService");
    }

    @Test
    public void testNewQueueRedeclaredExistingExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpNewQueueRedeclaredExistingExchangeService");
    }

    @Test
    public void testClientConsumerTag() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpClientConsumerTagService");
    }

    @Test
    public void testNewQueueNewExchange() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpNewQueueNewExchangeService");
    }

    @Test
    public void testMuleAcknowledgment() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpMuleAckService");
    }

    @Test
    public void testManualAcknowledgment() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpManualAckService");
    }

    @Test
    public void testManualRejection() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpManualRejectService");
        // check the message has been successfully pushed back to the queue
        assertNotNull(consumeMessageWithAmqp(getQueueName("amqpManualRejectService"), getTestTimeoutSecs()));
    }

    @Test
    public void testManualRecover() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpManualRecoverService");
        // check the message has been successfully pushed back to the queue
        assertNotNull(consumeMessageWithAmqp(getQueueName("amqpManualRecoverService"), getTestTimeoutSecs()));
    }

    @Test
    public void testExclusiveConsumer() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpExclusiveConsumerService");
    }
}
