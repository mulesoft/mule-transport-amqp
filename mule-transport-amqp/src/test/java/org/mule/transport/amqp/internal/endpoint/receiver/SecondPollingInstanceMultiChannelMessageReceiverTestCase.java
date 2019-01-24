/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mule.transport.amqp.internal.endpoint.receiver.MultiChannelMessageReceiver.MULE_ASYNC_CONSUMERS_STARTUP;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.CreateException;
import org.mule.tck.junit4.rule.SystemProperty;

/**
 * A Unit test to verify that the subreceivers are not started if we are in a secondary polling instance
 * and the listenOnPrimaryNodeOnly is true
 */
public class SecondPollingInstanceMultiChannelMessageReceiverTestCase extends AbstractMultiChannelMessageReceiverTestCase
{
    @Rule
    public SystemProperty verbose = new SystemProperty(MULE_ASYNC_CONSUMERS_STARTUP, "false");

    @Test
    public void testSubreceiversAreNotStartedWhenNotPrimaryNodeAndOnlyListenOnPrimaryNode() throws Exception
    {
        testSubreceiversAreNotStarted(false, true);
    }

    @Test
    public void testSubreceiversAreNotStartedWhenNotPrimaryNodeAndNotOnlyListenOnPrimaryNode() throws Exception
    {
        testSubreceiversAreNotStarted(false, false);
    }
    

    private void testSubreceiversAreNotStarted(boolean primaryPollingInstance, boolean listenOnPrimaryNodeOnly) throws MuleException, CreateException, Exception
    {
        setupConnector(primaryPollingInstance, listenOnPrimaryNodeOnly);
        verify(mockSubreceiver, never()).consume();
    }
}
