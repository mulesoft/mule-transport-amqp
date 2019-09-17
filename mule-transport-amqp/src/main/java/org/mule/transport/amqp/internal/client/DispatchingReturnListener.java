/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import org.apache.commons.lang.Validate;
import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.connector.AmqpConnectorFlowConstruct;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class DispatchingReturnListener extends AbstractAmqpReturnHandlerListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DispatchingReturnListener.class);

    protected final FlowConstruct eventFlowConstruct;
    protected final List<MessageProcessor> returnMessageProcessors;

    protected volatile AmqpConnector amqpConnector;

    public DispatchingReturnListener(final List<MessageProcessor> returnMessageProcessors,
                                     final MuleEvent event)
    {
        this(event.getFlowConstruct(), returnMessageProcessors);
    }

    public DispatchingReturnListener(final List<MessageProcessor> returnMessageProcessors,
                                     final AmqpConnectorFlowConstruct flowConstruct)
    {
        this(flowConstruct, returnMessageProcessors);
        this.amqpConnector = flowConstruct.getConnector();
    }

    DispatchingReturnListener(final FlowConstruct eventFlowConstruct,
                              final List<MessageProcessor> returnMessageProcessors)
    {
        Validate.notNull(eventFlowConstruct, "eventFlowConstruct can't be null");
        this.eventFlowConstruct = eventFlowConstruct;
        this.returnMessageProcessors = returnMessageProcessors;
    }

    public void setAmqpConnector(final AmqpConnector amqpConnector)
    {
        this.amqpConnector = amqpConnector;
    }

    @Override
    protected void doHandleReturn(final String errorMessage,
                                  final Map<String, Object> returnContext,
                                  final AmqpMessage returnedAmqpMessage)
    {
        try
        {
            // thread safe copy of the message
            final MuleMessage returnedMuleMessage = amqpConnector.getMuleMessageFactory().create(
                returnedAmqpMessage,
                amqpConnector.getMuleContext().getConfiguration().getDefaultEncoding());

            returnedMuleMessage.addProperties(returnContext, PropertyScope.INBOUND);

            for (final MessageProcessor returnMessageProcessor : returnMessageProcessors)
            {
                final DefaultMuleEvent returnedMuleEvent = new DefaultMuleEvent(returnedMuleMessage,
                    MessageExchangePattern.ONE_WAY, eventFlowConstruct);

                returnedMuleMessage.applyTransformers(returnedMuleEvent,
                    amqpConnector.getReceiveTransformer());

                returnMessageProcessor.process(returnedMuleEvent);
            }
        }
        catch (final Exception e)
        {
            LOGGER.error(String.format(
                "%s, impossible to dispatch the following message to the configured endpoint(s): %s",
                errorMessage, returnedAmqpMessage), e);
        }
    }
}
