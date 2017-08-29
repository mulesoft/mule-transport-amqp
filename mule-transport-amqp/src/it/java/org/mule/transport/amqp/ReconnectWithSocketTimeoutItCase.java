/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.probe.PollingProber.DEFAULT_TIMEOUT;

import org.junit.Before;
import org.junit.Test;

import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.transport.amqp.internal.connector.AmqpConnector;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

public class ReconnectWithSocketTimeoutItCase extends FunctionalTestCase
{
    private static Integer retries  = 0;
    private static Integer POLL_TIMEOUT = 10;

    private AmqpConnector connector = null;

    public ReconnectWithSocketTimeoutItCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "reconnect-socket-timeout-config.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class, RETURNS_DEEP_STUBS);
        connector = (AmqpConnector) muleContext.getRegistry()
                .lookupConnector("amqpConnectorWithReconnect");
        connector.setConnectionFactory(connectionFactory);
        when(connectionFactory.newConnection(any(ExecutorService.class))).thenThrow(SocketTimeoutException.class).thenReturn(connection);
    }

    @Test
    public void testReconnectSupported() throws Exception
    {
        muleContext.start();
        new PollingProber(DEFAULT_TIMEOUT, POLL_TIMEOUT).check(new JUnitProbe()
        {
            public String describeFailure()
            {
                return "Reconnection after SocketTimeoutException fails";
            }

            @Override
            protected boolean test() throws Exception
            {
                return connector.isConnected();
            }
        });

        assertThat(retries, is(1));
    }

    public static class TestNotifier implements RetryNotifier
    {

        public void onFailure(RetryContext context, Throwable e)
        {
            retries ++;
        }

        public void onSuccess(RetryContext context)
        {

        }
    }

}
