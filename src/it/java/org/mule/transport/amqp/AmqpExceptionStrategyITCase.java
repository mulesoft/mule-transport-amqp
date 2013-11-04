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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.mule.api.MuleMessage;

public class AmqpExceptionStrategyITCase extends AbstractAmqpInboundITCase
{
    public AmqpExceptionStrategyITCase() throws Exception
    {
        super();
        // create/delete the required pre-existing exchanges and queues
        setupExchangeAndQueue("amqpRejectingExceptionStrategy");
        setupExchangeAndQueue("amqpTransactedRedeliveryWithRollbackExceptionStrategy");
    }

    @Override
    protected String getConfigResources()
    {
        return "exception-strategy-tests-config.xml";
    }

    @Test
    public void testRejectingExceptionStrategy() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpRejectingExceptionStrategy");

        // check the message has been successfully pushed back to the queue
        assertNotNull(consumeMessageWithAmqp(getQueueName("amqpRejectingExceptionStrategy"),
            getTestTimeoutSecs()));
    }

    @Test
    public void testRedeliveryWithRollbackExceptionStrategy() throws Exception
    {
        final byte[] body = RandomStringUtils.randomAlphanumeric(20).getBytes();
        final String correlationId = publishMessageWithAmqp(body,
            "amqpTransactedRedeliveryWithRollbackExceptionStrategy");

        for (int i = 0; i < 6; i++)
        {
            final MuleMessage rolledBackMessage = muleContext.getClient().request(
                "vm://amqpTransactedRedeliveryWithRollbackExceptionStrategy.rollback",
                getTestTimeoutSecs() * 1000L);

            assertValidRedeliveredMessage(body, correlationId, rolledBackMessage);
        }

        final MuleMessage exceededMessage = muleContext.getClient().request(
            "vm://amqpTransactedRedeliveryWithRollbackExceptionStrategy.exceeded",
            getTestTimeoutSecs() * 1000L);

        assertValidRedeliveredMessage(body, correlationId, exceededMessage);
    }

    private void assertValidRedeliveredMessage(final byte[] body,
                                               final String correlationId,
                                               final MuleMessage exceededMessage) throws Exception
    {
        assertNotNull(exceededMessage);
        assertEquals(correlationId, exceededMessage.getCorrelationId());
        assertArrayEquals(body, exceededMessage.getPayloadAsBytes());
    }
}
