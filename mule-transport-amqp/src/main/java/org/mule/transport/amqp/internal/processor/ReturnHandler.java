/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.processor;

import java.util.List;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.transport.amqp.internal.client.DispatchingReturnListener;
import org.mule.transport.amqp.internal.client.LoggingReturnListener;
import org.mule.transport.amqp.internal.connector.AmqpConnector;

import com.rabbitmq.client.ReturnListener;

/**
 * Message processor that sets the return listener for the flow, leaving it up to the
 * endpoint to set it on the channel.<br/>
 * This class is also the holder of all the different return listeners of the
 * transport.
 */
public class ReturnHandler extends AbstractInterceptingMessageProcessor
{
    public static final ReturnListener DEFAULT_RETURN_LISTENER = new LoggingReturnListener();

    private List<MessageProcessor> returnMessageProcessors;

    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        final DispatchingReturnListener returnListener = new DispatchingReturnListener(
            returnMessageProcessors, event);
        event.getMessage().setInvocationProperty(AmqpConnector.RETURN_LISTENER, returnListener);
        return processNext(event);
    }

    public void setMessageProcessors(final List<MessageProcessor> returnMessageProcessors)
    {
        this.returnMessageProcessors = returnMessageProcessors;
    }
}
