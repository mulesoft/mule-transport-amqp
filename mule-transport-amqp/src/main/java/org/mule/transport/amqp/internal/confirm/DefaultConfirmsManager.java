/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.confirm;

import org.mule.api.MuleEvent;
import org.mule.transport.amqp.internal.connector.AmqpConnector;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DefaultConfirmsManager implements ConfirmsManager
{
    private final Map<Long, ConfirmHandler> pendingConfirms = new ConcurrentHashMap<Long, ConfirmHandler>();
    private final AmqpConnector connector;

    public DefaultConfirmsManager(AmqpConnector connector)
    {
        this.connector = connector;
    }

    public void requestConfirm(Channel channel, MuleEvent event) throws Exception
    {
        if (!handlesConfirms())
        {
            return;
        }

        channel.addConfirmListener(new ConfirmListener()
        {
            public void handleAck(long deliveryTag, boolean multiple) throws IOException
            {
                confirm(deliveryTag, true);
            }

            public void handleNack(long deliveryTag, boolean multiple) throws IOException
            {
                confirm(deliveryTag, false);
            }
        });

        channel.confirmSelect();
        long nextSequence = channel.getNextPublishSeqNo();
        pendingConfirms.put(nextSequence, new ConfirmHandler());
        event.setFlowVariable(AmqpConnector.MESSAGE_PROPERTY_NEXT_PUBLISH_SEQ_NO, nextSequence);

    }

    public boolean awaitConfirm(Channel channel, MuleEvent event, long timeout, TimeUnit timeUnit)
    {
        try
        {
            if (!handlesConfirms())
            {
                return true;
            }

            Long seqNo = event.getFlowVariable(AmqpConnector.MESSAGE_PROPERTY_NEXT_PUBLISH_SEQ_NO);
            if (seqNo == null)
            {
                throw new IllegalStateException("Event is missing publish sequence number");
            }

            ConfirmHandler confirmHandler = pendingConfirms.get(seqNo);
            if (confirmHandler != null)
            {
                return confirmHandler.awaitConfirmation(timeout, timeUnit);
            }
            else
            {
                return false;
            }
        }
        finally
        {
            channel.clearConfirmListeners();
        }
    }

    public void forget(MuleEvent event)
    {
        Long seqNo = event.getFlowVariable(AmqpConnector.MESSAGE_PROPERTY_NEXT_PUBLISH_SEQ_NO);
        if (seqNo != null)
        {
            pendingConfirms.remove(seqNo);
        }
    }

    private void confirm(long deliveryTag, boolean success)
    {
        ConfirmHandler handler = pendingConfirms.get(deliveryTag);
        if (handler != null)
        {
            handler.confirm(success);
        }
    }

    private boolean handlesConfirms()
    {
        return connector.isRequestBrokerConfirms();
    }

}
