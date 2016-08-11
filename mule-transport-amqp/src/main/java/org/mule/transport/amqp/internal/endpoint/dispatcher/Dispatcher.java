/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.dispatcher;

import static org.mule.transport.amqp.internal.connector.AmqpConnector.ENDPOINT_PROPERTY_QUEUE_AUTO_DELETE;
import static org.mule.transport.amqp.internal.connector.AmqpConnector.ENDPOINT_PROPERTY_QUEUE_DURABLE;
import static org.mule.transport.amqp.internal.connector.AmqpConnector.ENDPOINT_PROPERTY_QUEUE_EXCLUSIVE;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.StartException;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;
import org.mule.transport.amqp.internal.client.AmqpDeclarer;
import org.mule.transport.amqp.internal.client.DispatchingReturnListener;
import org.mule.transport.amqp.internal.confirm.ConfirmsManager;
import org.mule.transport.amqp.internal.confirm.DefaultConfirmsManager;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.mule.transport.amqp.internal.endpoint.AmqpEndpointUtil;
import org.mule.util.StringUtils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ReturnListener;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * The <code>Dispatcher</code> takes care of sending messages from Mule to an AMQP
 * broker. It supports synchronous sending by the means of private temporary reply queues.
 */
public class Dispatcher extends AbstractMessageDispatcher
{
    protected final AmqpConnector amqpConnector;

    private ConfirmsManager confirmsManager;

    private AmqpEndpointUtil endpointUtil;

    private AmqpDeclarer declarator;

    private Channel channel;

    public Dispatcher(final OutboundEndpoint endpoint)
    {
        super(endpoint);
        endpointUtil = new AmqpEndpointUtil();
        amqpConnector = (AmqpConnector) endpoint.getConnector();
        declarator = new AmqpDeclarer();

        if (logger.isDebugEnabled())
        {
            logger.debug("Instantiated: " + this);
        }

        confirmsManager = new DefaultConfirmsManager(amqpConnector);
    }


    @Override
    protected void doInitialise() throws InitialisationException {
        try
        {
            channel = amqpConnector.getChannelHandler().getOrCreateChannel(getEndpoint());
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    protected void doStart() throws MuleException
    {
        try
        {
            boolean activeDeclarationsOnly = amqpConnector.isActiveDeclarationsOnly();
            final String exchangeName = declarator.declareExchange(channel, endpoint, activeDeclarationsOnly);
            final String routingKey = endpointUtil.getRoutingKey(endpoint);
            if (StringUtils.isNotEmpty(endpointUtil.getQueueName(endpoint.getAddress()))
                    || endpoint.getProperties().containsKey(ENDPOINT_PROPERTY_QUEUE_DURABLE)
                    || endpoint.getProperties().containsKey(ENDPOINT_PROPERTY_QUEUE_AUTO_DELETE)
                    || endpoint.getProperties().containsKey(ENDPOINT_PROPERTY_QUEUE_EXCLUSIVE))
            {
                declarator.declareEndpoint(channel, endpoint, activeDeclarationsOnly, exchangeName, routingKey);
            }
        }
        catch (IOException e)
        {
            throw new StartException(MessageFactory.createStaticMessage("Could not start dispatcher."), e, this);
        }
    }

    @Override
    protected void doDispose() {
        try
        {
            if (channel != null)
            {
                amqpConnector.getChannelHandler().closeChannel(channel);
            }
        }
        catch (Exception e)
        {
            // close silently
        }
    }

    @Override
    public void doDispatch(final MuleEvent event) throws Exception
    {
        if (amqpConnector.getConnection() == null)
        {
            throw new IllegalStateException("No AMQP Connection");
        }

        doOutboundAction(event, new DispatcherActionDispatch());
    }

    @Override
    public MuleMessage doSend(final MuleEvent event) throws Exception
    {
        final MuleMessage resultMessage = createMuleMessage(doOutboundAction(event, new DispatcherActionSend()));

        if (resultMessage == null || resultMessage.getPayload() instanceof NullPayload)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Did not get response on endpoint %s after %dms. Will return null response", endpoint.getName(), getTimeOutForEvent(event)));
            }
        }
        else
        {
            resultMessage.applyTransformers(event, amqpConnector.getReceiveTransformer());
        }

        return resultMessage;
    }

    protected AmqpMessage doOutboundAction(final MuleEvent event, final DispatcherAction outboundAction)
            throws Exception
    {
        // Check if the channel got closed for any reason
        if (channel == null || !channel.isOpen())
        {
            logger.debug("Reopening unexpectedly closed channel");
            channel = amqpConnector.getChannelHandler().getOrCreateChannel(getEndpoint());
        }
        
        // If a transaction resource channel is present use it, otherwise use the dispatcher's channel
        Channel eventChannel = amqpConnector.getChannelHandler().getOrDefaultChannel(endpoint, event.getMessage(), channel);

        final MuleMessage message = event.getMessage();

        if (!(message.getPayload() instanceof AmqpMessage))
        {
            throw new DispatchException(
                    MessageFactory.createStaticMessage("Message payload is not an instance of: "
                                                       + AmqpMessage.class.getName()), event, getEndpoint());
        }

        final AmqpMessage amqpMessage = (AmqpMessage) message.getPayload();

        // override publication properties if they are not set
        if ((amqpMessage.getProperties().getDeliveryMode() == null)
            && (amqpConnector.getDeliveryMode() != null))
        {
            amqpMessage.setDeliveryMode(amqpConnector.getDeliveryMode());
        }
        if ((amqpMessage.getProperties().getPriority() == null) && (amqpConnector.getPriority() != null))
        {
            amqpMessage.setPriority(amqpConnector.getPriority().intValue());
        }

        addReturnListenerIfNeeded(event, eventChannel);

        try
        {
            confirmsManager.requestConfirm(eventChannel, event);
        }
        catch (Exception e)
        {
            throw new DispatchException(
                    MessageFactory.createStaticMessage("Broker failed to agree on confirming messages"
                                                       + AmqpMessage.class.getName()), event, getEndpoint(), e);
        }

        final String eventExchange = endpointUtil.getExchangeName(endpoint, event);
        final String eventRoutingKey = endpointUtil.getRoutingKey(endpoint, event);
        final long timeout = getTimeOutForEvent(event);

        AmqpMessage result;

        try
        {
            result = outboundAction.run(amqpConnector, eventChannel, eventExchange,
                                        eventRoutingKey, amqpMessage, timeout);

            if (!confirmsManager.awaitConfirm(eventChannel, event, timeout, TimeUnit.MILLISECONDS))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Broker failed to acknowledge delivery of message after %dms.\n%s", timeout, amqpMessage));
                }

                throw new DispatchException(
                        MessageFactory.createStaticMessage("Broker failed to acknowledge delivery of message"), event, getEndpoint());
            }

            if (logger.isDebugEnabled())
            {
                logger.debug(String.format(
                        "Successfully performed %s(channel: %s, exchange: %s, routing key: %s) for: %s and received: %s",
                        outboundAction, eventChannel, eventExchange, eventRoutingKey, event, result));
            }
        }
        finally
        {
            confirmsManager.forget(event);
            if (logger.isDebugEnabled())
            {
                logger.debug("Disconnecting: " + eventChannel);
            }
        }

        return result;
    }

    private int getTimeOutForEvent(final MuleEvent muleEvent)
    {
        final int defaultTimeOut = muleEvent.getMuleContext().getConfiguration().getDefaultResponseTimeout();
        final int eventTimeOut = muleEvent.getTimeout();

        // allow event time out to override endpoint response time
        if (eventTimeOut != defaultTimeOut)
        {
            return eventTimeOut;
        }
        return getEndpoint().getResponseTimeout();
    }

    /**
     * Try to associate a return listener to the channel in order to allow flow-level exception
     * strategy to handle return messages.
     */
    protected void addReturnListenerIfNeeded(final MuleEvent event, final Channel channel)
    {
        final ReturnListener returnListener = event.getMessage().getInvocationProperty(
                AmqpConnector.RETURN_LISTENER);

        if (returnListener == null)
        {
            // no return listener defined in the flow that encompasses the event
            return;
        }

        if (returnListener instanceof DispatchingReturnListener)
        {
            ((DispatchingReturnListener) returnListener).setAmqpConnector(amqpConnector);
        }

        channel.addReturnListener(returnListener);

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Set return listener: %s on channel: %s", returnListener, channel));
        }
    }

}
