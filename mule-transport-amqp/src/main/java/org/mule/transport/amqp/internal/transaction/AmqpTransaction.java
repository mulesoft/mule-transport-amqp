/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.transaction;

import static org.mule.transport.amqp.internal.transaction.AmqpTransaction.RecoverStrategy.NONE;

import java.io.IOException;

import org.apache.commons.lang.Validate;
import org.mule.api.MuleContext;
import org.mule.api.transaction.TransactionException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.AbstractSingleResourceTransaction;
import org.mule.transaction.IllegalTransactionStateException;

import com.rabbitmq.client.Channel;

/**
 * {@link AmqpTransaction} is a wrapper for an AMQP local transaction. This object holds the AMQP
 * channel and controls when the transaction is committed or rolled back.
 */
public class AmqpTransaction extends AbstractSingleResourceTransaction
{
    public enum RecoverStrategy
    {
        NONE, NO_REQUEUE, REQUEUE
    };

    private final RecoverStrategy recoverStrategy;

    public AmqpTransaction(final MuleContext muleContext, final RecoverStrategy recoverStrategy)
    {
        super(muleContext);

        Validate.notNull(recoverStrategy, "recoverStrategy can't be null");
        this.recoverStrategy = recoverStrategy;
    }

    @Override
    public void bindResource(final Object key, final Object resource) throws TransactionException
    {
        if (!(resource instanceof Channel || resource instanceof CloseableChannelWrapper))
        {
            throw new IllegalTransactionStateException(
                CoreMessages.transactionCanOnlyBindToResources(Channel.class.getName()));
        }

        super.bindResource(key, resource);
    }

    @Override
    protected void doBegin() throws TransactionException
    {
        // NOOP
    }

    @Override
    protected void doCommit() throws TransactionException
    {
        if (resource == null)
        {
            logger.warn(CoreMessages.commitTxButNoResource(this));
            return;
        }

        final Channel channel = getTransactedChannel();

        try
        {
            channel.txCommit();

            if (logger.isDebugEnabled())
            {
                logger.debug("Committed AMQP transaction on channel: " + channel);
            }
        }
        catch (final IOException ioe)
        {
            throw new TransactionException(CoreMessages.transactionCommitFailed(), ioe);
        }
        finally
        {
            closeChannelIfNeeded(channel);
        }
    }

    @Override
    protected void doRollback() throws TransactionException
    {
        if (resource == null)
        {
            logger.warn(CoreMessages.rollbackTxButNoResource(this));
            return;
        }

        final Channel channel = getTransactedChannel();

        try
        {
            try
            {
                channel.txRollback();

                if (logger.isDebugEnabled())
                {
                    logger.debug("Rolled back AMQP transaction (" + recoverStrategy + ") on channel: "
                                 + channel);
                }
            }
            catch (final IOException ioe)
            {
                throw new TransactionException(CoreMessages.transactionRollbackFailed(), ioe);
            }

            applyRecoverStrategy(channel);
        }
        finally
        {
            closeChannelIfNeeded(channel);
        }
    }

    protected void applyRecoverStrategy(final Channel channel)
    {
        try
        {
            switch (recoverStrategy)
            {
                case NONE :
                    // NOOP
                    break;
                case NO_REQUEUE :
                    channel.basicRecover(false);
                    break;
                case REQUEUE :
                    channel.basicRecover(true);
                    break;
            }

            if ((recoverStrategy != NONE) && (logger.isDebugEnabled()))
            {
                logger.debug("Applied " + recoverStrategy + " recover strategy on channel: " + channel);
            }
        }
        catch (final IOException ioe)
        {
            logger.warn("Failed to recover channel " + channel + " after rollback (recoverStrategy is "
                        + recoverStrategy + ")");
        }
    }

    public Channel getTransactedChannel()
    {
        return resource instanceof CloseableChannelWrapper
                                                          ? ((CloseableChannelWrapper) resource).getChannel()
                                                          : (Channel) resource;
    }

    protected boolean shouldCloseChannel()
    {
        return resource instanceof CloseableChannelWrapper;
    }

    protected void closeChannelIfNeeded(final Channel channel)
    {
        if (shouldCloseChannel())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Closing transacted channel: " + channel);
            }

            try
            {
                channel.close();
            }
            catch (final IOException ioe)
            {
                logger.error("Failed to close transacted channel: " + channel, ioe);
            }
        }
    }
}
