/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.context.notification.MuleContextNotification.CONTEXT_STARTED;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.context.notification.MuleContextNotification;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.connector.AmqpConnectorFlowConstruct;

import org.junit.Test;

/**
 * A Unit test to verify that the subreceivers are initiated once a notification from the mule context is received so
 * that they are started after the app is fully deployed.
 */
public class MultiChannelMessageReceiverTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testCreateFromFactory() throws Exception
    {
        final InboundEndpoint endpoint = muleContext.getEndpointFactory()
                                                    .getInboundEndpoint(getEndpointURI());
        TestMultiChannelMessageReceiver receiver = new TestMultiChannelMessageReceiver(endpoint.getConnector(), new AmqpConnectorFlowConstruct((AmqpConnector) endpoint.getConnector()), endpoint);
        receiver.doConnect();
        assertThat(receiver.isSubReceiversStarted(), is(false));
        muleContext.fireNotification(new MuleContextNotification(muleContext, CONTEXT_STARTED));
        assertThat(receiver.isSubReceiversStarted(), is(true));
    }

    public String getEndpointURI()
    {
        return "amqp://target-exchange/target-queue";
    }
}
