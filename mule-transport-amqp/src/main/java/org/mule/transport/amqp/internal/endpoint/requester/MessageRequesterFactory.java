/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.requester;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractMessageRequesterFactory;

/**
 * Creates instances of <code>MessageRequester</code>.
 */
public class MessageRequesterFactory extends AbstractMessageRequesterFactory
{

    @Override
    public org.mule.api.transport.MessageRequester create(final InboundEndpoint endpoint) throws MuleException
    {
        return new MessageRequester(endpoint);
    }

}
