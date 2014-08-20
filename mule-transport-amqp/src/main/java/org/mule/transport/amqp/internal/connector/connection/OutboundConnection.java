/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector.connection;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.amqp.internal.client.UrlEndpointURIParser;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.util.StringUtils;

public class OutboundConnection extends AmqpConnection
{
    private final String exchange;

    private final String routingKey;

    private UrlEndpointURIParser uriParser = new UrlEndpointURIParser();

    public OutboundConnection(final AmqpConnector amqpConnector,
                       final String exchange,
                       final String routingKey)
    {
        super(amqpConnector);
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public String getExchange()
    {
        return uriParser.isDefaultExchange(exchange) ? StringUtils.EMPTY : exchange;
    }

    public String getRoutingKey()
    {
        return routingKey;
    }

    public boolean canDispatch(final MuleEvent muleEvent, final OutboundEndpoint outboundEndpoint)
    {
        final String eventExchange = uriParser.getExchangeName(outboundEndpoint, muleEvent);
        final String eventRoutingKey = uriParser.getRoutingKey(outboundEndpoint, muleEvent);
        return StringUtils.equals(getExchange(), eventExchange)
               && StringUtils.equals(getRoutingKey(), eventRoutingKey);
    }
}
