/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.transport.amqp.internal.processor.ChannelUtils.ACK_CHANNEL_ACTION;
import static org.mule.transport.amqp.internal.processor.ChannelUtils.getChannelOrFail;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;
import org.mule.transport.amqp.internal.client.ChannelMessageProperty;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.processor.Acknowledger;
import org.mule.transport.amqp.internal.processor.ChannelUtils;

import com.rabbitmq.client.Channel;

public class MessageRequesterItCase extends AbstractItCase
{
	@ClassRule
	public static AmqpModelRule modelRule = new AmqpModelRule("message-requester-tests-model.json");
	
    @Override
    protected String getConfigResources()
    {
        return "message-requester-tests-config.xml";
    }

    @Test
    public void testAutoAcknowledgment() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpAutoAckRequester",
            "amqpAutoAckLocalhostConnector");
    }

    @Test
    public void testChannelMessagePropertyCorrectlyPropagated() throws Exception
    {
        MuleMessage receivedMessage = dispatchTestMessageAndAssertValidReceivedMessage(
                "amqpManualAckRequester", "amqpManualAckLocalhostConnector");
        Channel channel = getChannelOrFail(receivedMessage, ACK_CHANNEL_ACTION);
        assertThat(channel, is(not(nullValue())));
    }
    
    @Test
    public void testMuleAcknowledgment() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpMuleAckRequester",
            "amqpMuleAckLocalhostConnector");
    }

    @Test
    public void testManualAcknowledgment() throws Exception
    {
        MuleMessage receivedMessage = dispatchTestMessageAndAssertValidReceivedMessage(
                "amqpManualAckRequester", "amqpManualAckLocalhostConnector");
        Channel channel = getChannelOrFail(receivedMessage, ACK_CHANNEL_ACTION);
        new Acknowledger().ack(receivedMessage, false);
    }

    @Test
    public void testTimeOut() throws Exception
    {
        long startTime = System.nanoTime();

        MuleMessage receivedMessage = new MuleClient(muleContext).request(
            "amqp://amqp-queue." + nameFactory.getQueueName("amqpTimeOutRequester")
                            + "?connector=amqpAutoAckLocalhostConnector", 2500L);

        assertThat(receivedMessage, is(nullValue()));
        long durationNano = System.nanoTime() - startTime;
        assertTrue(TimeUnit.MILLISECONDS.convert(durationNano, TimeUnit.NANOSECONDS) >= 2400L);
    }

    @Test
    public void testMultipleRequests() throws Exception
    {
        String queueName = "amqpTestMultipleRequests";

        try
        {
            channel.queueDeclare(queueName, true, false, false, Collections.<String, Object> emptyMap());

             String requestedUrl = "amqp://" + AmqpConnector.ENDPOINT_DEFAULT_EXCHANGE_ALIAS + "/amqp-queue."
                                        + queueName + "?connector=amqpAutoAckLocalhostConnector";

            // try with messages already waiting
             byte[] body1 = RandomStringUtils.randomAlphanumeric(20).getBytes();
             String correlationId1 = amqpTestClient.publishMessageWithAmqpToDefaultExchange(body1, queueName);
             byte[] body2 = RandomStringUtils.randomAlphanumeric(20).getBytes();
             String correlationId2 = amqpTestClient.publishMessageWithAmqpToDefaultExchange(body2, queueName);

            MuleMessage receivedMessage = new MuleClient(muleContext).request(requestedUrl,
                getTestTimeoutSecs() * 1000L);

            amqpTestClient.assertValidReceivedMessage(correlationId1, body1, receivedMessage);

            receivedMessage = new MuleClient(muleContext).request(requestedUrl, getTestTimeoutSecs() * 1000L);
            amqpTestClient.assertValidReceivedMessage(correlationId2, body2, receivedMessage);

            // now try with a new message
             byte[] body3 = RandomStringUtils.randomAlphanumeric(20).getBytes();
             String correlationId3 = 
            		amqpTestClient.publishMessageWithAmqpToDefaultExchange(body3, queueName);

            receivedMessage = new MuleClient(muleContext).request(requestedUrl, getTestTimeoutSecs() * 1000L);
            amqpTestClient.assertValidReceivedMessage(correlationId3, body3, receivedMessage);

            // try with no message, with and without wait
            receivedMessage = new MuleClient(muleContext).request(requestedUrl, 2500L);
            assertThat(receivedMessage, is(nullValue()));

            receivedMessage = new MuleClient(muleContext).request(requestedUrl, 0L);
            assertThat(receivedMessage, is(nullValue()));
        }
        finally
        {
            channel.queueDelete(queueName);
        }
    }

    private MuleMessage dispatchTestMessageAndAssertValidReceivedMessage( String flowName,
    	 String connectorName) throws Exception
    {
        byte[] body = RandomStringUtils.randomAlphanumeric(20).getBytes();
        String correlationId = amqpTestClient.publishMessageWithAmqp(body, 
        	nameFactory.getExchangeName(flowName));

        MuleMessage receivedMessage = 
            new MuleClient(muleContext).request("amqp://amqp-queue."
                + nameFactory.getQueueName(flowName)
                + "?connector="
                + connectorName,
                getTestTimeoutSecs() * 1000L);

        amqpTestClient.assertValidReceivedMessage(correlationId, body, receivedMessage);

        return receivedMessage;
    }
}
