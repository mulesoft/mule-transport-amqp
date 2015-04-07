/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.util.UUID;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.QueueingConsumer.Delivery;

public abstract class AbstractAmqpOutboundITCase extends AbstractAmqpITCase
{
    public AbstractAmqpOutboundITCase() throws IOException
    {
        super();
    }

    protected void dispatchTestMessageAndAssertValidReceivedMessage(final String flowName) throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage(flowName, Collections.<String, String> emptyMap());
    }

    protected void dispatchTestMessageAndAssertValidReceivedMessage(final String flowName,
                                                                    final Map<String, String> properties)
        throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final String customHeaderValue = dispatchTestMessage(flowName, properties, payload);

        fetchAndValidateAmqpDeliveredMessage(flowName, payload, customHeaderValue);
    }

    protected void fetchAndValidateAmqpDeliveredMessage(final String flowName,
                                                final String expectedPayload,
                                                final String expectedCustomHeaderValue)
        throws IOException, InterruptedException
    {
        final Delivery dispatchedMessage = consumeMessageWithAmqp(getQueueName(flowName),
            getTestTimeoutSecs() * 1000L);

        assertNotNull(dispatchedMessage);

        validateAmqpDeliveredMessage(expectedPayload, expectedCustomHeaderValue, dispatchedMessage.getBody(),
            dispatchedMessage.getProperties());
    }

    protected void validateAmqpDeliveredMessage(final String expectedPayload,
                                                 final String expectedCustomHeaderValue,
                                                 final byte[] body,
                                                 final BasicProperties basicProperties)
    {
        assertEquals(expectedPayload, new String(body));
        assertEquals(expectedCustomHeaderValue, basicProperties.getHeaders().get("customHeader").toString());
    }

    protected String dispatchTestMessage(final String flowName,
                                         final Map<String, String> properties,
                                         final String payload) throws MuleException
    {
        final Map<String, String> actualProperties = new HashMap<String, String>(properties);

        final String customHeaderValue = UUID.getUUID();
        actualProperties.put("customHeader", customHeaderValue);

        new MuleClient(muleContext).dispatch("vm://" + flowName + ".in", payload, actualProperties);
        return customHeaderValue;
    }
}
