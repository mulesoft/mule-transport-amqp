/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector;

import com.rabbitmq.client.Channel;
import org.mule.transport.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ChannelHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelHandler.class);

    public static Channel createChannel(AmqpConnector connector) throws IOException
    {
        final Channel channel = connector.getConnection().createChannel();
        channel.addReturnListener(connector.getDefaultReturnListener());
        channel.basicQos(connector.getPrefetchSize(), connector.getPrefetchCount(), false);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Created and configured new channel: " + channel);
        }

        return channel;
    }

    public static void closeChannel(final Channel channel) throws ConnectException
    {
        if (channel == null)
        {
            return;
        }

        if (!channel.isOpen())
        {
            return;
        }

        try
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Closing channel: " + channel);
            }

            channel.close();

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Closed channel: " + channel);
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("Failed to close channel: " + channel, e);
        }
    }
}
