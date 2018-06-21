/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.ClassRule;
import org.junit.Test;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;

import com.rabbitmq.client.Delivery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExceptionStrategyItCase extends AbstractItCase
{

    private static final String EXCEEDED_QUEUE = "vm://amqpTransactedRedeliveryWithRollbackExceptionStrategy.exceeded";

    private static final String ROLLBACK_QUEUE = "vm://amqpTransactedRedeliveryWithRollbackExceptionStrategy.rollback";

    private static final String OK_QUEUE = "vm://amqpTransactedRedeliveryWithRollbackExceptionStrategy.ok";

    private static final String VALID_PAYLOAD = "VALID_PAYLOAD";

    private static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";

    @ClassRule
	public static AmqpModelRule modelRule = new AmqpModelRule("exception-strategy-tests-model.json");

    @Override
    protected boolean startMuleContext()
    {
        return false;
    }

    @Override
    protected String getConfigResources()
    {
        return "exception-strategy-tests-config.xml";
    }

    @Test
    public void testRejectingExceptionStrategy() throws Exception
    {
        muleContext.start();
    	String flowName = "amqpRejectingExceptionStrategy";
    	amqpTestClient.dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(nameFactory.getExchangeName(flowName),
    			getFunctionalTestComponent(flowName), getTestTimeoutSecs());

    	Delivery response = amqpTestClient.consumeMessageWithAmqp(nameFactory.getQueueName(flowName),
                                                                                   getTestTimeoutSecs());

    	// check the message has been successfully pushed back to the queue
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void testRedeliveryWithRollbackExceptionStrategy() throws Exception
    {
        muleContext.start();
        String flowName = "amqpTransactedRedeliveryWithRollbackExceptionStrategy";
        byte[] body = randomAlphanumeric(20).getBytes();
        String correlationId = amqpTestClient.publishMessageWithAmqp(body,
            nameFactory.getExchangeName(flowName));

        for (int i = 0; i < 6; i++)
        {
            sendAndAssertDispatchedMessage(ROLLBACK_QUEUE, body, correlationId);
        }

        sendAndAssertDispatchedMessage(EXCEEDED_QUEUE, body, correlationId);
    }

    @Test
    public void testRedeliveryWithRollbackAndManualAck() throws Exception
    {
        String flowName = "amqpTransactedRedeliveryWithRollbackExceptionStrategyWithManualAck";
        final int numberOfMessages = 200;

        List<String> correlationIds = new ArrayList<String>(numberOfMessages);

        for (int i = 0; i < numberOfMessages; i++)
        {
            String correlationId = amqpTestClient.publishMessageWithAmqp(i % 2 == 0 ? VALID_PAYLOAD.getBytes() : INVALID_PAYLOAD.getBytes(),
                                                  nameFactory.getExchangeName(flowName));
            correlationIds.add(correlationId);
        }

        muleContext.start();

        for (int i = 0; i < numberOfMessages / 2; i++)
        {
            sendAndAssertDispatchedMessage(OK_QUEUE, VALID_PAYLOAD.getBytes(), correlationIds);
            sendAndAssertDispatchedMessage(EXCEEDED_QUEUE, INVALID_PAYLOAD.getBytes(), correlationIds);
        }

    }

    private void sendAndAssertDispatchedMessage(String outboundPath, byte[] body, String correlationId) throws Exception
    {
        MuleMessage dispatchedMessage = muleContext.getClient().request(
                outboundPath,
                getTestTimeoutSecs() * 1000L);

        assertThat(dispatchedMessage, is(notNullValue()));
        assertThat(dispatchedMessage.getCorrelationId(), is(equalTo(correlationId)));
        assertThat(dispatchedMessage.getPayloadAsBytes(), is(equalTo(body)));
    }

    private void sendAndAssertDispatchedMessage(String outboundPath, byte[] body, Collection  correlationIds) throws Exception
    {
        MuleMessage dispatchedMessage = muleContext.getClient().request(
                outboundPath,
                10000);

        assertThat(dispatchedMessage, is(notNullValue()));
        assertThat(dispatchedMessage.getCorrelationId(), isIn(correlationIds));
        assertThat(dispatchedMessage.getPayloadAsBytes(), is(equalTo(body)));
    }

    public static class PayloadVerifier implements Callable
    {

        public Object onCall(MuleEventContext muleEventContext) throws Exception
        {

            if (!VALID_PAYLOAD.equals(muleEventContext.getMessage().getPayloadAsString()))
            {
                throw new IllegalStateException("Invalid payload");
            }

            return muleEventContext.getMessage().getPayloadAsString();

        }
    }
}
