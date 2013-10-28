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
 * Used to manually perform a basic recover of the current channel.
 */
public class AmqpRecover extends AbstractChannelMessageProcessor
{
    private static final Log LOG = LogFactory.getLog(AmqpRecover.class);
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

    public static void recover(final MuleEvent event, final boolean requeue) throws MuleException
    {
        recover(event.getMessage(), requeue);
    }

    public static void recover(final MuleMessage message, final boolean requeue) throws MuleException
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

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Manually recovered channel: " + channel);
        }
    }
}
