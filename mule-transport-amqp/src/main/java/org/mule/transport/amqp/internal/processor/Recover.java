/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.processor;

import static org.mule.transport.amqp.internal.processor.ChannelUtils.getChannelOrFail;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to manually perform a basic recover of the current channel.
 */
public class Recover implements MessageProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Recover.class);

    private static final String CHANNEL_ACTION = "recover";

    protected boolean requeue = false;

    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        recover(event, requeue);
        return event;
    }

    public void setRequeue(final boolean requeue)
    {
        this.requeue = requeue;
    }

    public void recover(final MuleEvent event, final boolean requeue) throws MuleException
    {
        recover(event.getMessage(), requeue);
    }

    public void recover(final MuleMessage message, final boolean requeue) throws MuleException
    {
        final Channel channel = getChannelOrFail(message, CHANNEL_ACTION);

        try
        {
            channel.basicRecover(requeue);
        }
        catch (final Exception e)
        {
            throw new DefaultMuleException("Failed to recover channel: " + channel, e);
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Manually recovered channel: " + channel);
        }
    }
}
