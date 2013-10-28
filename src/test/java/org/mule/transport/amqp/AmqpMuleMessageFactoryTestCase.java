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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Date;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Envelope;

public class AmqpMuleMessageFactoryTestCase extends AbstractMuleContextTestCase
{
    public static AmqpMessage getTestMessage()
    {
        return getTestMessage("messageId");
    }

    public static AmqpMessage getTestMessage(final String messageId)
    {
        final byte[] body = "payload".getBytes();

        final String consumerTag = "consumerTag";

        final Envelope envelope = new Envelope(123456L, true, "exchange", "routingKey");

        final AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
        bob.appId("appId")
            .contentEncoding("utf-16")
            .contentType("application/vnd+mule.xml")
            .correlationId("cid-951753")
            .deliveryMode(2)
            .expiration("expiration")
            .messageId(messageId)
            .priority(5)
            .replyTo("replyTo")
            .timestamp(new Date(100000L))
            .type("type")
            .userId("userId");

        bob.headers(Collections.<String, Object> singletonMap("customKey", "customValue"));

        final BasicProperties amqpProperties = bob.build();
        return new AmqpMessage(consumerTag, envelope, amqpProperties, body);
    }

    @Test
    public void testCreate() throws Exception
    {
        final AmqpMessage testMessage = getTestMessage();

        final AmqpMuleMessageFactory amqpMuleMessageFactory = new AmqpMuleMessageFactory(muleContext);
        final MuleMessage muleMessage = amqpMuleMessageFactory.create(testMessage, "utf-8");

        assertEquals(testMessage, muleMessage.getPayload());
        assertFalse(StringUtils.isEmpty(muleMessage.getUniqueId()));

        checkInboundProperties(testMessage, muleMessage);
    }

    @Test
    public void testMessageIdWhenNullAmqpProperties() throws Exception
    {
        final AmqpMessage testMessage = getTestMessage(null);

        assertNull(testMessage.getProperties().getMessageId());

        final AmqpMuleMessageFactory amqpMuleMessageFactory = new AmqpMuleMessageFactory(muleContext);
        final MuleMessage muleMessage = amqpMuleMessageFactory.create(testMessage, "utf-8");

        assertFalse(StringUtils.isEmpty(muleMessage.getUniqueId()));
    }

    public static void checkInboundProperties(final AmqpMessage amqpMessage, final MuleMessage muleMessage)
    {
        assertEquals(amqpMessage.getConsumerTag(),
            muleMessage.getProperty(AmqpConstants.CONSUMER_TAG, PropertyScope.INBOUND));

        final Envelope envelope = amqpMessage.getEnvelope();
        assertEquals(envelope.getDeliveryTag(),
            muleMessage.getProperty(AmqpConstants.DELIVERY_TAG, PropertyScope.INBOUND));
        assertEquals(envelope.isRedeliver(),
            muleMessage.getProperty(AmqpConstants.REDELIVER, PropertyScope.INBOUND));
        assertEquals(envelope.getExchange(),
            muleMessage.getProperty(AmqpConstants.EXCHANGE, PropertyScope.INBOUND));
        assertEquals(envelope.getRoutingKey(),
            muleMessage.getProperty(AmqpConstants.ROUTING_KEY, PropertyScope.INBOUND));

        final BasicProperties amqpProperties = amqpMessage.getProperties();
        assertEquals(amqpProperties.getAppId(),
            muleMessage.getProperty(AmqpConstants.APP_ID, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getContentEncoding(),
            muleMessage.getProperty(AmqpConstants.CONTENT_ENCODING, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getContentType(),
            muleMessage.getProperty(AmqpConstants.CONTENT_TYPE, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getCorrelationId(),
            muleMessage.getProperty(AmqpConstants.CORRELATION_ID, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getCorrelationId(), muleMessage.getCorrelationId());
        assertEquals(amqpProperties.getDeliveryMode(),
            muleMessage.getProperty(AmqpConstants.DELIVERY_MODE, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getExpiration(),
            muleMessage.getProperty(AmqpConstants.EXPIRATION, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getMessageId(),
            muleMessage.getProperty(AmqpConstants.MESSAGE_ID, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getMessageId(), muleMessage.getUniqueId());
        assertEquals(amqpProperties.getPriority(),
            muleMessage.getProperty(AmqpConstants.PRIORITY, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getReplyTo(),
            muleMessage.getProperty(AmqpConstants.REPLY_TO, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getReplyTo(), muleMessage.getReplyTo());
        assertEquals(amqpProperties.getTimestamp(),
            muleMessage.getProperty(AmqpConstants.TIMESTAMP, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getType(),
            muleMessage.getProperty(AmqpConstants.TYPE, PropertyScope.INBOUND));
        assertEquals(amqpProperties.getUserId(),
            muleMessage.getProperty(AmqpConstants.USER_ID, PropertyScope.INBOUND));

        for (final Entry<String, Object> header : amqpProperties.getHeaders().entrySet())
        {
            assertEquals(header.getValue(), muleMessage.getProperty(header.getKey(), PropertyScope.INBOUND));
        }
    }

}
