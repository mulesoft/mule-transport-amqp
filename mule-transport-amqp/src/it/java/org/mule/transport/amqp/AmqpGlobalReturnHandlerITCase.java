/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

public class AmqpGlobalReturnHandlerITCase extends AbstractAmqpOutboundITCase
{
    public AmqpGlobalReturnHandlerITCase() throws Exception
    {
        super();
        // create/delete the required pre-existing exchanges and queues
        setupExchangeAndQueue("amqpMandatoryDeliveryWithGlobalHandlerSuccess");
        setupExchange("amqpMandatoryDeliveryFailureGlobalHandler");
        setupExchange("amqpMandatoryDeliveryFailureFlowHandler");
    }

    @Override
    protected String getConfigResources()
    {
        return "global-return-handler-tests-config.xml";
    }

    @Test
    public void testMandatoryDeliverySuccess() throws Exception
    {
        dispatchTestMessageAndAssertValidReceivedMessage("amqpMandatoryDeliveryWithGlobalHandlerSuccess");
    }

    @Test
    public void testMandatoryDeliveryFailureGlobalHandler() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final Future<MuleMessage> futureReturnedMessage = setupFunctionTestComponentForFlow("globalReturnedMessageProcessor");
        new MuleClient(muleContext).dispatch("vm://amqpMandatoryDeliveryFailureGlobalHandler.in", payload,
            null);
        final MuleMessage returnedMessage = futureReturnedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);
        assertNotNull(returnedMessage);
        assertEquals(payload, returnedMessage.getPayloadAsString());
    }

    @Test
    public void testMandatoryDeliveryFailureFlowHandler() throws Exception
    {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final Future<MuleMessage> futureReturnedMessage = setupFunctionTestComponentForFlow("flowReturnedMessageProcessor");
        new MuleClient(muleContext).dispatch("vm://amqpMandatoryDeliveryFailureFlowHandler.in", payload, null);
        final MuleMessage returnedMessage = futureReturnedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);
        assertNotNull(returnedMessage);
        assertEquals(payload, returnedMessage.getPayloadAsString());
    }
}
