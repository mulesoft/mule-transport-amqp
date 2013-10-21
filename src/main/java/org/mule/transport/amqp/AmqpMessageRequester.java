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

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.ConnectException;
import org.mule.transport.amqp.AmqpConnector.InboundConnection;

import com.rabbitmq.client.Channel;

/**
 * The <code>AmqpMessageRequester</code> is used to consume individual messages from an AMQP broker.
 */
public class AmqpMessageRequester extends AbstractMessageRequester
{
    protected final AmqpConnector amqpConnector;
    protected InboundConnection inboundConnection;

    public AmqpMessageRequester(final InboundEndpoint endpoint)
    {
        super(endpoint);
        amqpConnector = (AmqpConnector) endpoint.getConnector();
    }

    @Override
    public void doConnect() throws ConnectException
    {
        inboundConnection = amqpConnector.connect(this);
    }

    @Override
    public void doDisconnect() throws MuleException
    {
        final Channel channel = getChannel();

        if (logger.isDebugEnabled())
        {
            logger.debug("Disconnecting: queue: " + getQueueName() + " from channel: " + channel);
        }

        inboundConnection = null;
        amqpConnector.closeChannel(channel);
    }

    @Override
    protected MuleMessage doRequest(final long timeout) throws Exception
    {
        final Channel channel = getChannel();

        final AmqpMessage amqpMessage = amqpConnector.consumeMessage(channel, getQueueName(),
            amqpConnector.getAckMode().isAutoAck(), timeout);

        if (amqpMessage == null) return null;

        final MuleMessage muleMessage = createMuleMessage(amqpMessage);

        amqpConnector.addInvocationPropertiesIfNecessary(channel, amqpMessage, muleMessage);
        amqpConnector.ackMessageIfNecessary(channel, amqpMessage, endpoint);

        return muleMessage;
    }

    protected Channel getChannel()
    {
        return inboundConnection == null ? null : inboundConnection.getChannel();
    }

    protected String getQueueName()
    {
        return inboundConnection == null ? null : inboundConnection.getQueue();
    }
}
