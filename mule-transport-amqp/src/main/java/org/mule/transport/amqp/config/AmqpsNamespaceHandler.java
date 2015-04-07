/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.config;

import org.mule.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.connector.AmqpsConnector;

/**
 * Registers a Bean Definition Parser for handling <code><amqps:connector></code> elements and
 * supporting endpoint elements.
 */
public class AmqpsNamespaceHandler extends AmqpNamespaceHandler
{
    @Override
    public void init()
    {
        super.init();

        registerBeanDefinitionParser("ssl-key-store", new KeyStoreDefinitionParser());
        registerBeanDefinitionParser("ssl-trust-store", new TrustStoreDefinitionParser());
    }

    @Override
    protected Class<? extends AmqpConnector> getConnectorClass()
    {
        return AmqpsConnector.class;
    }

    @Override
    protected String getConnectorProtocol()
    {
        return AmqpsConnector.AMQPS;
    }
}
