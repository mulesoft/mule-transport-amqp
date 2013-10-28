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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

import com.rabbitmq.client.Channel;

/**
 * Used to manually perform a basic ack of the message in flow, allowing fine control of message
 * throttling. It looks for a delivery-tag inbound message property and an amqp.channel session
 * property. If the former is missing, it logs a warning. If the former is present but not the
 * latter, it throws an exception.
 */
public class AmqpMessageAcknowledger extends AbstractChannelMessageProcessor
{
    private static final Log LOG = LogFactory.getLog(AmqpMessageAcknowledger.class);
    private static final String CHANNEL_ACTION = "ack";

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

    public static void ack(final MuleEvent event, final boolean multiple) throws MuleException
    {
        ack(event.getMessage(), multiple);
    }

    public static void ack(final MuleMessage message, final boolean multiple) throws MuleException
    {
        final Long deliveryTag = getDeliveryTagOrFail(message, CHANNEL_ACTION);
        final Channel channel = getChannelOrFail(message, CHANNEL_ACTION);

        try
        {
            channel.basicAck(deliveryTag, multiple);
        }
        catch (final Exception e)
        {
            throw new DefaultMuleException("Failed to ack message w/deliveryTag: " + deliveryTag
                                           + " on channel: " + channel, e);
        }

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Manually acknowledged message w/deliveryTag: " + deliveryTag + " on channel: "
                      + channel);
        }
    }
}
