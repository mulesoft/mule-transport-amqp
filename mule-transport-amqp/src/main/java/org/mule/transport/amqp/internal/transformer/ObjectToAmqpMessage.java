/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.transformer;

import java.util.*;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AmqpMessage;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Envelope;
import org.mule.util.ArrayUtils;

public class ObjectToAmqpMessage extends AbstractAmqpMessageToObject
{
    private static final String[] AMQP_ENVELOPE_PROPERTY_NAMES_ARRAY = new String[]{AmqpConnector.DELIVERY_TAG,
            AmqpConnector.EXCHANGE, AmqpConnector.REDELIVER, AmqpConnector.ROUTING_KEY};

    private static final String[] AMQP_BASIC_PROPERTY_NAMES_ARRAY = new String[]{AmqpConnector.APP_ID,
            AmqpConnector.CONTENT_ENCODING, AmqpConnector.CONTENT_TYPE, AmqpConnector.CORRELATION_ID,
            AmqpConnector.DELIVERY_MODE, AmqpConnector.EXPIRATION, AmqpConnector.MESSAGE_ID, AmqpConnector.PRIORITY,
            AmqpConnector.REPLY_TO, AmqpConnector.TIMESTAMP, AmqpConnector.TYPE, AmqpConnector.USER_ID};

    private static final String[] AMQP_TRANSPORT_TECHNICAL_PROPERTY_NAMES_ARRAY = new String[]{
            AmqpConnector.ALL_USER_HEADERS, AmqpConnector.CONSUMER_TAG, AmqpConnector.CHANNEL,
            AmqpConnector.AMQP_DELIVERY_TAG, AmqpConnector.RETURN_LISTENER, AmqpConnector.RETURN_REPLY_CODE,
            AmqpConnector.RETURN_REPLY_TEXT, AmqpConnector.RETURN_EXCHANGE, AmqpConnector.RETURN_ROUTING_KEY};

    public static final Set<String> AMQP_ALL_PROPERTY_NAMES = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList((String[]) ArrayUtils.addAll(
                    ArrayUtils.addAll(AMQP_ENVELOPE_PROPERTY_NAMES_ARRAY, AMQP_BASIC_PROPERTY_NAMES_ARRAY),
                    AMQP_TRANSPORT_TECHNICAL_PROPERTY_NAMES_ARRAY))));

    @Override
    protected void declareInputOutputClasses()
    {
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(AMQP_MESSAGE_DATA_TYPE);
    }

    @Override
    public Object transformMessage(final MuleMessage message, final String outputEncoding)
        throws TransformerException
    {
        byte[] body;
        try
        {
            body = message.getPayloadAsBytes();
        }
        catch (final Exception e)
        {
            throw new TransformerException(
                MessageFactory.createStaticMessage("Impossible to extract bytes out of: " + message), e);
        }

        final String consumerTag = getProperty(message, AmqpConnector.CONSUMER_TAG);

        final long deliveryTag = getProperty(message, AmqpConnector.DELIVERY_TAG, 0L);
        final boolean redelivered = getProperty(message, AmqpConnector.REDELIVER, false);
        final String exchange = getProperty(message, AmqpConnector.EXCHANGE);
        final String routingKey = getProperty(message, AmqpConnector.ROUTING_KEY);
        final Envelope envelope = new Envelope(deliveryTag, redelivered, exchange, routingKey);

        final AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
        bob.appId(this.<String> getProperty(message, AmqpConnector.APP_ID))
            .contentEncoding(
                this.<String> getProperty(message, AmqpConnector.CONTENT_ENCODING, outputEncoding))
            .contentType(this.<String> getProperty(message, AmqpConnector.CONTENT_TYPE))
            .correlationId(
                this.<String> getProperty(message, AmqpConnector.CORRELATION_ID, message.getCorrelationId()))
            .deliveryMode(this.<Integer> getProperty(message, AmqpConnector.DELIVERY_MODE))
            .expiration(this.<String> getProperty(message, AmqpConnector.EXPIRATION))
            .messageId(this.<String> getProperty(message, AmqpConnector.MESSAGE_ID, message.getUniqueId()))
            .priority(this.<Integer> getProperty(message, AmqpConnector.PRIORITY))
            .replyTo(
                this.<String> getProperty(message, AmqpConnector.REPLY_TO, (String) message.getReplyTo()))
            .timestamp(this.<Date> getProperty(message, AmqpConnector.TIMESTAMP, new Date()))
            .type(this.<String> getProperty(message, AmqpConnector.TYPE))
            .userId(this.<String> getProperty(message, AmqpConnector.USER_ID));

        bob.headers(getHeaders(message));

        final BasicProperties amqpProperties = bob.build();
        return new AmqpMessage(consumerTag, envelope, amqpProperties, body);
    }

    private Map<String, Object> getHeaders(final MuleMessage message)
    {
        final Map<String, Object> headers = new HashMap<String, Object>();
        for (final String propertyName : message.getPropertyNames(PropertyScope.OUTBOUND))
        {
            if (!AMQP_ALL_PROPERTY_NAMES.contains(propertyName))
            {
                headers.put(propertyName, message.getProperty(propertyName, PropertyScope.OUTBOUND));
            }
        }
        return headers;
    }

    private <T> T getProperty(final MuleMessage message, final String key)
    {
        return this.<T> getProperty(message, key, null);
    }

    private <T> T getProperty(final MuleMessage message, final String key, final T defaultValue)
    {
        return message.getProperty(key, PropertyScope.OUTBOUND, defaultValue);
    }
}
