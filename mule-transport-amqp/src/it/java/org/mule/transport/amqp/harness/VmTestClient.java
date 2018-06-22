/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.harness;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.tck.MuleTestUtils;
import org.mule.util.UUID;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Delivery;

public class VmTestClient 
{
	protected MuleContext muleContext; 
	
	protected AmqpTestClient amqpTestClient;
	
	public VmTestClient(MuleContext muleContext, AmqpTestClient amqpTestClient)
	{
		this.muleContext = muleContext;
		this.amqpTestClient = amqpTestClient;
	}
	
	public void dispatchTestMessageAndAssertValidReceivedMessage(String vmQueueName, 
		String amqpQueueName, int timeoutInSeconds) throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage(vmQueueName, amqpQueueName,
        	Collections.<String, String> emptyMap(), timeoutInSeconds);
    }

	public void dispatchTestMessageAndAssertValidReceivedMessage(String vmQueueName, 
		String amqpQueueName, Map<String, String> properties, int timeoutInSeconds)
					throws Exception
    {
		String payload = RandomStringUtils.randomAlphanumeric(20);
		String customHeaderValue = dispatchTestMessage(vmQueueName, properties, payload);

        fetchAndValidateAmqpDeliveredMessage(amqpQueueName, timeoutInSeconds, payload, customHeaderValue);
    }

	public void fetchAndValidateAmqpDeliveredMessage(String amqpQueueName, int timeoutInSeconds,
		String expectedPayload, String expectedCustomHeaderValue)
			throws IOException, InterruptedException
    {
		Delivery dispatchedMessage = amqpTestClient.consumeMessageWithAmqp(amqpQueueName, timeoutInSeconds * 1000L);

		assertThat(dispatchedMessage, is(notNullValue()));

		validateAmqpDeliveredMessage(expectedPayload, expectedCustomHeaderValue, dispatchedMessage.getBody(),
			dispatchedMessage.getProperties());
    }

	public void validateAmqpDeliveredMessage(String expectedPayload, String expectedCustomHeaderValue, 
		byte[] body, BasicProperties basicProperties)
    {
		assertThat(new String(body), is(equalTo(expectedPayload)));
		assertThat(basicProperties.getHeaders().get("customHeader").toString(), 
			is(equalTo(expectedCustomHeaderValue)));
    }

	public String dispatchTestMessage(String vmQueueName,
			Map<String, String> properties, String payload) throws Exception
    {
        Map<String, Object> actualProperties = new HashMap<String, Object>(properties);

        String customHeaderValue = UUID.getUUID();
        actualProperties.put("customHeader", customHeaderValue);
        
        MuleEvent event = MuleTestUtils.getTestEvent(payload, muleContext);
        event.getMessage().addProperties(actualProperties, PropertyScope.OUTBOUND);
        
        new MuleClient(muleContext).dispatch(vmQueueName, payload, actualProperties);
        return customHeaderValue;
    }
    
}
