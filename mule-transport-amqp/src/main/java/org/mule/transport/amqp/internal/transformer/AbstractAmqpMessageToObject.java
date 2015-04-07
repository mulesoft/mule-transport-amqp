/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.transformer;

import org.mule.api.transformer.DataType;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.amqp.internal.domain.AmqpMessage;

public abstract class AbstractAmqpMessageToObject extends AbstractMessageTransformer
    implements DiscoverableTransformer
{
    public static final DataType<AmqpMessage> AMQP_MESSAGE_DATA_TYPE = DataTypeFactory.create(AmqpMessage.class);
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public AbstractAmqpMessageToObject()
    {
        super();
        declareInputOutputClasses();
    }

    protected abstract void declareInputOutputClasses();

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(final int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }
}
