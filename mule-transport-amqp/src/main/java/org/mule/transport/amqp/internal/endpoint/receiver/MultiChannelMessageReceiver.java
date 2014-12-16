/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;


import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.amqp.internal.client.AmqpDeclarer;
import org.mule.transport.amqp.internal.client.ChannelHandler;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.endpoint.AmqpEndpointUtil;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * In Mule an endpoint corresponds to a single receiver. It's up to the receiver to do multithreaded consumption and
 * resource allocation, if needed. This class honors the <code>numberOfConcurrentTransactedReceivers</code> strictly
 * and will create exactly this number of consumers.
 */
public class MultiChannelMessageReceiver extends AbstractMessageReceiver
{
    protected final AmqpConnector amqpConnector;

    protected AmqpDeclarer declarator;

    protected final List<MultiChannelMessageSubReceiver> subReceivers;

    protected int numberOfChannels;
    protected String queueName;

    private boolean started = false;
    private boolean declared = false;


    public MultiChannelMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);

        this.amqpConnector = (AmqpConnector) connector;
        declarator = new AmqpDeclarer();
        numberOfChannels = new AmqpEndpointUtil().getNumberOfChannels(endpoint);
        subReceivers = new ArrayList<MultiChannelMessageSubReceiver>(numberOfChannels);
    }

    @Override
    public synchronized void doStart() throws MuleException
    {
        started = true;
        logger.info("Starting message receiver for endpoint " + endpoint.getEndpointURI());

        try
        {
            for (int i = 0; i < numberOfChannels; i++)
            {
                MultiChannelMessageSubReceiver sub = new MultiChannelMessageSubReceiver(this);
                sub.initialise();
                sub.setListener(listener);
                subReceivers.add(sub);
            }

            for (MultiChannelMessageSubReceiver channel : subReceivers)
            {
                channel.doStart();
            }

            logger.info("Message receiver for endpoint " + endpoint.getEndpointURI() + " has been successfully connected.");
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    @Override
    public void doStop() throws MuleException
    {
        super.doStop();
        logger.debug("doStop()");

        for (MultiChannelMessageSubReceiver sub : subReceivers)
        {
            sub.doStop();
        }

        subReceivers.clear();
    }

    protected void declareEndpoint(Channel channel) throws IOException
    {
        if (started && !declared)
        {
            queueName = declarator.declareEndpoint(channel, endpoint, true);
            declared = true;
        }
    }


    protected String getQueueOrCreateTemporaryQueue(Channel channel) throws IOException
    {
        if (StringUtils.isEmpty(queueName))
        {
            queueName = new AmqpEndpointUtil().getQueueName(endpoint.getAddress());
            logger.debug("queue: " + queueName + "found for " +  endpoint.getAddress());
        }
        return queueName;
    }
}
