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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

public class AmqpMessageRequesterITCase extends AbstractAmqpITCase
{
    public AmqpMessageRequesterITCase() throws IOException
    {
        super();

        // create the required pre-existing exchanges and queues
        setupExchangeAndQueue("amqpAutoAckRequester");
        setupExchangeAndQueue("amqpMuleAckRequester");
        setupExchangeAndQueue("amqpManualAckRequester");
        setupExchangeAndQueue("amqpTimeOutRequester");
    }

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
    public void testMuleAcknowledgment() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpMuleAckRequester",
            "amqpMuleAckLocalhostConnector");
    }

    @Test
    public void testManualAcknowledgment() throws Exception
    {
        final MuleMessage receivedMessage = dispatchTestMessageAndAssertValidReceivedMessage(
            "amqpManualAckRequester", "amqpManualAckLocalhostConnector");

        AmqpMessageAcknowledger.ack(receivedMessage, false);
    }

    @Test
    public void testTimeOut() throws Exception
    {
        final long startTime = System.nanoTime();

        final MuleMessage receivedMessage = new MuleClient(muleContext).request(
            "amqp://amqp-queue." + getQueueName("amqpTimeOutRequester")
                            + "?connector=amqpAutoAckLocalhostConnector", 2500L);

        assertNull(receivedMessage);
        final long durationNano = System.nanoTime() - startTime;
        assertTrue(TimeUnit.MILLISECONDS.convert(durationNano, TimeUnit.NANOSECONDS) >= 2400L);
    }

    @Test
    public void testMultipleRequests() throws Exception
    {
        final String queueName = "amqpTestMultipleRequests";

        try
        {
            getChannel().queueDeclare(queueName, true, false, false, Collections.<String, Object> emptyMap());

            final String requestedUrl = "amqp://" + AmqpConstants.DEFAULT_EXCHANGE_ALIAS + "/amqp-queue."
                                        + queueName + "?connector=amqpAutoAckLocalhostConnector";

            // try with messages already waiting
            final byte[] body1 = RandomStringUtils.randomAlphanumeric(20).getBytes();
            final String correlationId1 = publishMessageWithAmqpToDefaultExchange(body1, queueName);
            final byte[] body2 = RandomStringUtils.randomAlphanumeric(20).getBytes();
            final String correlationId2 = publishMessageWithAmqpToDefaultExchange(body2, queueName);

            MuleMessage receivedMessage = new MuleClient(muleContext).request(requestedUrl,
                getTestTimeoutSecs() * 1000L);

            assertValidReceivedMessage(correlationId1, body1, receivedMessage);

            receivedMessage = new MuleClient(muleContext).request(requestedUrl, getTestTimeoutSecs() * 1000L);
            assertValidReceivedMessage(correlationId2, body2, receivedMessage);

            // now try with a new message
            final byte[] body3 = RandomStringUtils.randomAlphanumeric(20).getBytes();
            final String correlationId3 = publishMessageWithAmqpToDefaultExchange(body3, queueName);

            receivedMessage = new MuleClient(muleContext).request(requestedUrl, getTestTimeoutSecs() * 1000L);
            assertValidReceivedMessage(correlationId3, body3, receivedMessage);

            // try with no message, with and without wait
            receivedMessage = new MuleClient(muleContext).request(requestedUrl, 2500L);
            assertNull(receivedMessage);

            receivedMessage = new MuleClient(muleContext).request(requestedUrl, 0L);
            assertNull(receivedMessage);
        }
        finally
        {
            getChannel().queueDelete(queueName);
        }
    }

    private MuleMessage dispatchTestMessageAndAssertValidReceivedMessage(final String flowName,
                                                                         final String connectorName)
        throws Exception
    {
        final byte[] body = RandomStringUtils.randomAlphanumeric(20).getBytes();
        final String correlationId = publishMessageWithAmqp(body, flowName);

        final MuleMessage receivedMessage = new MuleClient(muleContext).request("amqp://amqp-queue."
                                                                                + getQueueName(flowName)
                                                                                + "?connector="
                                                                                + connectorName,
            getTestTimeoutSecs() * 1000L);

        assertValidReceivedMessage(correlationId, body, receivedMessage);

        return receivedMessage;
    }
}
