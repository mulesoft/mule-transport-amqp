
package org.mule.transport.amqp;

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
