/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import static org.mule.transport.amqp.internal.endpoint.receiver.MultiChannelMessageReceiver.MULE_ASYNC_CONSUMERS_STARTUP;

import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.rule.SystemProperty;

/**
 * A Unit test to verify that the subreceivers are initiated once a notification from the mule context is received so
 * that they are started after the app is fully deployed in a primary polling instance.
 */
public class PrimaryPollingMultiChannelMessageReceiverTestCase extends AbstractMultiChannelMessageReceiverTestCase
{

    @Rule
    public SystemProperty verbose = new SystemProperty(MULE_ASYNC_CONSUMERS_STARTUP, "true");

    @Test
    public void testSubreceiversAreStartedWhenPrimaryNodeAndOnlyListenOnPrimaryNode() throws Exception
    {
        testSubreceiversAreStarted(true, false);
    }

    @Test
    public void testSubreceiversAreStartedWhenPrimaryNodeAndAlwaysListen() throws Exception
    {
        testSubreceiversAreStarted(true, true);
    }
}
