/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.util.UUID;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

public abstract class AbstractAmqpITCase extends FunctionalTestCase
{
    protected final ConnectionFactory factory;
    protected final Connection connection;
    private final AtomicReference<Channel> channelRef = new AtomicReference<Channel>();

    public AbstractAmqpITCase() throws IOException
    {
        super();
        setDisposeContextPerClass(true);

        factory = new ConnectionFactory();
        factory.setUsername("mule");
        factory.setPassword("elum");
        factory.setVirtualHost("mule-test");
        connection = factory.newConnection();
    }

    private Channel newChannel() throws IOException
    {
        final Channel channel = connection.createChannel();
        channel.addShutdownListener(new ShutdownListener()
        {
            public void shutdownCompleted(final ShutdownSignalException sse)
            {
                if (!sse.isInitiatedByApplication())
                {
                    channelRef.set(null);
                }
            }
        });
        return channel;
    }

    protected Channel getChannel() throws IOException
    {
        Channel channel = channelRef.get();

        if ((channel != null) && (channel.isOpen()))
        {
            return channel;
        }

        channel = newChannel();

        if (channelRef.compareAndSet(null, channel))
        {
            return channel;
        }

        return getChannel();
    }

    @Override
    protected boolean isGracefulShutdown()
    {
        return true;
    }

    protected Future<MuleMessage> setupFunctionTestComponentForFlow(final String flowName) throws Exception
    {

        final FunctionalTestComponent functionalTestComponent = getFunctionalTestComponent(flowName);

        final CountDownLatch messageReceivedLatch = new CountDownLatch(1);
        final AtomicReference<MuleMessage> receivedMessageRef = new AtomicReference<MuleMessage>(null);

        functionalTestComponent.setEventCallback(new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object component)
                throws Exception
            {
                receivedMessageRef.set(context.getMessage());
                messageReceivedLatch.countDown();
            }
        });

        final Future<MuleMessage> futureReceivedMessage = new Future<MuleMessage>()
        {
            public boolean cancel(final boolean mayInterruptIfRunning)
            {
                throw new UnsupportedOperationException();
            }

            public boolean isCancelled()
            {
                throw new UnsupportedOperationException();
            }

            public boolean isDone()
            {
                throw new UnsupportedOperationException();
            }

            public MuleMessage get() throws InterruptedException, ExecutionException
            {
                throw new UnsupportedOperationException();
            }

            public MuleMessage get(final long timeout, final TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException
            {
                if (messageReceivedLatch.await(timeout, unit))
                {
                    return receivedMessageRef.get();
                }
                else
                {
                    return null;
                }
            }
        };

        return futureReceivedMessage;
    }

    protected void setupDirectExchangeAndQueue(final String flowName) throws IOException
    {
        final String exchange = getExchangeName(flowName);
        getChannel().exchangeDeclare(exchange, "direct");

        final String queue = setupQueue(flowName);
        getChannel().queueBind(queue, exchange, queue);
        getChannel().queuePurge(queue);
    }

    protected void setupExchangeAndQueue(final String flowName) throws IOException
    {
        final String exchange = setupExchange(flowName);
        final String queue = setupQueue(flowName);
        getChannel().queueBind(queue, exchange, "");
        getChannel().queuePurge(queue);
    }

    protected String setupQueue(final String flowName) throws IOException
    {
        final String queue = getQueueName(flowName);
        getChannel().queueDeclare(queue, false, false, true, Collections.<String, Object> emptyMap());
        return queue;
    }

    protected String setupExchange(final String flowName) throws IOException
    {
        final String exchange = getExchangeName(flowName);
        getChannel().exchangeDeclare(exchange, "fanout");
        return exchange;
    }

    protected void deleteExchange(final String flowName) throws InterruptedException
    {
        final String exchange = getExchangeName(flowName);
        try
        {
            getChannel().exchangeDelete(exchange);
        }
        catch (final IOException ioe)
        {
            // ignored
            Thread.sleep(1000L);
        }
    }

    protected void deleteQueue(final String flowName) throws InterruptedException
    {
        final String queue = getQueueName(flowName);
        try
        {
            getChannel().queueDelete(queue);
        }
        catch (final IOException ioe)
        {
            // ignored
            Thread.sleep(1000L);
        }
    }

    protected String getQueueName(final String flowName)
    {
        return flowName + "-queue";
    }

    protected static String getExchangeName(final String flowName)
    {
        return flowName + "-exchange";
    }

    protected Delivery sendMessageWithAmqp(final String correlationId,
                                           final byte[] body,
                                           final String flowName,
                                           final long timeout) throws IOException, InterruptedException
    {
        final DeclareOk declareOk = getChannel().queueDeclare();
        final String queue = declareOk.getQueue();
        publishMessageWithAmqp(correlationId, body, flowName, queue);
        return consumeMessageWithAmqp(queue, timeout);
    }

    protected String publishMessageWithAmqp(final byte[] body, final String flowName) throws IOException
    {
        return publishMessageWithAmqp(body, flowName, null);
    }

    protected String publishMessageWithAmqp(final byte[] body, final String flowName, final String replyTo)
        throws IOException
    {
        final String correlationId = UUID.getUUID();
        publishMessageWithAmqp(correlationId, body, flowName, replyTo);
        return correlationId;
    }

    protected void publishMessageWithAmqp(final String correlationId,
                                          final byte[] body,
                                          final String flowName,
                                          final String replyTo) throws IOException
    {
        final AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
        bob.contentType("text/plain").correlationId(correlationId).replyTo(replyTo);
        bob.headers(Collections.<String, Object> singletonMap("customHeader", 123L));

        final BasicProperties props = bob.build();
        final String exchangeName = getExchangeName(flowName);
        getChannel().basicPublish(exchangeName, "", props, body);
        logger.info("Published " + props + " / " + new String(body) + " to: " + exchangeName
                    + " with empty routing key");
    }

    protected String publishMessageWithAmqpToDefaultExchange(final byte[] body, final String routingKey)
        throws IOException
    {
        final String correlationId = UUID.getUUID();
        publishMessageWithAmqpToDefaultExchange(correlationId, body, routingKey);
        return correlationId;
    }

    protected void publishMessageWithAmqpToDefaultExchange(final String correlationId,
                                                           final byte[] body,
                                                           final String routingKey) throws IOException
    {
        final AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
        bob.contentType("text/plain").correlationId(correlationId);
        bob.headers(Collections.<String, Object> singletonMap("customHeader", 123L));

        final BasicProperties props = bob.build();
        getChannel().basicPublish("", routingKey, props, body);
        logger.info("Published " + props + " / " + new String(body) + " to default exchange"
                    + " with routing key " + routingKey);
    }

    protected GetResponse waitUntilGetMessageWithAmqp(final String queue, final long timeout)
        throws IOException, InterruptedException
    {
        final long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeout)
        {
            final GetResponse getResponse = getMessageWithAmqp(queue);
            if (getResponse != null)
            {
                return getResponse;
            }

            Thread.yield();
            Thread.sleep(250L);
        }

        return null;
    }

    protected GetResponse getMessageWithAmqp(final String queue) throws IOException
    {
        return getChannel().basicGet(queue, true);
    }

    protected Delivery consumeMessageWithAmqp(final String queue, final long timeout)
        throws IOException, InterruptedException
    {
        return consumeMessagesWithAmqp(queue, timeout, 1).get(0);
    }

    protected List<Delivery> consumeMessagesWithAmqp(final String queue,
                                                     final long timeout,
                                                     final int expectedCount)
        throws IOException, InterruptedException
    {
        final List<Delivery> deliveries = new ArrayList<Delivery>();

        final QueueingConsumer consumer = new QueueingConsumer(getChannel());
        final String consumerTag = getChannel().basicConsume(queue, true, consumer);

        for (int i = 0; i < expectedCount; i++)
        {
            deliveries.add(consumer.nextDelivery(timeout));
        }

        getChannel().basicCancel(consumerTag);

        return deliveries;
    }

    protected void assertValidReceivedMessage(final String correlationId,
                                              final byte[] body,
                                              final MuleMessage receivedMessage) throws Exception
    {
        assertNotNull(receivedMessage);
        assertTrue(receivedMessage.getPayload() instanceof byte[]);
        assertTrue(Arrays.equals(body, receivedMessage.getPayloadAsBytes()));
        assertEquals(correlationId, receivedMessage.getCorrelationId());
        assertEquals(correlationId, receivedMessage.getInboundProperty(AmqpConnector.MESSAGE_PROPERTY_CORRELATION_ID));
        assertEquals(123L, receivedMessage.getInboundProperty("customHeader"));

        final Map<String, Object> allHeaders = receivedMessage.getInboundProperty("amqp.headers");
        assertNotNull(allHeaders);
        assertEquals(1, allHeaders.size());
        assertEquals(123L, allHeaders.get("customHeader"));
    }
}
