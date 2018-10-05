/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.processor;

import static org.mule.transport.amqp.internal.connector.AmqpConnector.AMQP_DELIVERY_TAG;
import static org.mule.transport.amqp.internal.connector.AmqpConnector.MESSAGE_PROPERTY_DELIVERY_TAG;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

import com.rabbitmq.client.Channel;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.client.ChannelHandler;
import org.mule.transport.amqp.internal.client.ChannelMessageProperty;

/**
 * Provides common logic to all channel aware components.
 */
public class ChannelUtils
{
    private static ChannelHandler channelHandler = new ChannelHandler();

    public static final String ACK_CHANNEL_ACTION = "ack";

    public static final String NACK_CHANNEL_ACTION = "nack";

    public static Long getDeliveryTagOrFail(final MuleMessage muleMessage, final String channelAction)
        throws MuleException
    {
        final Long deliveryTag = getDeliveryTagFromMessage(muleMessage);

        if (deliveryTag == null)
        {
            throw new DefaultMuleException("No " + AMQP_DELIVERY_TAG
                                           + " invocation property found, impossible to " + channelAction
                                           + " message: " + muleMessage);
        }

        return deliveryTag;
    }

    public static Channel getChannelOrFail(final MuleMessage muleMessage, final String channelAction)
        throws MuleException
    {
        final ChannelMessageProperty channelProperty = getChannelMessageProperty(muleMessage);

        if (channelProperty == null || channelProperty.getChannel() == null)
        {
            throw new DefaultMuleException("No " + AmqpConnector.MESSAGE_PROPERTY_CHANNEL
                                           + " invocation property found, impossible to " + channelAction
                                           + " message: " + muleMessage);
        }

        return channelProperty.getChannel();
    }

    public static ChannelMessageProperty getChannelMessageProperty(final MuleMessage muleMessage)
    {
        return channelHandler.getFlowVariableChannel(muleMessage);
    }

    public static Long getDeliveryTagFromMessage(final MuleMessage message)
    {
        return message.getInvocationProperty(AMQP_DELIVERY_TAG,
                                             message.<Long> getInboundProperty(MESSAGE_PROPERTY_DELIVERY_TAG));
    }

}
