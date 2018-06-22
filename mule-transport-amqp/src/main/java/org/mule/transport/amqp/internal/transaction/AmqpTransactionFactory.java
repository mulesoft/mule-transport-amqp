/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.transaction;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.UniversalTransactionFactory;
import org.mule.transport.amqp.internal.transaction.AmqpTransaction.RecoverStrategy;

/**
 * {@link AmqpTransactionFactory} creates an AMQP local transaction.
 */
public class AmqpTransactionFactory implements UniversalTransactionFactory
{
    public static final RecoverStrategy DEFAULT_RECOVER_STRATEGY = RecoverStrategy.REQUEUE;

    private RecoverStrategy recoverStrategy = DEFAULT_RECOVER_STRATEGY;

    public Transaction beginTransaction(final MuleContext muleContext) throws TransactionException
    {
        final AmqpTransaction tx = new AmqpTransaction(muleContext, recoverStrategy);
        tx.begin();
        return tx;
    }

    public boolean isTransacted()
    {
        return true;
    }

    public Transaction createUnboundTransaction(final MuleContext muleContext) throws TransactionException
    {
        return new AmqpTransaction(muleContext, recoverStrategy);
    }

    public RecoverStrategy getRecoverStrategy()
    {
        return recoverStrategy;
    }

    public void setRecoverStrategy(final RecoverStrategy recoverStrategy)
    {
        this.recoverStrategy = recoverStrategy;
    }
}
