/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.endpoint.dispatcher;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

/**
 * Creates instances of <code>DispatcherFactory</code>.
 */
public class DispatcherFactory extends AbstractMessageDispatcherFactory
{
    @Override
    public MessageDispatcher create(final OutboundEndpoint endpoint) throws MuleException
    {
        return new Dispatcher(endpoint);
    }
}
