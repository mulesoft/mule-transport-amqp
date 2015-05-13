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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;

public class ReturnHandlerItCase extends AbstractItCase
{
	@ClassRule
	public static AmqpModelRule modelRule = new AmqpModelRule("global-return-handler-tests-model.json");
	
    @Override
    protected String getConfigResources()
    {
        return "global-return-handler-tests-config.xml";
    }

    @Test
    public void testMandatoryDeliverySuccess() throws Exception
    {
        String flowName = "amqpMandatoryDeliveryWithGlobalHandlerSuccess";
        
        vmTestClient.dispatchTestMessageAndAssertValidReceivedMessage(
        		nameFactory.getVmName(flowName), 
        		nameFactory.getQueueName(flowName), 
        		getTestTimeoutSecs());
    }

    @Test
    public void testMandatoryDeliveryFailureGlobalHandler() throws Exception
    {
         String payload = RandomStringUtils.randomAlphanumeric(20);
         Future<MuleMessage> futureReturnedMessage = 
        	amqpTestClient.setupFunctionTestComponentForFlow(
        			getFunctionalTestComponent("globalReturnedMessageProcessor"));

         new MuleClient(muleContext).dispatch(
        		 nameFactory.getVmName("amqpMandatoryDeliveryFailureGlobalHandler"), payload, null);
        
         MuleMessage returnedMessage = futureReturnedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);
        
         assertThat(returnedMessage, is(notNullValue()));
         assertThat(returnedMessage.getPayloadAsString(), equalTo(payload));
    }

    @Test
    public void testMandatoryDeliveryFailureFlowHandler() throws Exception
    {
         String payload = RandomStringUtils.randomAlphanumeric(20);
         Future<MuleMessage> futureReturnedMessage = 
        	amqpTestClient.setupFunctionTestComponentForFlow(getFunctionalTestComponent("flowReturnedMessageProcessor"));
        
         new MuleClient(muleContext).dispatch(nameFactory.getVmName("amqpMandatoryDeliveryFailureFlowHandler"), payload, null);
        
         MuleMessage returnedMessage = futureReturnedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);
         assertThat(returnedMessage, is(notNullValue()));
         assertThat(returnedMessage.getPayloadAsString(), equalTo(payload));
    }
}
