/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector.connection;

import org.mule.transport.amqp.internal.connector.AmqpConnector;

public class ConnectorConnection extends AmqpConnection
{
    public ConnectorConnection(final AmqpConnector amqpConnector)
    {
        super(amqpConnector);
    }
}
