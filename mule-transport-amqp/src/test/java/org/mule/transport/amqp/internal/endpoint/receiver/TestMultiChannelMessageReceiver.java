/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;

/**
 * A test class that overrides the logic to start subreceivers to verify that they are effectively started once the
 * consumer recovery thread recreates them.
 */
public class TestMultiChannelMessageReceiver extends MultiChannelMessageReceiver
{

    private boolean subReceiversStarted = false;

    private MultiChannelMessageSubReceiver mockSubreceiver;

    public TestMultiChannelMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint, MultiChannelMessageSubReceiver mockSubreceiver) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        this.mockSubreceiver = mockSubreceiver;
    }

    @Override
    protected void createSubreceivers() throws CreateException, InitialisationException
    {
        this.subReceivers.add(mockSubreceiver);
    }

    public MultiChannelMessageSubReceiver getMockSubreceiver()
    {
        return mockSubreceiver;
    }
}
