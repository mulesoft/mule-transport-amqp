/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.processor;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;

import com.rabbitmq.client.Channel;
import org.mule.transport.amqp.internal.connector.AmqpConnector;

/**
 * Provides common logic to all channel aware message processors.
 */
public abstract class AbstractChannelMessageProcessor implements MessageProcessor
{
    protected static Long getDeliveryTagOrFail(final MuleMessage muleMessage, final String channelAction)
        throws MuleException
    {
        final Long deliveryTag = getDeliveryTagFromMessage(muleMessage);

        if (deliveryTag == null)
        {
            throw new DefaultMuleException("No " + AmqpConnector.AMQP_DELIVERY_TAG
                                           + " invocation property found, impossible to " + channelAction
                                           + " message: " + muleMessage);
        }

        return deliveryTag;
    }

    protected static Channel getChannelOrFail(final MuleMessage muleMessage, final String channelAction)
        throws MuleException
    {
        final Channel channel = getChannelFromMessage(muleMessage);

        if (channel == null)
        {
            throw new DefaultMuleException("No " + AmqpConnector.CHANNEL
                                           + " invocation property found, impossible to " + channelAction
                                           + " message: " + muleMessage);
        }

        return channel;
    }

    public static Long getDeliveryTagFromMessage(final MuleMessage message)
    {
        return message.getInvocationProperty(AmqpConnector.AMQP_DELIVERY_TAG,
                message.<Long> getInboundProperty(AmqpConnector.DELIVERY_TAG));
    }

    protected static Channel getChannelFromMessage(final MuleMessage message)
    {
        return getChannelFromMessage(message, null);
    }

    protected static Channel getChannelFromMessage(final MuleMessage message, final Channel defaultValue)
    {
        return message.getInvocationProperty(AmqpConnector.CHANNEL, defaultValue);
    }

}
