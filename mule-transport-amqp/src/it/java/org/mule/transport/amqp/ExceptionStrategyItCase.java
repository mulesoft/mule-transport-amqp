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

import org.apache.commons.lang.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;

import com.rabbitmq.client.QueueingConsumer.Delivery;

public class ExceptionStrategyItCase extends AbstractItCase
{
	@ClassRule
	public static AmqpModelRule modelRule = new AmqpModelRule("exception-strategy-tests-model.json");
	
    @Override
    protected String getConfigResources()
    {
        return "exception-strategy-tests-config.xml";
    }

    @Test
    public void testRejectingExceptionStrategy() throws Exception
    {
    	String flowName = "amqpRejectingExceptionStrategy";
    	amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    			getFunctionalTestComponent(flowName), getTestTimeoutSecs());

    	Delivery response = amqpTestClient.consumeMessageWithAmqp(nameFactory.getQueueName(flowName),
        		getTestTimeoutSecs());

    	// check the message has been successfully pushed back to the queue
        assertThat(response, is(notNullValue()));
    }

    @Test
    @Ignore // MULE-8606
    public void testRedeliveryWithRollbackExceptionStrategy() throws Exception
    {
    	String flowName = "amqpTransactedRedeliveryWithRollbackExceptionStrategy";
         byte[] body = RandomStringUtils.randomAlphanumeric(20).getBytes();
         String correlationId = amqpTestClient.publishMessageWithAmqp(body,
            nameFactory.getExchangeName(flowName));

        for (int i = 0; i < 6; i++)
        {
             MuleMessage rolledBackMessage = muleContext.getClient().request(
                "vm://amqpTransactedRedeliveryWithRollbackExceptionStrategy.rollback",
                getTestTimeoutSecs() * 1000L);

            assertValidRedeliveredMessage(body, correlationId, rolledBackMessage);
        }

         MuleMessage exceededMessage = muleContext.getClient().request(
            "vm://amqpTransactedRedeliveryWithRollbackExceptionStrategy.exceeded",
            getTestTimeoutSecs() * 1000L);

        assertValidRedeliveredMessage(body, correlationId, exceededMessage);
    }

    private void assertValidRedeliveredMessage(byte[] body, String correlationId,
    	MuleMessage exceededMessage) throws Exception
    {
        assertThat(exceededMessage, is(notNullValue()));
        assertThat(exceededMessage.getCorrelationId(), is(equalTo(correlationId)));
        assertThat(exceededMessage.getPayloadAsBytes(), is(equalTo(body)));
    }
}
