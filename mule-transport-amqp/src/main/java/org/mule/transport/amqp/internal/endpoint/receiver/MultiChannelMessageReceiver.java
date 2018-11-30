/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.endpoint.receiver;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.getProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.amqp.internal.client.AmqpDeclarer;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.transport.amqp.internal.endpoint.AmqpEndpointUtil;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.DaemonThreadFactory;

import com.rabbitmq.client.Channel;

/**
 * In Mule an endpoint corresponds to a single receiver. It's up to the receiver to do multithreaded consumption and
 * resource allocation, if needed. This class honors the <code>numberOfConcurrentTransactedReceivers</code> strictly and
 * will create exactly this number of consumers.
 */
public class MultiChannelMessageReceiver extends AbstractMessageReceiver
{
    public static int DEFAULT_CONSUMER_RECOVERY_INTERVAL = 10000;

    public static final String MULE_ASYNC_CONSUMERS_STARTUP = SYSTEM_PROPERTY_PREFIX + "async.consumers.startup";

    protected final AmqpConnector amqpConnector;

    protected AmqpDeclarer declarator;

    protected final List<MultiChannelMessageSubReceiver> subReceivers = new ArrayList<MultiChannelMessageSubReceiver>();

    protected int numberOfChannels;
    protected String queueName;

    private boolean started = false;
    private boolean declared = false;

    protected ScheduledThreadPoolExecutor scheduler;

    private boolean asyncConsumersStartup;


    public MultiChannelMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        asyncConsumersStartup = getBoolean(MULE_ASYNC_CONSUMERS_STARTUP);
        this.amqpConnector = (AmqpConnector) connector;
        declarator = new AmqpDeclarer();
        numberOfChannels = new AmqpEndpointUtil().getNumberOfChannels(endpoint);
    }

    @Override
    protected synchronized void doConnect() throws Exception
    {
        started = true;
        logger.info("Connecting message receiver for endpoint " + endpoint.getEndpointURI());

        try
        {
            synchronized (subReceivers)
            {
                clearSubreceivers();

                createSubreceivers();

                // If the async consumers startup flag is set, the consumers recovery thread
                // will recreates the subreceivers asynchronously, not blocking the context
                // startup process.
                if (!asyncConsumersStartup)
                {
                    startSubReceivers();
                }

                logger.info("Message receiver for endpoint " + endpoint.getEndpointURI() + " has been successfully connected.");
            }

            triggerScheduler();

        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    protected void createSubreceivers() throws CreateException, InitialisationException
    {
        for (int i = 0; i < numberOfChannels; i++)
        {
            MultiChannelMessageSubReceiver sub = new MultiChannelMessageSubReceiver(this, asyncConsumersStartup);
            sub.initialise();
            sub.setListener(listener);
            subReceivers.add(sub);
        }
    }

    protected void startSubReceivers() throws MuleException
    {
        for (MultiChannelMessageSubReceiver channel : subReceivers)
        {
            channel.doStart();
        }
    }

    private void triggerScheduler()
    {
        if (scheduler == null)
        {
            this.scheduler = new ScheduledThreadPoolExecutor(1);
            scheduler.setThreadFactory(
                    new DaemonThreadFactory("ConsumerRecreationMonitor", this.getClass().getClassLoader()));
            scheduler.scheduleWithFixedDelay(new ConsumerRecoveryMonitor(subReceivers), 0, DEFAULT_CONSUMER_RECOVERY_INTERVAL, MILLISECONDS);
        }
    }

    private void clearSubreceivers() throws MuleException
    {
        if (subReceivers == null)
        {
            return;
        }

        for (MultiChannelMessageSubReceiver channel : subReceivers)
        {
            if (!channel.isStopped())
            {
                channel.stop();
            }
        }

        subReceivers.clear();
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        super.doStop();
        logger.debug("doDisconnect()");

        synchronized (subReceivers)
        {
            clearSubreceivers();
        }

        stopScheduler();
    }

    private void stopScheduler()
    {
        if (scheduler != null)
        {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    protected void declareEndpoint(Channel channel) throws IOException
    {
        if (started && !declared)
        {
            queueName = declarator.declareEndpoint(channel, endpoint, true);
            declared = true;
        }
        else
        {
            final String exchangeName = declarator.declareExchange(channel, endpoint, true);
            String routingKey = declarator.getEndpointUtil().getRoutingKey(endpoint);
            declarator.declareBinding(channel, exchangeName, routingKey, queueName);
        }
    }


    protected String getQueueOrCreateTemporaryQueue(Channel channel) throws IOException
    {
        if (StringUtils.isEmpty(queueName))
        {
            queueName = new AmqpEndpointUtil().getQueueName(endpoint.getAddress());
            logger.debug("queue: " + queueName + "found for " + endpoint.getAddress());
        }
        return queueName;
    }

    private static class ConsumerRecoveryMonitor implements Runnable
    {

        protected transient Log logger = LogFactory.getLog(getClass());
        private List<MultiChannelMessageSubReceiver> subReceivers;

        public ConsumerRecoveryMonitor(List<MultiChannelMessageSubReceiver> subReceivers)
        {
            this.subReceivers = subReceivers;
        }

        public void run()
        {

            synchronized (subReceivers)
            {
                for (MultiChannelMessageSubReceiver subReceiver : subReceivers)
                {
                    try
                    {
                        if (subReceiver.isCancelled())
                        {
                            subReceiver.consume();
                        }
                    }
                    catch (Exception e)
                    {
                        logger.info("The scheduled recovery couldn't restart the consumer");
                        // Ensure the release resources which where taken in
                        // in the recreation attempt.
                        subReceiver.cancelConsumer();
                    }
                }
            }
        }

    }

}
