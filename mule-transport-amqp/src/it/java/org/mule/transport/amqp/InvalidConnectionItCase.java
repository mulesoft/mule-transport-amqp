/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import org.junit.Test;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.transport.amqp.harness.AbstractItCase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        try
        {
            muleContext.start();
            fail("Should have failed to start the AMQP channel with invalid queue name");
        }
        catch (LifecycleException e)
        {
            assertTrue(muleContext.getRegistry().lookupConnector("NonPersistent").getLifecycleState().isStopped());
        }
    }
}
