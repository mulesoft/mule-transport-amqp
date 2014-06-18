/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp;

import org.mule.api.MuleEvent;

import com.rabbitmq.client.Channel;

import java.util.concurrent.TimeUnit;

public interface AmqpConfirmsManager
{

    void requestConfirm(Channel channel, MuleEvent event) throws Exception;

    boolean awaitConfirm(Channel channel, MuleEvent event, long timeout, TimeUnit timeUnit);

    void forget(MuleEvent event);
}
