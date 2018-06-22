/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.processor;

import static org.mule.transport.amqp.internal.processor.ChannelUtils.ACK_CHANNEL_ACTION;
import static org.mule.transport.amqp.internal.processor.ChannelUtils.getChannelOrFail;
import static org.mule.transport.amqp.internal.processor.ChannelUtils.getDeliveryTagOrFail;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to manually perform a basic ack of the message in flow, allowing fine control of message
 * throttling. It looks for a delivery-tag inbound message property and an amqp.channel session
 * property. If the former is missing, it logs a warning. If the former is present but not the
 * latter, it throws an exception.
 */
public class Acknowledger implements MessageProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Acknowledger.class);

    protected boolean multiple = false;

    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        ack(event, multiple);
        return event;
    }

    public void setMultiple(final boolean multiple)
    {
        this.multiple = multiple;
    }

    public void ack(final MuleEvent event, final boolean multiple) throws MuleException
    {
        ack(event.getMessage(), multiple);
    }

    public void ack(final MuleMessage message, final boolean multiple) throws MuleException
    {
        final Long deliveryTag = getDeliveryTagOrFail(message, ACK_CHANNEL_ACTION);
        final Channel channel = getChannelOrFail(message, ACK_CHANNEL_ACTION);

        try
        {
            channel.basicAck(deliveryTag, multiple);
        }
        catch (final Exception e)
        {
            throw new DefaultMuleException("Failed to ack message w/deliveryTag: " + deliveryTag
                                           + " on channel: " + channel, e);
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Manually acknowledged message w/deliveryTag: " + deliveryTag + " on channel: "
                    + channel);
        }
    }
}
