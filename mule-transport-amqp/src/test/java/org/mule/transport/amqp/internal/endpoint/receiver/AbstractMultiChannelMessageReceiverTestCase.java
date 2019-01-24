/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.connector.AmqpConnectorFlowConstruct;

/**
 * A Unit test to verify that the subreceivers are initiated once a notification from the mule context is received so
 * that they are started after the app is fully deployed.
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractMultiChannelMessageReceiverTestCase extends AbstractMuleContextTestCase
{
    @Mock
    public MultiChannelMessageSubReceiver mockSubreceiver;

    private static long TIMEOUT = 20000;
    private static long POLLING_PROBER_DELAY = 2000;

    protected void testSubreceiversAreStarted(boolean primaryPollingInstance, boolean listenOnPrimaryNodeOnly) throws MuleException, CreateException, Exception
    {
        setupConnector(primaryPollingInstance, listenOnPrimaryNodeOnly);
        new PollingProber(TIMEOUT, POLLING_PROBER_DELAY).check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(mockSubreceiver).consume();
                }
                catch (Throwable e)
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

    protected void setupConnector(boolean primaryPollingInstance, boolean listenOnPrimaryNodeOnly) throws MuleException, CreateException, Exception
    {
        when(mockSubreceiver.isCancelled()).thenReturn(true);
        MuleContext spiedMuleContext = spy(muleContext);
        when(spiedMuleContext.isPrimaryPollingInstance()).thenReturn(primaryPollingInstance);
        final InboundEndpoint endpoint = spiedMuleContext.getEndpointFactory().getInboundEndpoint(getEndpointURI());
        AmqpConnector connector = new AmqpConnector(spiedMuleContext);
        connector.setListenOnPrimaryNodeOnly(listenOnPrimaryNodeOnly);
        final TestMultiChannelMessageReceiver receiver = new TestMultiChannelMessageReceiver(connector,
                new AmqpConnectorFlowConstruct(connector), endpoint, mockSubreceiver);
        receiver.doConnect();
    }


    public String getEndpointURI()
    {
        return "amqp://target-exchange/target-queue";
    }
}
