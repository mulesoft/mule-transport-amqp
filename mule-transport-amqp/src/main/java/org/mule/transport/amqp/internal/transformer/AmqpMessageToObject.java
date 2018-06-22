/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.amqp.internal.domain.AmqpMessage;

public class AmqpMessageToObject extends AbstractAmqpMessageToObject
{
    @Override
    protected void declareInputOutputClasses()
    {
        registerSourceType(AMQP_MESSAGE_DATA_TYPE);
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    public Object transformMessage(final MuleMessage message, final String outputEncoding)
        throws TransformerException
    {
        return ((AmqpMessage) message.getPayload()).getBody();
    }
}
