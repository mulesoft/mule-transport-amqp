/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint;

import org.apache.commons.lang.BooleanUtils;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class AmqpEndpointUtil
{
    public String getQueueName(final String endpointAddress)
    {
        return StringUtils.defaultString(StringUtils.substringAfter(trimQuery(endpointAddress), AmqpConnector.ENDPOINT_QUEUE_PREFIX));
    }

    public String getExchangeName(final ImmutableEndpoint endpoint)
    {
        return getExchangeName(endpoint.getAddress(), endpoint.getConnector().getProtocol());
    }

    public String getExchangeName(final ImmutableEndpoint endpoint, final MuleEvent muleEvent)
    {
        final String exchangeName = muleEvent.getMessage().getOutboundProperty(AmqpConnector.EXCHANGE,
                getExchangeName(endpoint));

        return isDefaultExchange(exchangeName) ? StringUtils.EMPTY : exchangeName;
    }

    public String getExchangeName(final String endpointAddress, final String protocol)
    {
        final String trimmedQuery = trimQuery(endpointAddress);
        final String exchangeName = StringUtils.defaultString(
                StringUtils.substringBetween(trimmedQuery, protocol + "://", "/" + AmqpConnector.ENDPOINT_QUEUE_PREFIX),
                StringUtils.substringAfter(trimmedQuery, protocol + "://"));

        return exchangeName.startsWith(AmqpConnector.ENDPOINT_QUEUE_PREFIX) ? StringUtils.EMPTY : exchangeName;
    }

    public boolean isDefaultExchange(final String exchangeName)
    {
        return StringUtils.isBlank(exchangeName)
                || StringUtils.equals(exchangeName, AmqpConnector.ENDPOINT_DEFAULT_EXCHANGE_ALIAS);
    }

    public boolean isExchangeDurable(final ImmutableEndpoint endpoint)
    {
        return BooleanUtils.toBoolean((String) endpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_EXCHANGE_DURABLE));
    }

    public boolean isExchangeAutoDelete(final ImmutableEndpoint endpoint)
    {
        return BooleanUtils.toBoolean((String) endpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_EXCHANGE_AUTO_DELETE));
    }

    public String getEndpointType(ImmutableEndpoint endpoint)
    {
        return (String) endpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_EXCHANGE_TYPE);
    }

    public String getRoutingKey(final ImmutableEndpoint endpoint)
    {
        return StringUtils.defaultString((String) endpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_ROUTING_KEY));
    }

    public String getRoutingKey(final ImmutableEndpoint endpoint, final MuleEvent muleEvent)
    {
        final String exchange = getExchangeName(endpoint);
        String routingKey = getDynamicRoutingKey(endpoint, muleEvent);

        // if dispatching to default exchange and routing key has been omitted use the
        // queueName as routing key
        if ((isDefaultExchange(exchange)) && (StringUtils.isBlank(routingKey)))
        {
            final String queueName = getQueueName(endpoint.getAddress());
            if (StringUtils.isNotBlank(queueName))
            {
                routingKey = queueName;
            }
        }
        return routingKey;
    }

    public boolean isDynamicRoutingKey(final String routingKey, final ImmutableEndpoint endpoint)
    {
        ExpressionManager expressionManager = endpoint.getMuleContext().getExpressionManager();
        return expressionManager.isExpression(routingKey);
    }

    private String getDynamicRoutingKey(final ImmutableEndpoint endpoint, final MuleEvent muleEvent)
    {
        final String eventRoutingKey = muleEvent.getMessage().getOutboundProperty(AmqpConnector.MESSAGE_PROPERTY_ROUTING_KEY,
                StringUtils.defaultString((String) endpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_ROUTING_KEY)));

        // MEL in exchange and queue is auto-resolved as being part of the endpoint URI but routing
        // key must be resolved by hand
        final ExpressionManager expressionManager = muleEvent.getMuleContext().getExpressionManager();
        return expressionManager.parse(eventRoutingKey, muleEvent);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getArguments(final ImmutableEndpoint endpoint,
                                            final String argumentPrefix)
    {
        final Map<String, Object> arguments = new HashMap<String, Object>();

        for (final Map.Entry<Object, Object> property : ((Map<Object, Object>) endpoint.getProperties()).entrySet())
        {
            final String name = property.getKey().toString();
            if (StringUtils.startsWith(name, argumentPrefix))
            {
                arguments.put(StringUtils.substringAfter(name, argumentPrefix), property.getValue());
            }
        }

        return arguments;
    }

    public int getNumberOfChannels(final ImmutableEndpoint endpoint)
    {
        String endpointNumberOfChannels = (String) endpoint.getProperty(AmqpConnector.NUMBER_OF_CHANNELS);
        if (endpointNumberOfChannels != null)
        {
            return Integer.valueOf((String) endpoint.getProperty(AmqpConnector.NUMBER_OF_CHANNELS));
        }
        else
        {
            return ((AmqpConnector)endpoint.getConnector()).getNumberOfChannels();
        }
    }

    private String trimQuery(final String address)
    {
        return StringUtils.substringBefore(address, "?");
    }
}
