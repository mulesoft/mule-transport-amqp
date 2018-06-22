/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.api.transport.PropertyScope.INVOCATION;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.transport.amqp.harness.AbstractItCase;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;
import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Test;

public class DynamicRoutingKeyItCase extends AbstractItCase
{

    @ClassRule
    public static AmqpModelRule modelRule = new AmqpModelRule("dynamic-routing-key-config.json");
    
    @Override
    protected String getConfigFile()
    {
        return "dynamic-routing-key-config.xml";
    }

    @Test
    public void testValidDynamicRoutingKeys() throws Exception
    {
        verifyPayload(sendMessageAndAssertReply("testRoutingKeyValue", notNullValue()));
        verifyPayload(sendMessageAndAssertReply("testRoutingKeyValue2", notNullValue()));
    }

    @Test
    public void testInvalidRoutingKey() throws Exception
    {
        sendMessageAndAssertReply("testInvalidRoutingKey", nullValue());
    }

    private String sendMessageAndAssertReply(String routingKeyValue, Matcher matcher) throws Exception
    {
        MuleEvent event = getTestEvent("Testing");
        event.getMessage().setProperty("testRoutingKey", routingKeyValue, INVOCATION);
        testFlow("senderFlow", event);
        MuleMessage result = muleContext.getClient().request("vm://result", 2000);
        assertThat(result, is(matcher));
        return result != null ? (String) result.getPayload() : null;
    }

    private void verifyPayload(String resultPayload)
    {
        assertThat(resultPayload, is("testedDynamicRoutingKey"));
    }

}
