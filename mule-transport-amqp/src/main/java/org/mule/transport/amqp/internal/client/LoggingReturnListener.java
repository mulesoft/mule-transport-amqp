/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggingReturnListener extends AbstractAmqpReturnHandlerListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingReturnListener.class);

    protected final AtomicInteger hitCount = new AtomicInteger(0);

    @Override
    protected void doHandleReturn(final String errorMessage,
                                  final Map<String, Object> ignored,
                                  final AmqpMessage returnedAmqpMessage)
    {
        hitCount.incrementAndGet();
        LOGGER.warn(String.format("%s: %s", errorMessage, returnedAmqpMessage));
    }

    public int getHitCount()
    {
        return hitCount.intValue();
    }
}
