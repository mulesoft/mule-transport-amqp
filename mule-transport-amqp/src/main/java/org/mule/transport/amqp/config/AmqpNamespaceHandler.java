/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.assembly.configuration.PrefixValueMap;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.amqp.internal.client.DispatchingReturnListener;
import org.mule.transport.amqp.internal.config.NonExclusiveAddressedEndpointDefinitionParser;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.processor.*;
import org.mule.transport.amqp.internal.transaction.AmqpTransactionFactory;
import org.mule.transport.amqp.internal.transformer.AmqpMessageToObject;
import org.mule.transport.amqp.internal.transformer.ObjectToAmqpMessage;

/**
 * Registers a Bean Definition Parser for handling <code><amqp:connector></code> elements and
 * supporting endpoint elements.
 */
public class AmqpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    private static final String[][] AMQP_ENDPOINT_ATTRIBUTES = new String[][]{new String[]{"queueName"},
        new String[]{"exchangeName"}, new String[]{"exchangeName", "queueName"}};

    public void init()
    {
        registerAmqpTransportEndpoints();

        registerConnectorDefinitionParser(getConnectorClass());

        registerBeanDefinitionParser("amqpmessage-to-object-transformer",
            new MessageProcessorDefinitionParser(AmqpMessageToObject.class));
        registerBeanDefinitionParser("object-to-amqpmessage-transformer",
            new MessageProcessorDefinitionParser(ObjectToAmqpMessage.class));

        registerBeanDefinitionParser("acknowledge-message", new MessageProcessorDefinitionParser(
            Acknowledger.class));

        registerBeanDefinitionParser("reject-message", new MessageProcessorDefinitionParser(
            Rejecter.class));

        registerBeanDefinitionParser("recover", new MessageProcessorDefinitionParser(Recover.class));

        registerBeanDefinitionParser("return-handler", new MessageProcessorDefinitionParser(
            ReturnHandler.class));

        registerBeanDefinitionParser("dispatching-return-listener", new ChildDefinitionParser(
            "returnListener", DispatchingReturnListener.class));

        registerBeanDefinitionParser("transaction", new TransactionDefinitionParser(
            AmqpTransactionFactory.class));
    }

    protected Class<? extends AmqpConnector> getConnectorClass()
    {
        return AmqpConnector.class;
    }

    protected String getConnectorProtocol()
    {
        return AmqpConnector.AMQP;
    }

    protected void registerAmqpTransportEndpoints()
    {
        registerAmqpEndpointDefinitionParser("endpoint", new NonExclusiveAddressedEndpointDefinitionParser(
            getConnectorProtocol(), TransportGlobalEndpointDefinitionParser.PROTOCOL,
            new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class),
            TransportGlobalEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES,
            URIBuilder.ALL_ATTRIBUTES, AMQP_ENDPOINT_ATTRIBUTES, new String[][]{}));

        registerAmqpEndpointDefinitionParser("inbound-endpoint",
            new NonExclusiveAddressedEndpointDefinitionParser(getConnectorProtocol(),
                TransportEndpointDefinitionParser.PROTOCOL, new ChildEndpointDefinitionParser(
                    InboundEndpointFactoryBean.class),
                TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, URIBuilder.ALL_ATTRIBUTES,
                AMQP_ENDPOINT_ATTRIBUTES, new String[][]{}));

        registerAmqpEndpointDefinitionParser("outbound-endpoint",
            new NonExclusiveAddressedEndpointDefinitionParser(getConnectorProtocol(),
                TransportEndpointDefinitionParser.PROTOCOL, new ChildEndpointDefinitionParser(
                    OutboundEndpointFactoryBean.class),
                TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, URIBuilder.ALL_ATTRIBUTES,
                AMQP_ENDPOINT_ATTRIBUTES, new String[][]{}));
    }

    protected void registerAmqpEndpointDefinitionParser(final String element,
                                                        final MuleDefinitionParser parser)
    {
        parser.addAlias("exchangeName", URIBuilder.HOST);
        parser.addAlias("queueName", URIBuilder.PATH);
        parser.addMapping("queueName", new PrefixValueMap(AmqpConnector.ENDPOINT_QUEUE_PREFIX));
        registerBeanDefinitionParser(element, parser);
    }

}
