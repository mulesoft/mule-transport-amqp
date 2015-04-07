/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import com.rabbitmq.client.Channel;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AckMode;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MessagePropertiesHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePropertiesHandler.class);

    public void ackMessageIfNecessary(final Channel channel,
                                      final AmqpMessage amqpMessage,
                                      final ImmutableEndpoint endpoint) throws IOException
    {
        AmqpConnector connector = (AmqpConnector) endpoint.getConnector();

        if ((endpoint.getTransactionConfig().isTransacted()) || (connector.getAckMode() != AckMode.MULE_AUTO))
        {
            return;
        }

        channel.basicAck(amqpMessage.getEnvelope().getDeliveryTag(), false);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Mule acknowledged message: " + amqpMessage + " on channel: " + channel);
        }
    }

    public void addInvocationProperties(final Channel channel,
                                        final AmqpMessage amqpMessage,
                                        final MuleMessage muleMessage,
                                        final AmqpConnector connector)
    {
        // in manual AckMode, the channel will be needed to ack the message
        muleMessage.setProperty(AmqpConnector.MESSAGE_PROPERTY_CHANNEL, channel, PropertyScope.INVOCATION);
        // so will the consumer tag (which is already added in the inbound properties
        // for the end user but that we also add here in the invocation scope for
        // internal needs)
        muleMessage.setProperty(AmqpConnector.AMQP_DELIVERY_TAG, amqpMessage.getEnvelope()
            .getDeliveryTag(), PropertyScope.INVOCATION);
    }
}
