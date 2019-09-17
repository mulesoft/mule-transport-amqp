/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.MessageFactory;
import org.mule.processor.DelegateTransaction;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.ConnectException;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.transaction.AmqpTransaction;
import org.mule.transport.amqp.internal.transaction.CloseableChannelWrapper;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelHandler.class);

    public Channel getOrCreateChannel(ImmutableEndpoint endpoint) throws Exception
    {
        Channel ret;

        ret = getChannel(endpoint);
        if (ret != null)
        {
            return ret;
        }

        // if no transaction is configured there is no way to find the channel. Creating a new one.
        LOGGER.debug("Channel not found creating a new one");
        return createChannel(endpoint);
    }

    public Channel getOrDefaultChannel(ImmutableEndpoint endpoint,  Channel defaultChannel) throws Exception
    {
        Channel ret;

        ret = getChannel(endpoint);
        if (ret != null)
        {
            return ret;
        }

        LOGGER.debug("Channel not found using default");
        // if no transaction is configured there is no way to find the channel. Returning the default value
        return defaultChannel;
    }

    public Channel getChannel(ImmutableEndpoint endpoint)
            throws IOException, TransactionException
    {
        Channel ret;

        ret = getTransactedResourceChannel(endpoint);
        if (ret != null)
        {
            LOGGER.debug("Found Channel as transaction resource");
            return ret;
        }

        return null;
    }

    public Channel getFlowVariableChannel(final MuleMessage message)
    {
        ChannelMessageProperty channelMessageProperty = message.getInvocationProperty(AmqpConnector.MESSAGE_PROPERTY_CHANNEL);
        
        if (channelMessageProperty != null)
        {
            return channelMessageProperty.getChannel();
        }
        
        return null;
    }

    public Channel getTransactedResourceChannel(ImmutableEndpoint endpoint) throws IOException, TransactionException
    {
        if (!endpoint.getTransactionConfig().isConfigured())
        {
            return null;
        }

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
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Using transacted channel from current transaction: " + transaction);
                }

                return ((AmqpTransaction) transaction).getTransactedChannel();
            }
        }
        else if (transaction instanceof DelegateTransaction)
        {
            // we can't use the current endpoint channel because it may get closed (if the
            // endpoint instance is destroyed) while the transaction block is not done with...
            final Channel channel = createChannel(endpoint);
            channel.txSelect();
            // we wrap the channel so the transaction will know it can safely close it an
            // commit/rollback
            transaction.bindResource(channel.getConnection(), new CloseableChannelWrapper(
                    channel));

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Created transacted channel for delegate transaction: " + transaction);
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

        return null;
    }

    public Channel createChannel(ImmutableEndpoint endpoint) throws IOException
    {
        final AmqpConnector connector = (AmqpConnector) endpoint.getConnector();

        try
        {
            final Channel channel = connector.getConnection().createChannel();
            channel.addReturnListener(connector.getDefaultReturnListener());
            channel.basicQos(connector.getPrefetchSize(), connector.getPrefetchCount(), false);

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Created and configured new channel: " + channel);
            }

            channel.addShutdownListener(new ShutdownListener()
            {
                public void shutdownCompleted(final ShutdownSignalException sse)
                {
                    if (sse.isInitiatedByApplication())
                    {
                        return;
                    }

                    // do not inform the connector of the issue as it can't
                    // decide what to do reset the channel so it would later
                    // be lazily reconnected
                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug("Terminated dead channel: " + channel, sse);
                    }
                }
            });

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Shutdown listener configured on channel: " + channel);
            }

            return channel;
        }
        catch (final Exception e)
        {
            if ((!connector.isStopping()) && (connector.isStarted()))
            {
                connector.getMuleContext()
                        .getExceptionListener()
                        .handleException(
                                new ConnectException(
                                        MessageFactory.createStaticMessage("Impossible to create new channels on connection: "
                                                + connector.getConnection()), e, connector));
            }
            return null;
        }

    }

    public void closeChannelIfNotTransacted(final Channel channel) throws ConnectException
    {
        if (TransactionCoordination.getInstance().getTransaction() != null)
        {
            return;
        }

        closeChannel(channel);
    }

    public void closeChannel(final Channel channel) throws ConnectException
    {
        if (channel == null)
        {
            return;
        }

        if (!channel.isOpen())
        {
            return;
        }

        try
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Closing channel: " + channel);
            }

            channel.close();

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Closed channel: " + channel);
            }
        }
        catch (AlreadyClosedException alreadyClosedException)
        {
            LOGGER.warn("Attempted to close an already closed channel: " + channel, alreadyClosedException);
        }
        catch (final Exception e)
        {
            LOGGER.warn("Failed to close channel: " + channel, e);
        }
    }
}
