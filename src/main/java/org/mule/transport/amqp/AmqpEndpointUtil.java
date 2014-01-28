/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.util.StringUtils;

import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;

public abstract class AmqpEndpointUtil
{
    private static final Log LOG = LogFactory.getLog(AmqpEndpointUtil.class);

    public static final String QUEUE_EXCLUSIVE = "queueExclusive";
    public static final String QUEUE_AUTO_DELETE = "queueAutoDelete";
    public static final String QUEUE_DURABLE = "queueDurable";

    public static final String EXCHANGE_AUTO_DELETE = "exchangeAutoDelete";
    public static final String EXCHANGE_DURABLE = "exchangeDurable";
    public static final String EXCHANGE_TYPE = "exchangeType";

    public static final String ROUTING_KEY = "routingKey";
    public static final String CONSUMER_TAG = "consumerTag";

    public static final String QUEUE_PREFIX = "amqp-queue.";
    public static final String EXCHANGE_PREFIX = "amqp-exchange.";

    public static String getOrCreateQueue(final Channel channel,
                                          final ImmutableEndpoint endpoint,
                                          final boolean activeDeclarationsOnly) throws IOException
    {
        final String exchangeName = getOrCreateExchange(channel, endpoint, activeDeclarationsOnly);
        return getOrCreateQueue(channel, endpoint, activeDeclarationsOnly, exchangeName);
    }

    public static String getOrCreateQueue(final Channel channel,
                                          final ImmutableEndpoint endpoint,
                                          final boolean activeDeclarationsOnly,
                                          final String exchangeName) throws IOException
    {
        final String routingKey = getRoutingKey(endpoint);
        return getOrCreateQueue(channel, endpoint, activeDeclarationsOnly, exchangeName, routingKey);
    }

    public static String getOrCreateQueue(final Channel channel,
                                          final ImmutableEndpoint endpoint,
                                          final boolean activeDeclarationsOnly,
                                          final String exchangeName,
                                          final String routingKey) throws IOException
    {
        final String queueName = getQueueName(endpoint.getAddress());

        if (StringUtils.isBlank(queueName))
        {
            // no queue name -> create a private one on the server
            final DeclareOk queueDeclareResult = channel.queueDeclare();

            final String privateQueueName = queueDeclareResult.getQueue();
            LOG.info("Declared private queue: " + privateQueueName);

            bindQueue(channel, endpoint, exchangeName, routingKey, privateQueueName);
            return privateQueueName;
        }

        // queue name -> either create or ensure the queue exists
        if (endpoint.getProperties().containsKey(QUEUE_DURABLE)
            || endpoint.getProperties().containsKey(QUEUE_AUTO_DELETE)
            || endpoint.getProperties().containsKey(QUEUE_EXCLUSIVE))
        {
            // any of the queue declaration parameter provided -> declare the queue
            final boolean queueDurable = BooleanUtils.toBoolean((String) endpoint.getProperty(QUEUE_DURABLE));
            final boolean queueExclusive = BooleanUtils.toBoolean((String) endpoint.getProperty(QUEUE_EXCLUSIVE));
            final boolean queueAutoDelete = BooleanUtils.toBoolean((String) endpoint.getProperty(QUEUE_AUTO_DELETE));

            final Map<String, Object> arguments = getArguments(endpoint, QUEUE_PREFIX);

            channel.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, arguments);
            LOG.info("Declared queue: " + queueName + ", durable: " + queueDurable + ", exclusive: "
                     + queueExclusive + ", autoDelete: " + queueAutoDelete + ", arguments: " + arguments);

            bindQueue(channel, endpoint, exchangeName, routingKey, queueName);
        }
        else if (!activeDeclarationsOnly)
        {
            // no declaration parameter -> ensure the queue exists
            channel.queueDeclarePassive(queueName);

            if (LOG.isDebugEnabled())
            {
                LOG.debug("Validated presence of queue: " + queueName);
            }
        }

        return queueName;
    }

    private static void bindQueue(final Channel channel,
                                  final ImmutableEndpoint endpoint,
                                  final String exchangeName,
                                  final String routingKey,
                                  final String queueName) throws IOException
    {
        if (isDefaultExchange(exchangeName))
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Skipped binding of queue: " + queueName + " to default exchange");
            }
            return;
        }

				// bind queue to exchange
				String[] routingKeyArray = routingKey.split(",");
				for (int i = 0; i < routingKeyArray.length; i++) {
					channel.queueBind(queueName, exchangeName, routingKeyArray[i].trim());
				}

        LOG.info("Bound queue: " + queueName + " to exchange: " + exchangeName + " with routing key: "
                 + routingKey);
    }

    public static boolean isDefaultExchange(final String exchangeName)
    {
        return StringUtils.isBlank(exchangeName)
               || StringUtils.equals(exchangeName, AmqpConstants.DEFAULT_EXCHANGE_ALIAS);
    }

    public static String getOrCreateExchange(final Channel channel,
                                             final ImmutableEndpoint endpoint,
                                             final boolean activeDeclarationsOnly) throws IOException
    {
        final String exchangeName = getExchangeName(endpoint);

        if (isDefaultExchange(exchangeName))
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Using default exchange for endpoint: " + endpoint);
            }

            return AmqpConstants.DEFAULT_EXCHANGE_ALIAS;
        }

        final String exchangeType = (String) endpoint.getProperty(EXCHANGE_TYPE);
        if (StringUtils.isNotBlank(exchangeType))
        {
            // an exchange type is provided -> the exchange must be declared
            final boolean exchangeDurable = BooleanUtils.toBoolean((String) endpoint.getProperty(EXCHANGE_DURABLE));
            final boolean exchangeAutoDelete = BooleanUtils.toBoolean((String) endpoint.getProperty(EXCHANGE_AUTO_DELETE));

            final Map<String, Object> arguments = getArguments(endpoint, EXCHANGE_PREFIX);

            channel.exchangeDeclare(exchangeName, exchangeType, exchangeDurable, exchangeAutoDelete,
                arguments);

            LOG.info("Declared exchange: " + exchangeName + " of type: " + exchangeType + ", durable: "
                     + exchangeDurable + ", autoDelete: " + exchangeAutoDelete + ", arguments: " + arguments);
        }
        else if (!activeDeclarationsOnly)
        {
            // no exchange type -> ensure the exchange exists
            channel.exchangeDeclarePassive(exchangeName);

            if (LOG.isDebugEnabled())
            {
                LOG.debug("Validated presence of exchange: " + exchangeName);
            }
        }

        return exchangeName;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getArguments(final ImmutableEndpoint endpoint,
                                                    final String argumentPrefix)
    {
        final Map<String, Object> arguments = new HashMap<String, Object>();

        for (final Entry<Object, Object> property : ((Map<Object, Object>) endpoint.getProperties()).entrySet())
        {
            final String name = property.getKey().toString();
            if (StringUtils.startsWith(name, argumentPrefix))
            {
                arguments.put(StringUtils.substringAfter(name, argumentPrefix), property.getValue());
            }
        }

        return arguments;
    }

    public static String getRoutingKey(final ImmutableEndpoint endpoint)
    {
        return StringUtils.defaultString((String) endpoint.getProperty(ROUTING_KEY));
    }

    public static String getRoutingKey(final ImmutableEndpoint endpoint, final MuleEvent muleEvent)
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

    private static String getDynamicRoutingKey(final ImmutableEndpoint endpoint, final MuleEvent muleEvent)
    {
        final String eventRoutingKey = muleEvent.getMessage().getOutboundProperty(AmqpConstants.ROUTING_KEY,
            StringUtils.defaultString((String) endpoint.getProperty(ROUTING_KEY)));

        // MEL in exchange and queue is auto-resolved as being part of the endpoint URI but routing
        // key must be resolved by hand
        final ExpressionManager expressionManager = muleEvent.getMuleContext().getExpressionManager();
        return expressionManager.parse(eventRoutingKey, muleEvent);
    }

    public static String getConsumerTag(final ImmutableEndpoint endpoint)
    {
        return StringUtils.defaultString((String) endpoint.getProperty(CONSUMER_TAG));
    }

    public static String getQueueName(final String endpointAddress)
    {
        return StringUtils.defaultString(StringUtils.substringAfter(trimQuery(endpointAddress), QUEUE_PREFIX));
    }

    public static String getExchangeName(final ImmutableEndpoint endpoint, final MuleEvent muleEvent)
    {
        final String exchangeName = muleEvent.getMessage().getOutboundProperty(AmqpConstants.EXCHANGE,
            getExchangeName(endpoint));

        return AmqpEndpointUtil.isDefaultExchange(exchangeName) ? StringUtils.EMPTY : exchangeName;
    }

    public static String getExchangeName(final ImmutableEndpoint endpoint)
    {
        return getExchangeName(endpoint.getAddress(), endpoint.getConnector().getProtocol());
    }

    public static String getExchangeName(final String endpointAddress, final String protocol)
    {
        final String trimmedQuery = trimQuery(endpointAddress);
        final String exchangeName = StringUtils.defaultString(
            StringUtils.substringBetween(trimmedQuery, protocol + "://", "/" + QUEUE_PREFIX),
            StringUtils.substringAfter(trimmedQuery, protocol + "://"));

        return exchangeName.startsWith(QUEUE_PREFIX) ? StringUtils.EMPTY : exchangeName;
    }

    private static String trimQuery(final String address)
    {
        return StringUtils.substringBefore(address, "?");
    }
}
