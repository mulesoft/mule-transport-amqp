/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.domain;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.LongString;

public class AmqpMuleMessageFactory extends AbstractMuleMessageFactory
{
    public static final Charset LONG_STRING_CHARSET = Charset.forName("UTF-8");

    public AmqpMuleMessageFactory(final MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[]{AmqpMessage.class};
    }

    @Override
    protected Object extractPayload(final Object transportMessage, final String encoding) throws Exception
    {
        return transportMessage;
    }

    @Override
    protected void addProperties(final DefaultMuleMessage muleMessage, final Object transportMessage)
        throws Exception
    {
        final AmqpMessage amqpMessage = (AmqpMessage) transportMessage;

        final Map<String, Object> inboundProperties = new HashMap<String, Object>();
        putIfNonNull(AmqpConnector.CONSUMER_TAG, amqpMessage.getConsumerTag(), inboundProperties);
        addEnvelopeProperties(amqpMessage.getEnvelope(), inboundProperties);
        addBasicProperties(amqpMessage.getProperties(), muleMessage, inboundProperties);

        // add user defined headers both as regular inbound headers and as a single map (for easy
        // processing)
        final Map<String, Object> userHeaders = new HashMap<String, Object>();
        if (amqpMessage.getProperties().getHeaders() != null)
        {
            addHeaders(amqpMessage.getProperties().getHeaders(), inboundProperties);
            addNonMuleHeaders(amqpMessage.getProperties().getHeaders(), userHeaders);
        }
        inboundProperties.put(AmqpConnector.ALL_USER_HEADERS, userHeaders);

        muleMessage.addInboundProperties(inboundProperties);
    }

    private void addEnvelopeProperties(final Envelope envelope, final Map<String, Object> messageProperties)
    {
        if (envelope == null) return;

        putIfNonNull(AmqpConnector.DELIVERY_TAG, envelope.getDeliveryTag(), messageProperties);
        putIfNonNull(AmqpConnector.REDELIVER, envelope.isRedeliver(), messageProperties);
        putIfNonNull(AmqpConnector.EXCHANGE, envelope.getExchange(), messageProperties);
        putIfNonNull(AmqpConnector.ROUTING_KEY, envelope.getRoutingKey(), messageProperties);
    }

    private void addBasicProperties(final BasicProperties amqpProperties,
                                    final DefaultMuleMessage muleMessage,
                                    final Map<String, Object> messageProperties)
    {
        if (amqpProperties == null) return;

        putIfNonNull(AmqpConnector.APP_ID, amqpProperties.getAppId(), messageProperties);
        putIfNonNull(AmqpConnector.CONTENT_ENCODING, amqpProperties.getContentEncoding(), messageProperties);
        putIfNonNull(AmqpConnector.CONTENT_TYPE, amqpProperties.getContentType(), messageProperties);

        final String correlationId = amqpProperties.getCorrelationId();
        putIfNonNull(AmqpConnector.CORRELATION_ID, correlationId, messageProperties);
        putIfNonNull(MuleProperties.MULE_CORRELATION_ID_PROPERTY, correlationId, messageProperties);
        muleMessage.setCorrelationId(correlationId);

        putIfNonNull(AmqpConnector.DELIVERY_MODE, amqpProperties.getDeliveryMode(), messageProperties);
        putIfNonNull(AmqpConnector.EXPIRATION, amqpProperties.getExpiration(), messageProperties);

        final String messageId = amqpProperties.getMessageId();
        putIfNonNull(AmqpConnector.MESSAGE_ID, messageId, messageProperties);
        putIfNonNull(MuleProperties.MULE_MESSAGE_ID_PROPERTY, messageId, messageProperties);
        muleMessage.setUniqueId(messageId == null ? UUID.getUUID() : messageId);

        final String clusterId = amqpProperties.getClusterId();
        putIfNonNull(AmqpConnector.CLUSTER_ID, clusterId, messageProperties);

        putIfNonNull(AmqpConnector.PRIORITY, amqpProperties.getPriority(), messageProperties);

        final String replyTo = amqpProperties.getReplyTo();
        putIfNonNull(AmqpConnector.REPLY_TO, replyTo, messageProperties);
        muleMessage.setReplyTo(replyTo);

        putIfNonNull(AmqpConnector.TIMESTAMP, amqpProperties.getTimestamp(), messageProperties);
        putIfNonNull(AmqpConnector.TYPE, amqpProperties.getType(), messageProperties);
        putIfNonNull(AmqpConnector.USER_ID, amqpProperties.getUserId(), messageProperties);
    }

    private void addHeaders(final Map<String, Object> headers, final Map<String, Object> messageProperties)
    {
        for (final Entry<String, Object> header : headers.entrySet())
        {
            putIfNonNull(header.getKey(), header.getValue(), messageProperties);
        }
    }

    private void addNonMuleHeaders(final Map<String, Object> headers,
                                   final Map<String, Object> messageProperties)
    {
        for (final Entry<String, Object> header : headers.entrySet())
        {
            if (!StringUtils.startsWith(header.getKey(), MuleProperties.PROPERTY_PREFIX))
            {
                putIfNonNull(header.getKey(), header.getValue(), messageProperties);
            }
        }
    }

    private void putIfNonNull(final String key,
                              final Object value,
                              final Map<String, Object> messageProperties)
    {
        if (value == null)
        {
            return;
        }

        if (value instanceof LongString)
        {
            final String stringValue = new String(((LongString) value).getBytes(), LONG_STRING_CHARSET);
            messageProperties.put(key, stringValue);
        }
        else
        {
            messageProperties.put(key, value);
        }
    }
}
