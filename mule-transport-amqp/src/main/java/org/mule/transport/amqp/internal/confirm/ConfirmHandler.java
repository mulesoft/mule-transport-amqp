/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.confirm;

import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

class ConfirmHandler
{
    private Latch latch = new Latch();
    private boolean successful = false;

    public void confirm(boolean successful)
    {
        this.successful = successful;
        latch.release();
    }

    public boolean awaitConfirmation(long timeout, TimeUnit timeUnit)
    {
        try
        {
            latch.await(timeout, timeUnit);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        return successful;
    }

}
