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

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.mule.api.MuleMessage;

public abstract class AbstractAmqpInboundITCase extends AbstractAmqpITCase
{
    public AbstractAmqpInboundITCase() throws IOException
    {
        super();
    }

    protected void dispatchTestMessageAndAssertValidReceivedMessage(final String flowName) throws Exception
    {
        final Future<MuleMessage> futureReceivedMessage = setupFunctionTestComponentForFlow(flowName);

        final byte[] body = RandomStringUtils.randomAlphanumeric(20).getBytes();
        final String correlationId = publishMessageWithAmqp(body, flowName);

        final MuleMessage receivedMessage = futureReceivedMessage.get(getTestTimeoutSecs(), TimeUnit.SECONDS);

        assertValidReceivedMessage(correlationId, body, receivedMessage);
    }
}
