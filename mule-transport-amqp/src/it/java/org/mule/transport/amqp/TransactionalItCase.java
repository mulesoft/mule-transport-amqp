/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;

import com.rabbitmq.client.Delivery;

public class TransactionalItCase extends AbstractItCase
{
    private static String FLOW_NAME = "amqpTransactionalSupport";

	private static final String TEST_PAYLOAD = "testPayload";

    @ClassRule
	public static AmqpModelRule modelRule = new AmqpModelRule("transactional-tests-model.json");
	
	@Override
	protected String getConfigResources()
	{
		return "transactional-tests-config.xml";
	}
	 
    @Test
    public void testTransactionalSupport() throws Exception
    {
        muleContext.getClient().dispatch(nameFactory.getVmName(FLOW_NAME),
        	new DefaultMuleMessage(TEST_PAYLOAD, muleContext));

        // should have received two messages in the queue
        List<Delivery> dispatchedMessage = 
        	amqpTestClient.consumeMessagesWithAmqp(nameFactory.getQueueName(FLOW_NAME),
        		getTestTimeoutSecs() * 1000L, 2);
        
        assertThat(dispatchedMessage, is(notNullValue()));
        assertThat(dispatchedMessage, hasSize(2));
        assertThat(new String(dispatchedMessage.get(0).getBody()), is(equalTo(TEST_PAYLOAD)));
        assertThat(new String(dispatchedMessage.get(1).getBody()), is(equalTo(TEST_PAYLOAD)));
    }
}
