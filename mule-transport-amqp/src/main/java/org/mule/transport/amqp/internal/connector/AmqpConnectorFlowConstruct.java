/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector;

import org.mule.construct.AbstractFlowConstruct;

/**
 * A fake {@link org.mule.api.construct.FlowConstruct} that is used when the events need to be dispatched on behalf of
 * the connector and out of any actual Flow context.
 */
public class AmqpConnectorFlowConstruct extends AbstractFlowConstruct
{
    private final AmqpConnector connector;

    AmqpConnectorFlowConstruct(final AmqpConnector connector)
    {
        super(connector.getName(), connector.getMuleContext());
        this.connector = connector;
    }

    @Override
    public String getConstructType()
    {
        return "Global AMQP Connector Fake Flow";
    }

    public AmqpConnector getConnector()
    {
        return connector;
    }
}
