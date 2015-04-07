/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.requester;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.ConnectException;
import org.mule.transport.amqp.internal.client.MessageConsumer;
import org.mule.transport.amqp.internal.client.MessagePropertiesHandler;
import org.mule.transport.amqp.internal.client.ChannelHandler;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.mule.transport.amqp.internal.connector.AmqpConnector;

import com.rabbitmq.client.Channel;
import org.mule.transport.amqp.internal.endpoint.AmqpEndpointUtil;

/**
 * The <code>MessageRequester</code> is used to consume individual messages from an AMQP broker.
 */
public class MessageRequester extends AbstractMessageRequester
{
    protected final AmqpConnector amqpConnector;

    protected MessageConsumer messageConsumer = new MessageConsumer();

    protected MessagePropertiesHandler messagePropertiesHandler = new MessagePropertiesHandler();

    private AmqpEndpointUtil endpointUtil;

    private InboundEndpoint endpoint;

    private Channel channel;

    public MessageRequester(final InboundEndpoint endpoint)
    {
        super(endpoint);
        this.endpoint = endpoint;
        amqpConnector = (AmqpConnector) endpoint.getConnector();
        endpointUtil = new AmqpEndpointUtil();
    }

    @Override
    public void doConnect() throws ConnectException
    {
        try
        {
            channel = amqpConnector.getChannelHandler().getOrCreateChannel(endpoint);
        }
        catch (Exception e)
        {
            throw new ConnectException(e, this);
        }
    }

    @Override
    public void doDisconnect() throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Disconnecting: queue: " + getQueueName() + " from channel: " + channel);
        }

        amqpConnector.getChannelHandler().closeChannel(channel);
    }

    @Override
    protected MuleMessage doRequest(final long timeout) throws Exception
    {
        final AmqpMessage amqpMessage = messageConsumer.consumeMessage(channel, getQueueName(),
                amqpConnector.getAckMode().isAutoAck(), timeout);

        if (amqpMessage == null) return null;

        final MuleMessage muleMessage = createMuleMessage(amqpMessage);

        messagePropertiesHandler.addInvocationProperties(channel, amqpMessage, muleMessage, amqpConnector);
        messagePropertiesHandler.ackMessageIfNecessary(channel, amqpMessage, endpoint);

        return muleMessage;
    }

    protected String getQueueName()
    {
        return endpointUtil.getQueueName(getEndpoint().getAddress());
    }
}
