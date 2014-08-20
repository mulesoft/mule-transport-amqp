/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.transaction;

import java.io.Closeable;
import java.io.IOException;

import org.apache.commons.lang.Validate;

import com.rabbitmq.client.Channel;

public class CloseableChannelWrapper implements Closeable
{
    private final Channel channel;

    public CloseableChannelWrapper(final Channel channel)
    {
        Validate.notNull(channel, "channel can't be null");
        this.channel = channel;
    }

    public Channel getChannel()
    {
        return channel;
    }

    public void close() throws IOException
    {
        channel.close();
    }
}
