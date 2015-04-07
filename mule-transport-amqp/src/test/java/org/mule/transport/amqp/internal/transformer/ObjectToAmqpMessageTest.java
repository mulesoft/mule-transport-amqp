/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.transformer;

import java.util.HashMap;
import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.mule.transport.amqp.internal.domain.AmqpMuleMessageFactory;
import org.mule.transport.amqp.internal.domain.AmqpMuleMessageFactoryTestCase;

public class ObjectToAmqpMessageTest extends AbstractTransformerTestCase
{
    private final AmqpMessage amqpMessage = AmqpMuleMessageFactoryTestCase.getTestMessage();

    @Override
    public Transformer getTransformer() throws Exception
    {
        return new ObjectToAmqpMessage();
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    @Override
    public Object getTestData()
    {
        try
        {
            final MuleMessage muleMessage = new AmqpMuleMessageFactory(muleContext).create(amqpMessage,
                "utf-8");

            muleMessage.setPayload(amqpMessage.getBody());

            // massage properties to ensure all non Mule ones are correctly picked-up
            final Map<String, Object> inboundProperties = new HashMap<String, Object>();
            for (final String name : muleMessage.getInboundPropertyNames())
            {
                if (!name.startsWith("MULE_"))
                {
                    inboundProperties.put(name, muleMessage.getInboundProperty(name));
                }
            }
            muleMessage.clearProperties(PropertyScope.OUTBOUND);
            muleMessage.clearProperties(PropertyScope.INBOUND);
            muleMessage.addProperties(inboundProperties, PropertyScope.OUTBOUND);
            return muleMessage;
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getResultData()
    {
        return amqpMessage;
    }

}
