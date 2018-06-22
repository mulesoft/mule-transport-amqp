/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Field;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Test;

public class AmqpConnectorThreadingProfileTestCase extends FunctionalTestCase
{

    private static final int MAX_POOL_SIZE = 20;

    @Override
    protected String getConfigResources()
    {
        return "amqp-threading-profile-config.xml";
    }

    @Test
    public void verifyMaxThreadsReceiverThreadingProfileProperties() throws Exception
    {
        final AmqpConnector c = (AmqpConnector) muleContext.getRegistry().lookupConnector("amqpDefaultConnector");
        Field field = c.getClass().getDeclaredField("receiverExecutor");
        field.setAccessible(true);
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) field.get(c);
        assertThat(executorService.getMaximumPoolSize(), equalTo(MAX_POOL_SIZE));
    }
    
}
