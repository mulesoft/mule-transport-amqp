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

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleMessage;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

public abstract class AbstractSslConnectivityITCase extends AbstractAmqpITCase
{
    public AbstractSslConnectivityITCase() throws IOException
    {
        super();
    }

    @Before
    public void ensureAmqpsTestsMustRun()
    {
        assumeTrue(BooleanUtils.toBoolean(System.getProperty("runAmqpsTests")));
    }

    @Test
    public void sslDispatchingAndReceiving() throws Exception
    {
        dispatchAndReceiveAMQPS("sslReceiver", "vm://sslDispatcher.in");
    }

    protected void dispatchAndReceiveAMQPS(String flowName, String vmDispatcher)
        throws Exception
    {
        final Future<MuleMessage> futureMuleMessage = setupFunctionTestComponentForFlow(flowName);

        final String testPayload = RandomStringUtils.randomAlphanumeric(20);

        muleContext.getClient().dispatch(vmDispatcher, testPayload, null);

        final MuleMessage muleMessage = futureMuleMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);

        assertThat(muleMessage.getPayloadAsString(), is(testPayload));
    }
}
