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

import java.util.Arrays;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Envelope;

public class AmqpMessage
{
    private final String consumerTag;
    private final Envelope envelope;
    private final byte[] body;
    private AMQP.BasicProperties properties;

    public AmqpMessage(final String consumerTag,
                       final Envelope envelope,
                       final BasicProperties properties,
                       final byte[] body)
    {
        this.consumerTag = consumerTag;
        this.envelope = envelope;
        this.properties = properties;
        this.body = body;
    }

    public String getConsumerTag()
    {
        return consumerTag;
    }

    public Envelope getEnvelope()
    {
        return envelope;
    }

    public AMQP.BasicProperties getProperties()
    {
        return properties;
    }

    public void setDeliveryMode(final DeliveryMode deliveryMode)
    {
        this.properties = properties.builder().deliveryMode(deliveryMode.getCode()).build();
    }

    public void setPriority(final int priority)
    {
        this.properties = properties.builder().priority(priority).build();
    }

    public void setReplyTo(final String replyTo)
    {
        this.properties = properties.builder().replyTo(replyTo).build();
    }

    public byte[] getBody()
    {
        return body;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(body);
        result = prime * result + ((consumerTag == null) ? 0 : consumerTag.hashCode());
        result = prime * result + ((envelope == null) ? 0 : HashCodeBuilder.reflectionHashCode(envelope));
        result = prime * result + ((properties == null) ? 0 : HashCodeBuilder.reflectionHashCode(properties));
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final AmqpMessage other = (AmqpMessage) obj;
        if (!Arrays.equals(body, other.body)) return false;
        if (consumerTag == null)
        {
            if (other.consumerTag != null) return false;
        }
        else if (!consumerTag.equals(other.consumerTag)) return false;
        if (envelope == null)
        {
            if (other.envelope != null) return false;
        }
        else if (!EqualsBuilder.reflectionEquals(envelope, other.envelope)) return false;
        if (properties == null)
        {
            if (other.properties != null) return false;
        }
        else if (!EqualsBuilder.reflectionEquals(properties, other.properties)) return false;
        return true;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
