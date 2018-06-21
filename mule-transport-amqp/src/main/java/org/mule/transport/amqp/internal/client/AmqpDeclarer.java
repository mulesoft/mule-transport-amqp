/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang.BooleanUtils;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.endpoint.AmqpEndpointUtil;
import org.mule.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static org.mule.transport.amqp.internal.connector.AmqpConnector.ENDPOINT_PROPERTY_QUEUE_AUTO_DELETE;
import static org.mule.transport.amqp.internal.connector.AmqpConnector.ENDPOINT_PROPERTY_QUEUE_DURABLE;
import static org.mule.transport.amqp.internal.connector.AmqpConnector.ENDPOINT_PROPERTY_QUEUE_EXCLUSIVE;
public class AmqpDeclarer
{
    private static final Logger logger = LoggerFactory.getLogger(AmqpDeclarer.class);

    protected AmqpEndpointUtil endpointUtil = new AmqpEndpointUtil();

    public String declareEndpoint(final Channel channel,
                                  final ImmutableEndpoint endpoint,
                                  final boolean activeDeclarationsOnly) throws IOException
    {
        final String exchangeName = declareExchange(channel, endpoint, activeDeclarationsOnly);
        return declareEndpoint(channel, endpoint, activeDeclarationsOnly, exchangeName);
    }

    public String declareEndpoint(final Channel channel,
                                  final ImmutableEndpoint endpoint,
                                  final boolean activeDeclarationsOnly,
                                  final String exchangeName) throws IOException
    {
        final String routingKey = endpointUtil.getRoutingKey(endpoint);
        return declareEndpoint(channel, endpoint, activeDeclarationsOnly, exchangeName, routingKey);
    }

    public String declareEndpoint(final Channel channel,
                                  final ImmutableEndpoint endpoint,
                                  final boolean activeDeclarationsOnly,
                                  final String exchangeName,
                                  final String routingKey) throws IOException
    {
        final String queueName = endpointUtil.getQueueName(endpoint.getAddress());
        final EndpointURI uri = endpoint.getEndpointURI();
        final boolean isDynamicBinding = endpointUtil.isDynamicRoutingKey(routingKey, endpoint);

        // no queue name -> create a private one on the server
        if (StringUtils.isBlank(queueName))
        {
            final String privateQueueName = declareTemporaryQueue(channel);

            if(!isDynamicBinding)
            {
                declareBinding(channel, exchangeName, routingKey, privateQueueName);
            }

            return privateQueueName;
        }

        logDeclaredEndpoint(uri, exchangeName, routingKey, queueName, isDynamicBinding);


        // queue name -> either create or ensure the queue exists
        if (endpoint.getProperties().containsKey(AmqpConnector.ENDPOINT_PROPERTY_QUEUE_DURABLE)
                || endpoint.getProperties().containsKey(AmqpConnector.ENDPOINT_PROPERTY_QUEUE_AUTO_DELETE)
                || endpoint.getProperties().containsKey(AmqpConnector.ENDPOINT_PROPERTY_QUEUE_EXCLUSIVE))
        {
            // any of the queue declaration parameter provided -> declare the queue
            final boolean queueDurable = BooleanUtils.toBoolean((String) endpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_QUEUE_DURABLE));
            final boolean queueExclusive = BooleanUtils.toBoolean((String) endpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_QUEUE_EXCLUSIVE));
            final boolean queueAutoDelete = BooleanUtils.toBoolean((String) endpoint.getProperty(AmqpConnector.ENDPOINT_PROPERTY_QUEUE_AUTO_DELETE));

            final Map<String, Object> arguments = endpointUtil.getArguments(endpoint, AmqpConnector.ENDPOINT_QUEUE_PREFIX);

            declareQueueActively(channel, queueName, queueDurable, queueExclusive, queueAutoDelete, arguments);
            if(!isDynamicBinding)
            {
                declareBinding(channel, exchangeName, routingKey, queueName);
            }
        }
        else if (!activeDeclarationsOnly)
        {
            // no declaration parameter -> ensure the queue exists
            declareQueuePassively(channel, queueName);
        }

        logDeclaredEndpoint(uri, exchangeName, routingKey, queueName, isDynamicBinding);

        return queueName;
    }

    private void declareQueueActively(Channel channel, String queueName,
                                      boolean queueDurable, boolean queueExclusive,
                                      boolean queueAutoDelete, Map<String, Object> arguments) throws IOException
    {
        channel.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, arguments);
        logger.info("Declared queue: " + queueName + ", durable: " + queueDurable + ", exclusive: "
                + queueExclusive + ", autoDelete: " + queueAutoDelete + ", arguments: " + arguments);
    }

    public void declareQueuePassively(Channel channel, String queueName) throws IOException
    {
        channel.queueDeclarePassive(queueName);

        if (logger.isDebugEnabled())
        {
            logger.debug("Validated presence of queue: " + queueName);
        }
    }

    public String declareTemporaryQueue(Channel channel) throws IOException
    {
        final AMQP.Queue.DeclareOk queueDeclareResult =  channel.queueDeclare();

        final String privateQueueName = queueDeclareResult.getQueue();
        logger.debug("Declared private queue: " + privateQueueName);

        return privateQueueName;
    }

    public void declareBinding(final Channel channel,
                                final String exchangeName,
                                final String routingKey,
                                final String queueName) throws IOException
    {
        if (endpointUtil.isDefaultExchange(exchangeName))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Skipped binding of queue: " + queueName + " to default exchange");
            }
            return;
        }

        // bind queue to exchange
        String[] routingKeyArray = routingKey.split(",");
        for (int i = 0; i < routingKeyArray.length; i++) {
            channel.queueBind(queueName, exchangeName, routingKeyArray[i].trim());
        }

        logger.info("Bound queue: " + queueName + " to exchange: " + exchangeName + " with routing key: "
                + routingKey);
    }


    public String declareExchange(final Channel channel,
                                  final ImmutableEndpoint endpoint,
                                  final boolean activeDeclarationsOnly) throws IOException
    {
        final String exchangeName = endpointUtil.getExchangeName(endpoint);

        if (endpointUtil.isDefaultExchange(exchangeName))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Using default exchange for endpoint: " + endpoint);
            }

            return AmqpConnector.ENDPOINT_DEFAULT_EXCHANGE_ALIAS;
        }

        final String exchangeType = endpointUtil.getEndpointType(endpoint);
        if (StringUtils.isNotBlank(exchangeType))
        {
            // an exchange type is provided -> the exchange must be declared
            final boolean exchangeDurable = endpointUtil.isExchangeDurable(endpoint);
            final boolean exchangeAutoDelete = endpointUtil.isExchangeAutoDelete(endpoint);

            final Map<String, Object> arguments = endpointUtil.getArguments(endpoint, AmqpConnector.ENDPOINT_EXCHANGE_PREFIX);

            channel.exchangeDeclare(exchangeName, exchangeType, exchangeDurable, exchangeAutoDelete,
                    arguments);

            logger.info("Declared exchange: " + exchangeName + " of type: " + exchangeType + ", durable: "
                    + exchangeDurable + ", autoDelete: " + exchangeAutoDelete + ", arguments: " + arguments);
        }
        else if (!activeDeclarationsOnly)
        {
            // no exchange type -> ensure the exchange exists
            channel.exchangeDeclarePassive(exchangeName);

            if (logger.isDebugEnabled())
            {
                logger.debug("Validated presence of exchange: " + exchangeName);
            }
        }

        return exchangeName;
    }

    public AmqpEndpointUtil getEndpointUtil()
    {
        return endpointUtil;
    }

    private void logDeclaredEndpoint (EndpointURI uri, String exchangeName, String routingKey, String queueName, boolean isDynamicBinding)
    {
        if (logger.isDebugEnabled())
        {
            if(isDynamicBinding)
            {
                logger.debug("Declaring endpoint with URI: " + uri + " with exchange: " + exchangeName + " queueName: " + queueName
                    +". As routing key has a dynamic value, initial binding is ignored.");
            }
            else
            {
                logger.debug("Declaring endpoint with URI: " + uri + " with exchange: " + exchangeName + " rountingKey: " +
                             routingKey + " queueName: " + queueName);
            }
        }
    }

    public void declareExchangeAndEndpointIfNecessary(final Channel channel,
                                                      final ImmutableEndpoint endpoint,
                                                      final boolean activeDeclarationsOnly) throws IOException
    {
        final String exchangeName = declareExchange(channel, endpoint, activeDeclarationsOnly);
        if (StringUtils.isNotEmpty(endpointUtil.getQueueName(endpoint.getAddress()))
                || endpoint.getProperties().containsKey(ENDPOINT_PROPERTY_QUEUE_DURABLE)
                || endpoint.getProperties().containsKey(ENDPOINT_PROPERTY_QUEUE_AUTO_DELETE)
                || endpoint.getProperties().containsKey(ENDPOINT_PROPERTY_QUEUE_EXCLUSIVE))
        {
            declareEndpoint(channel, endpoint, activeDeclarationsOnly, exchangeName);
        }
    }

}
