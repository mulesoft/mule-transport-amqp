/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.transaction;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

import com.rabbitmq.client.Connection;

public class AmqpTransactionFactoryRegistor implements MuleContextAware, Initialisable
{
    private MuleContext muleContext;

    public void initialise() throws InitialisationException
    {
        muleContext.getTransactionFactoryManager().registerTransactionFactory(Connection.class,
            new AmqpTransactionFactory());
    }

    public void setMuleContext(final MuleContext context)
    {
        this.muleContext = context;
    }
}
