/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.dispatcher;

import java.io.IOException;

/**
 * Exception to throw in case Rabbitmq informed the broker is blocked.
 *
 */
public class AmqpBlockedBrokerException extends IOException
{

    public AmqpBlockedBrokerException(String reason)
    {
        super("No publishing in AQMP broker can be done. A notification was received indicating the broker is blocked. " + reason);
    }
}
