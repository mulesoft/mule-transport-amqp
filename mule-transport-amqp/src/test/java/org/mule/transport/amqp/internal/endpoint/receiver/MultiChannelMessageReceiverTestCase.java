/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.transport.amqp.internal.endpoint.receiver.MultiChannelMessageReceiver.MULE_ASYNC_CONSUMERS_STARTUP;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.connector.AmqpConnectorFlowConstruct;

/**
 * A Unit test to verify that the subreceivers are initiated once a notification from the mule context is received so
 * that they are started after the app is fully deployed.
 */
@RunWith(MockitoJUnitRunner.class)
public class MultiChannelMessageReceiverTestCase extends AbstractMuleContextTestCase
{

    @Rule
    public SystemProperty verbose = new SystemProperty(MULE_ASYNC_CONSUMERS_STARTUP, "true");

    @Mock
    public MultiChannelMessageSubReceiver mockSubreceiver;

    private static long POLLING_PROBER_TIMEOUT = 20000;
    private static long POLLING_PROBER_DELAY = 2000;

    @Test
    public void testSubreceiversAreStarted() throws Exception
    {
        when(mockSubreceiver.isCancelled()).thenReturn(true);
        final InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(getEndpointURI());
        final TestMultiChannelMessageReceiver receiver = new TestMultiChannelMessageReceiver(endpoint.getConnector(),
                new AmqpConnectorFlowConstruct((AmqpConnector) endpoint.getConnector()), endpoint, mockSubreceiver);
        receiver.doConnect();
        new PollingProber(POLLING_PROBER_TIMEOUT, POLLING_PROBER_DELAY).check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(mockSubreceiver).consume();
                }
                catch (Exception e)
                {
                    return false;
                }

                return true;

            }

            public String describeFailure()
            {
                return "Subreceivers were not started";
            }
        });

    }

    public String getEndpointURI()
    {
        return "amqp://target-exchange/target-queue";
    }
}
