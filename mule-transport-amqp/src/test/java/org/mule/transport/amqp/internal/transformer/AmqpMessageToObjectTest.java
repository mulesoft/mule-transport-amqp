/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.transformer;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transport.amqp.internal.domain.AmqpMessage;
import org.mule.transport.amqp.internal.domain.AmqpMuleMessageFactory;
import org.mule.transport.amqp.internal.domain.AmqpMuleMessageFactoryTestCase;

public class AmqpMessageToObjectTest extends AbstractTransformerTestCase
{
    private final AmqpMessage amqpMessage = AmqpMuleMessageFactoryTestCase.getTestMessage();

    @Override
    public Transformer getTransformer() throws Exception
    {
        return new AmqpMessageToObject();
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
            return new AmqpMuleMessageFactory(muleContext).create(amqpMessage, "utf-8");
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getResultData()
    {
        return amqpMessage.getBody();
    }
}
