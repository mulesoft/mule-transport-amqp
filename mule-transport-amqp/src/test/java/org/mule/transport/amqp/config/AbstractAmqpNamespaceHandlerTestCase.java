/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.mule.api.construct.Pipeline;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.processor.Acknowledger;
import org.mule.transport.amqp.internal.transaction.AmqpTransaction.RecoverStrategy;
import org.mule.transport.amqp.internal.transaction.AmqpTransactionFactory;
import org.mule.transport.amqp.internal.transformer.AmqpMessageToObject;
import org.mule.transport.amqp.internal.transformer.ObjectToAmqpMessage;

public abstract class AbstractAmqpNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected AbstractAmqpNamespaceHandlerTestCase()
    {
        super();
        setStartContext(false);
        setDisposeContextPerClass(true);
    }

    protected abstract String getProtocol();

    @Test
    public void testFullGlobalEndpoint() throws Exception
    {
        final EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(
            getProtocol() + "FullGlobalEndpoint");
        assertNotNull(endpointBuilder);

        final InboundEndpoint inboundEndpoint = endpointBuilder.buildInboundEndpoint();
        assertEquals(getProtocol() + "://target-exchange/amqp-queue.target-queue", inboundEndpoint.getAddress());
        assertEquals(getProtocol() + "://target-exchange/amqp-queue.target-queue",
            inboundEndpoint.getEndpointURI().getAddress());
        assertEquals("a.b.c", inboundEndpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_ROUTING_KEY));
        assertEquals("true", inboundEndpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_EXCHANGE_DURABLE));
        assertFalse(inboundEndpoint.getTransactionConfig().isTransacted());

        final OutboundEndpoint outboundEndpoint = endpointBuilder.buildOutboundEndpoint();
        assertEquals(getProtocol() + "://target-exchange/amqp-queue.target-queue", outboundEndpoint.getAddress());
        assertEquals(getProtocol() + "://target-exchange/amqp-queue.target-queue",
            outboundEndpoint.getEndpointURI().getAddress());
        assertEquals("a.b.c", outboundEndpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_ROUTING_KEY));
        assertEquals("true", outboundEndpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_EXCHANGE_DURABLE));
        assertFalse(outboundEndpoint.getTransactionConfig().isTransacted());
    }

    @Test
    public void testExistingQueueGlobalEndpoint() throws Exception
    {
        final EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(
            getProtocol() + "ExistingQueueGlobalEndpoint");
        assertNotNull(endpointBuilder);

        final InboundEndpoint inboundEndpoint = endpointBuilder.buildInboundEndpoint();
        assertEquals(getProtocol() + "://amqp-queue.target-queue", inboundEndpoint.getAddress());
        assertEquals(getProtocol() + "://amqp-queue.target-queue", inboundEndpoint.getEndpointURI()
            .getAddress());
        assertFalse(inboundEndpoint.getTransactionConfig().isTransacted());
    }

    @Test
    public void testPrivateQueueGlobalEndpoint() throws Exception
    {
        final EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(
            getProtocol() + "PrivateQueueGlobalEndpoint");
        assertNotNull(endpointBuilder);

        final InboundEndpoint inboundEndpoint = endpointBuilder.buildInboundEndpoint();
        assertEquals(getProtocol() + "://target-exchange", inboundEndpoint.getAddress());
        assertEquals(getProtocol() + "://target-exchange", inboundEndpoint.getEndpointURI().getAddress());
        assertFalse(inboundEndpoint.getTransactionConfig().isTransacted());
    }

    @Test
    public void testExistingExchangeGlobalEndpoint() throws Exception
    {
        final EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(
            getProtocol() + "ExistingExchangeGlobalEndpoint");
        assertNotNull(endpointBuilder);

        final InboundEndpoint inboundEndpoint = endpointBuilder.buildInboundEndpoint();
        assertEquals(getProtocol() + "://target-exchange", inboundEndpoint.getAddress());
        assertEquals(getProtocol() + "://target-exchange", inboundEndpoint.getEndpointURI().getAddress());
        assertFalse(inboundEndpoint.getTransactionConfig().isTransacted());
    }

    @Test
    public void testGlobalTransformers() throws Exception
    {
        assertTrue(muleContext.getRegistry().lookupTransformer("a2o") instanceof AmqpMessageToObject);
        assertTrue(muleContext.getRegistry().lookupTransformer("o2a") instanceof ObjectToAmqpMessage);
    }

    @Test
    public void testAcknowledger() throws Exception
    {
        final List<MessageProcessor> messageProcessors = ((Pipeline) muleContext.getRegistry()
            .lookupFlowConstruct("ackerFlow")).getMessageProcessors();
        assertEquals(1, messageProcessors.size());
        assertTrue(messageProcessors.get(0) instanceof Acknowledger);
    }

    @Test
    public void testTransactedEndpoint() throws Exception
    {
        final EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(
            getProtocol() + "TransactedEndpoint");
        assertNotNull(endpointBuilder);
        final InboundEndpoint inboundEndpoint = endpointBuilder.buildInboundEndpoint();
        assertTrue(inboundEndpoint.getTransactionConfig().isTransacted());
    }

    @Test
    public void testTransactedEndpointWithRecoverStrategy() throws Exception
    {
        final EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(
            getProtocol() + "TransactedEndpointWithRecoverStrategy");
        assertNotNull(endpointBuilder);
        final InboundEndpoint inboundEndpoint = endpointBuilder.buildInboundEndpoint();
        assertTrue(inboundEndpoint.getTransactionConfig().isTransacted());
        assertTrue(((AmqpTransactionFactory) inboundEndpoint.getTransactionConfig().getFactory()).getRecoverStrategy() == RecoverStrategy.REQUEUE);
    }

    @Test
    public void testEndpointWithArguments() throws Exception
    {
        final EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(
            getProtocol() + "EndpointWithArguments");
        assertNotNull(endpointBuilder);
        final InboundEndpoint inboundEndpoint = endpointBuilder.buildInboundEndpoint();
        assertTrue(inboundEndpoint.getProperties().containsKey("amqp-exchange.alternate-exchange"));
        assertTrue(inboundEndpoint.getProperties().containsKey("amqp-queue.x-dead-letter-exchange"));
    }
}
