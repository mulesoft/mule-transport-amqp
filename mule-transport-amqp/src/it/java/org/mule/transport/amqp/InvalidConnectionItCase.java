/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.transport.amqp.harness.AbstractItCase;

import org.junit.Test;


public class InvalidConnectionItCase extends AbstractItCase
{

    @Override
    protected String getConfigResources()
    {
        return "connection-error-config.xml";
    }

    @Override
    protected boolean startMuleContext()
    {
        return false;
    }

    @Test
    public void testInvalidQueueName() throws Exception
    {
        muleContext.start();
        // An error is logged when the notification that the context is started arrives.
        // That will stop the connector.
        assertThat(muleContext.getRegistry().lookupConnector("NonPersistent").getLifecycleState().isStopped(), is(true));
    }
}
