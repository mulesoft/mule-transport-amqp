/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.transport.amqp.harness.AbstractItCase;

import org.junit.Test;

public class DynamicRoutingKeyItCase extends AbstractItCase
{

    @Override
    protected String getConfigFile()
    {
        return "dynamic-routing-key-config.xml";
    }

    @Test
    public void testDynamicRoutingKey() throws Exception
    {
        runFlow("senderFlow");
        MuleMessage result = muleContext.getClient().request("vm://result", 5000);
        assertThat(result, is(notNullValue()));
        assertThat(result.getPayloadAsString(), is("testedDynamicRoutingKey"));
    }

}
