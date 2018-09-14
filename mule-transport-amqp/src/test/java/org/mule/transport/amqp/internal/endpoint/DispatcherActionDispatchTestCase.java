/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Test;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.mule.transport.amqp.internal.endpoint.dispatcher.AmqpBlockedBrokerException;
import org.mule.transport.amqp.internal.endpoint.dispatcher.DispatcherActionDispatch;

import com.rabbitmq.client.Channel;

public class DispatcherActionDispatchTestCase extends AbstractMuleContextTestCase
{

    private static final String DUMMY_ROUTING_KEY = "routing-key";
    private static final String DUMMY_EXCHANGE = "dummy-exchange";

    @Test(expected = AmqpBlockedBrokerException.class)
    public void dispatchWithBlockingServerAndFailOnBlockedServer() throws Exception
    {
        testWith(true, true);
    }

    @Test
    public void dispatchWithBlockingServerAndNoFailOnBlockedServer() throws Exception
    {
        testWith(true, false);
    }

    @Test
    public void dispatchWithNoBlockingServerAndNoFailOnBlockedServer() throws Exception
    {
        testWith(false, false);
    }

    private void testWith(boolean blocked, boolean failOnBlockedBroker) throws IOException
    {
        DispatcherActionDispatch dispatchAction = new DispatcherActionDispatch();
        AmqpConnector amqpConnector = mock(AmqpConnector.class);
        Channel channel = mock(Channel.class);
        when(amqpConnector.isBlocked()).thenReturn(blocked);
        when(amqpConnector.isFailOnBlockedBroker()).thenReturn(failOnBlockedBroker);
        AmqpMessage amqpMessage = mock(AmqpMessage.class);
        dispatchAction.run(amqpConnector, channel, DUMMY_EXCHANGE, DUMMY_ROUTING_KEY, amqpMessage, 3000);
    }
}
