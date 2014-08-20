/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.endpoint.dispatcher;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.MessageFactory;
import org.mule.processor.DelegateTransaction;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.ConnectException;
import org.mule.transport.NullPayload;
import org.mule.transport.amqp.internal.client.UrlEndpointURIParser;
import org.mule.transport.amqp.internal.confirm.ConfirmsManager;
import org.mule.transport.amqp.internal.confirm.DefaultConfirmsManager;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.connector.ChannelHandler;
import org.mule.transport.amqp.internal.connector.connection.OutboundConnection;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ReturnListener;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.mule.transport.amqp.internal.client.DispatchingReturnListener;
import org.mule.transport.amqp.internal.transaction.CloseableChannelWrapper;
import org.mule.transport.amqp.internal.transaction.AmqpTransaction;

import java.util.concurrent.TimeUnit;

/**
 * The <code>Dispatcher</code> takes care of sending messages from Mule to an AMQP
 * broker. It supports synchronous sending by the means of private temporary reply queues.
 */
public class Dispatcher extends AbstractMessageDispatcher
{
    protected final AmqpConnector amqpConnector;

    protected volatile OutboundConnection outboundConnection;

    private ConfirmsManager confirmsManager;

    private UrlEndpointURIParser uriParser;

    public Dispatcher(final OutboundEndpoint endpoint)
    {
        super(endpoint);

        uriParser = new UrlEndpointURIParser();

        amqpConnector = (AmqpConnector) endpoint.getConnector();

        if (logger.isDebugEnabled())
        {
            logger.debug("Instantiated: " + this);
        }

        confirmsManager = new DefaultConfirmsManager(amqpConnector);
    }

    /**
     * Connecting an outbound AMQP endpoint does more than just connecting a channel: it can also
     * potentially declare an exchange, a queue and bind the latter to the former. Since exchange
     * and queue name can be dynamic, we can only perform the connection when an event is in flight.
     */
    protected void internalDoConnect(final MuleEvent event) throws ConnectException
    {
        outboundConnection = amqpConnector.connect(this, event);
    }

    @Override
    protected void doDisconnect() throws MuleException
    {
        if (outboundConnection != null)
        {
            final Channel channel = outboundConnection.getChannel();

            if (logger.isDebugEnabled())
            {
                logger.debug("Disconnecting: " + outboundConnection);
            }

            outboundConnection = null;
            ChannelHandler.closeChannel(channel);
        }
    }

    @Override
    public void doDispatch(final MuleEvent event) throws Exception
    {
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
        ensureOutboundConnectionCanHandleEvent(event);

        final MuleMessage message = event.getMessage();

        if (!(message.getPayload() instanceof AmqpMessage))
        {
            throw new DispatchException(
                    MessageFactory.createStaticMessage("Message payload is not an instance of: "
                                                       + AmqpMessage.class.getName()), event, getEndpoint());
        }

        final Channel eventChannel = getEventChannel();

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

        final String eventExchange = uriParser.getExchangeName(endpoint, event);
        final String eventRoutingKey = uriParser.getRoutingKey(endpoint, event);
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
        }
        finally
        {
            confirmsManager.forget(event);
        }


        if (logger.isDebugEnabled())
        {
            logger.debug(String.format(
                    "Successfully performed %s(channel: %s, exchange: %s, routing key: %s) for: %s and received: %s",
                    outboundAction, eventChannel, eventExchange, eventRoutingKey, event, result));
        }

        return result;
    }

    private void ensureOutboundConnectionCanHandleEvent(final MuleEvent event) throws MuleException
    {
        // no need to protect this for thread safety because only one thread at a time can
        // traverse a endpoint instance
        if (outboundConnection == null)
        {
            internalDoConnect(event);
        }
        else
        {
            if (!outboundConnection.canDispatch(event, getEndpoint()))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Outbound connection: "
                                 + outboundConnection
                                 + " can't handle current event. Refreshing it before performing outbound action.");
                }

                doDisconnect();
                internalDoConnect(event);
            }
        }
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

    protected Channel getEventChannel() throws Exception
    {
        if (endpoint.getTransactionConfig().isConfigured())
        {
            final byte action = endpoint.getTransactionConfig().getAction();

            final boolean mayUseChannelFromTransaction = action == TransactionConfig.ACTION_BEGIN_OR_JOIN
                                                         || action == TransactionConfig.ACTION_JOIN_IF_POSSIBLE
                                                         || action == TransactionConfig.ACTION_INDIFFERENT;

            final boolean mustUseChannelFromTransaction = action == TransactionConfig.ACTION_ALWAYS_JOIN;

            final Transaction transaction = TransactionCoordination.getInstance().getTransaction();
            if (transaction instanceof AmqpTransaction)
            {
                if (mustUseChannelFromTransaction || mayUseChannelFromTransaction)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Using transacted channel from current transaction: " + transaction);
                    }

                    return ((AmqpTransaction) transaction).getTransactedChannel();
                }
            }
            else if (transaction instanceof DelegateTransaction)
            {
                // we can't use the current endpoint channel because it may get closed (if the
                // endpoint instance is destroyed) while the transaction block is not done with...
                final Channel channel = ChannelHandler.createChannel(amqpConnector);
                channel.txSelect();
                // we wrap the channel so the transaction will know it can safely close it an
                // commit/rollback
                transaction.bindResource(channel.getConnection(), new CloseableChannelWrapper(
                        channel));

                if (logger.isDebugEnabled())
                {
                    logger.debug("Created transacted channel for delegate transaction: " + transaction);
                }

                return channel;
            }
            else
            {
                if (mustUseChannelFromTransaction)
                {
                    throw new IllegalTransactionStateException(
                            MessageFactory.createStaticMessage("No active AMQP transaction found for endpoint: "
                                                               + endpoint));
                }
            }
        }

        return outboundConnection.getChannel();
    }
}
