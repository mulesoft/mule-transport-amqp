/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.client;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.transport.amqp.internal.connector.AmqpConnector.ENDPOINT_PROPERTY_QUEUE_DURABLE;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.amqp.internal.endpoint.AmqpEndpointUtil;

import com.rabbitmq.client.Channel;

import org.junit.Before;
import org.junit.Test;

public class AMQPDeclarationsTestCase
{
    private AmqpEndpointUtil amqpEndpointUtil = mock(AmqpEndpointUtil.class);
    private final TestAmqpDeclarer amqpDeclarer =  new TestAmqpDeclarer();
    private final Channel channel = mock(Channel.class, RETURNS_DEEP_STUBS);
    private final ImmutableEndpoint endpoint = mock(ImmutableEndpoint.class, RETURNS_DEEP_STUBS);

    @Before
    public void setUp() throws Exception
    {
        when(amqpEndpointUtil.getEndpointType(endpoint)).thenReturn("testType");
        when(amqpEndpointUtil.getQueueName(anyString())).thenReturn("testQueueName");
        when(endpoint.getProperty(anyString())).thenReturn("");
        when(endpoint.getProperties().containsKey(ENDPOINT_PROPERTY_QUEUE_DURABLE)).thenReturn(true);
    }

    @Test
    public void declareExchangePassively() throws Exception
    {
        amqpDeclarer.declareExchange(channel, endpoint, false);
        verify(channel).exchangeDeclarePassive(anyString());
        verify(channel, never()).exchangeDeclare(anyString(), anyString(), anyBoolean(), anyBoolean(), anyMap());
    }

    @Test
    public void declareExchangeActively() throws Exception
    {
        amqpDeclarer.declareExchange(channel, endpoint, true);
        verify(channel, never()).exchangeDeclarePassive(anyString());
        verify(channel).exchangeDeclare(anyString(), anyString(), anyBoolean(), anyBoolean(), anyMap());
    }

    @Test
    public void declareQueuePassively() throws Exception
    {
        amqpDeclarer.declareEndpoint(channel, endpoint, false, "testExchangeName", "testRoutingKey");
        verify(channel).queueDeclarePassive(anyString());
        verify(channel, never()).queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), anyMap());
    }

    @Test
    public void declareQueueActively() throws Exception
    {
        amqpDeclarer.declareEndpoint(channel, endpoint, true, "testExchangeName", "testRoutingKey");
        verify(channel, never()).queueDeclarePassive(anyString());
        verify(channel).queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), anyMap());
    }

    private class TestAmqpDeclarer extends AmqpDeclarer
    {

        @Override
        protected AmqpEndpointUtil createAmqpEndpointUtil()
        {
            return amqpEndpointUtil;
        }
    }
}