/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.client;


import static java.util.Arrays.asList;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.runners.Parameterized.Parameters;
import static org.mule.api.transaction.TransactionConfig.ACTION_INDIFFERENT;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.transaction.AmqpTransaction;

import com.rabbitmq.client.Channel;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ChannelHandlerTestCase extends AbstractMuleTestCase
{

    private static final Channel transactionChannel = mock(Channel.class);
    private static final Channel newChannel = mock(Channel.class);

    private final ChannelHandler channelHandler = new ChannelHandler();
    private final ImmutableEndpoint endpoint = mock(ImmutableEndpoint.class, RETURNS_DEEP_STUBS);
    private final AmqpTransaction transaction = mock(AmqpTransaction.class);
    private final AmqpConnector connector = mock(AmqpConnector.class, RETURNS_DEEP_STUBS);

    private final Channel channelToAssert;
    private final boolean isTransactionalEndpoint;

    public ChannelHandlerTestCase(Channel channelToAssert, boolean isTransactionalEndpoint)
    {
        this.channelToAssert = channelToAssert;
        this.isTransactionalEndpoint = isTransactionalEndpoint;
    }

    @Parameters
    public static Collection<Object[]> data()
    {
        return asList(new Object[][] {
                {transactionChannel, true},
                {newChannel, false}
        });
    }

    @Before
    public void setUp() throws Exception
    {
        mockTransactionChannelScenario();
        mockNewChannelScenario();
    }


    @Test
    public void getOrCreateChannel() throws Exception
    {
        Channel channel = channelHandler.getOrCreateChannel(endpoint);
        assertThat(channel, sameInstance(channelToAssert));
    }

    @Test
    public void getOrDefaultChannel() throws Exception
    {
        Channel channel = channelHandler.getOrDefaultChannel(endpoint, newChannel);
        assertThat(channel, sameInstance(channelToAssert));
    }

    private void mockTransactionChannelScenario() throws Exception
    {
        TransactionCoordination.getInstance().bindTransaction(transaction);
        when(endpoint.getTransactionConfig().isConfigured()).thenReturn(isTransactionalEndpoint);
        when(endpoint.getTransactionConfig().getAction()).thenReturn(ACTION_INDIFFERENT);
        when(transaction.getTransactedChannel()).thenReturn(transactionChannel);
    }

    private void mockNewChannelScenario() throws Exception
    {
        when(endpoint.getConnector()).thenReturn(connector);
        when(connector.getConnection().createChannel()).thenReturn(newChannel);
    }

}
