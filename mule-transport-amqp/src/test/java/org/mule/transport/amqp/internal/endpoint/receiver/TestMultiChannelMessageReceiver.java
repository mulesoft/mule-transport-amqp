/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;

/**
 * A test class that overrides the logic to start subreceivers to verify that they
 * are effectively started once a notification from mule context guarantees that
 * the the latter has been already started.
 * Apart from that overriden method, functionality is similar to that in parent
 * class.
 */
public class TestMultiChannelMessageReceiver extends MultiChannelMessageReceiver
{

    private boolean subReceiversStarted = false; 
    
    public TestMultiChannelMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    protected void startSubReceivers()
    {
        subReceiversStarted = true;
    }

    public boolean isSubReceiversStarted()
    {
        return subReceiversStarted;
    }
}
