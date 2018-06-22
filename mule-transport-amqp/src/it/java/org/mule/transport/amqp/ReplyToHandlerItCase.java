/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelCleanupRule;
import org.mule.util.ArrayUtils;

public class ReplyToHandlerItCase extends AbstractItCase
{
	private static final String TEST_PAYLOAD = "testPayload";
	
	@ClassRule
	public static AmqpModelCleanupRule modelCleanupRule = new AmqpModelCleanupRule(
			new String[] {"amqpReplierService-queue", "amqpReplyTargetService-queue"},
			new String[] {"amqpReplierService-exchange", "amqpReplyTargetService-exchange"});
	
    @Override
    protected String getConfigResources()
    {
        return "reply-to-tests-config.xml";
    }

    @Test
    public void testReplyTo() throws Exception
    {
        Future<MuleMessage> futureReceivedMessage = 
        		amqpTestClient.setupFunctionTestComponentForFlow(
        				getFunctionalTestComponent("amqpReplyTargetService"));

        String correlationId = amqpTestClient.publishMessageWithAmqp(TEST_PAYLOAD.getBytes(),
        		"amqpReplierService-exchange", "amqpReplyTargetService-queue");

        MuleMessage receivedMessage = futureReceivedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);

        amqpTestClient.assertValidReceivedMessage(correlationId, 
        		ArrayUtils.addAll(TEST_PAYLOAD.getBytes(), "-reply".getBytes()), receivedMessage);
    }

}
