/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector.connection;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.ConnectException;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.connector.ChannelHandler;

import java.util.concurrent.atomic.AtomicReference;

abstract class AmqpConnection
{
    private final Log logger = LogFactory.getLog(getClass());
    private final AmqpConnector amqpConnector;
    private final AtomicReference<Channel> channelRef = new AtomicReference<Channel>();

    AmqpConnection(final AmqpConnector amqpConnector)
    {
        this.amqpConnector = amqpConnector;
    }

    private Channel newChannel()
    {
        try
        {
            final Channel channel = ChannelHandler.createChannel(amqpConnector);

            channel.addShutdownListener(new ShutdownListener()
            {
                public void shutdownCompleted(final ShutdownSignalException sse)
                {
                    if (sse.isInitiatedByApplication())
                    {
                        return;
                    }

                    // do not inform the connector of the issue as it can't
                    // decide what to do reset the channel so it would later
                    // be lazily reconnected
                    channelRef.set(null);
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Terminated dead channel: " + channel, sse);
                    }
                }
            });

            if (logger.isDebugEnabled())
            {
                logger.debug("Shutdown listener configured on channel: " + channel);
            }

            return channel;
        }
        catch (final Exception e)
        {
            if ((!amqpConnector.isStopping()) && (amqpConnector.isStarted()))
            {
                amqpConnector.getMuleContext()
                    .getExceptionListener()
                    .handleException(
                        new ConnectException(
                            MessageFactory.createStaticMessage("Impossible to create new channels on connection: "
                                    + amqpConnector.getConnection()), e,
                            amqpConnector));
            }
            return null;
        }
    }

    public AmqpConnector getAmqpConnector()
    {
        return amqpConnector;
    }

    public Channel getChannel()
    {
        Channel channel = channelRef.get();

        if (channel != null)
        {
            return channel;
        }

        channel = newChannel();

        if (channelRef.compareAndSet(null, channel))
        {
            return channel;
        }

        // race condition: use the channel created by another thread
        return getChannel();
    }

    @Override
    public String toString()
    {
        return super.toString() + ", Channel: " + getChannel();
    }
}
