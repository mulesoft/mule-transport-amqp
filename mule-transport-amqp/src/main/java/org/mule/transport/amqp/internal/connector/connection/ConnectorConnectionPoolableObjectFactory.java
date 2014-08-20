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

import com.rabbitmq.client.Channel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.mule.transport.amqp.internal.connector.AmqpConnector;

public class ConnectorConnectionPoolableObjectFactory extends BasePoolableObjectFactory
{
    private static final Log logger = LogFactory.getLog(ConnectorConnectionPoolableObjectFactory.class);

    private final AmqpConnector amqpConnector;

    public ConnectorConnectionPoolableObjectFactory(final AmqpConnector amqpConnector)
    {
        this.amqpConnector = amqpConnector;
    }

    @Override
    public Object makeObject() throws Exception
    {
        final ConnectorConnection connectorConnection = new ConnectorConnection(amqpConnector);
        if (logger.isDebugEnabled())
        {
            logger.debug("Created new: " + connectorConnection);
        }
        return connectorConnection;
    }

    @Override
    public boolean validateObject(final Object obj)
    {
        final Channel channel = ((ConnectorConnection) obj).getChannel();
        return channel != null && channel.isOpen();
    }

    @Override
    public void destroyObject(final Object obj) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Destroying " + obj);
        }

        try
        {
            final Channel channel = ((ConnectorConnection) obj).getChannel();
            if ((channel != null) && (channel.isOpen()))
            {
                channel.close();
            }
        }
        catch (final Exception e)
        {
            logger.info("Ignored exception when destroying ConnectorConnection:" + obj, e);
        }
    }
}
