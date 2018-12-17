/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.ClassRule;
import org.junit.Test;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.amqp.harness.rules.AmqpModelRule;
import org.mule.transport.amqp.internal.connector.AmqpConnector;

public class AmqpReceiverExecutorServiceItCase extends FunctionalTestCase
{
    @ClassRule
    public static AmqpModelRule modelRule = new AmqpModelRule("receiver-executor-service-config.json");

    @Override
    protected String getConfigResources()
    {
        return "receiver-executor-service-config.xml";
    }

    @Test
    public void testBothExecutorServiceNames() throws Exception
    {
        assertThat(((AmqpConnector) muleContext.getRegistry().lookupConnector("amqpConnector1")).getExecutorName(),
                equalTo("amqpConnector1-amqpReceiver"));
        assertThat(((AmqpConnector) muleContext.getRegistry().lookupConnector("amqpConnector2")).getExecutorName(),
                equalTo("amqpConnector2-amqpReceiver"));
    }
}
