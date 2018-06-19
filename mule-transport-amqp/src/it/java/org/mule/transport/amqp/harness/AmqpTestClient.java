/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.harness;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.RandomStringUtils;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.amqp.internal.client.QueueingConsumer;
import org.mule.transport.amqp.internal.connector.AmqpConnector;
import org.mule.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.GetResponse;


public class AmqpTestClient 
{
    private static  Logger logger = LoggerFactory.getLogger(AmqpTestClient.class);

	protected Channel channel;
	
	public AmqpTestClient(Channel channel)
	{
		this.channel = channel;
	}
	
	public void dispatchTestMessageAndAssertValidReceivedMessageWithAmqp(String exchangeName, 
			FunctionalTestComponent functionalTestComponent, int timeoutSeconds) throws Exception
    {
        Future<MuleMessage> futureReceivedMessage = setupFunctionTestComponentForFlow(functionalTestComponent);

        byte[] body = RandomStringUtils.randomAlphanumeric(20).getBytes();
        String correlationId = publishMessageWithAmqp(body, exchangeName);

        MuleMessage receivedMessage = futureReceivedMessage.get(timeoutSeconds, TimeUnit.SECONDS);
        assertValidReceivedMessage(correlationId, body, receivedMessage);
    }
	
	
	public void dispatchAndReceiveAMQPS(String vmPath, MuleContext muleContext, 
			FunctionalTestComponent functionalTestComponent, int timeoutSeconds) throws Exception
    {
		Future<MuleMessage> futureMuleMessage = setupFunctionTestComponentForFlow(functionalTestComponent);
		String testPayload = RandomStringUtils.randomAlphanumeric(20);

		muleContext.getClient().dispatch(vmPath, new DefaultMuleMessage(testPayload, muleContext));

		MuleMessage muleMessage = futureMuleMessage.get(timeoutSeconds, TimeUnit.SECONDS);
		assertThat(muleMessage.getPayloadAsString(), is(testPayload));
    }
	
	public Delivery sendMessageWithAmqp(String correlationId, byte[] body, 
			String exchangeName, long timeout) throws IOException, InterruptedException
	{
		com.rabbitmq.client.AMQP.Queue.DeclareOk declareOk = channel.queueDeclare();
		String queue = declareOk.getQueue();

		publishMessageWithAmqp(correlationId, body, exchangeName, queue);
		
		return consumeMessageWithAmqp(queue, timeout);
	}

	public String publishMessageWithAmqp( byte[] body,  String exchangeName) throws IOException
	{
		return publishMessageWithAmqp(body, exchangeName, null);
	}

	public String publishMessageWithAmqp( byte[] body,  String exchangeName,  String replyTo)
			throws IOException
	{
		String correlationId = UUID.getUUID();
		publishMessageWithAmqp(correlationId, body, exchangeName, replyTo);
		return correlationId;
	}

	public void publishMessageWithAmqp( String correlationId,
            byte[] body, String exchangeName, String replyTo) throws IOException
	{
		AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
		bob.contentType("text/plain").correlationId(correlationId).replyTo(replyTo);
		bob.headers(Collections.<String, Object> singletonMap("customHeader", 123L));

		BasicProperties props = bob.build();
		channel.basicPublish(exchangeName, "", props, body);
		logger.info("Published " + props + " / " + new String(body) + " to: " + exchangeName
				+ " with empty routing key");
	}

	public String publishMessageWithAmqpToDefaultExchange( byte[] body,  String routingKey)
			throws IOException
	{
		String correlationId = UUID.getUUID();
		publishMessageWithAmqpToDefaultExchange(correlationId, body, routingKey);
		return correlationId;
	}

	public void publishMessageWithAmqpToDefaultExchange( String correlationId,
			byte[] body, String routingKey) throws IOException
	{
		AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
		bob.contentType("text/plain").correlationId(correlationId);
		bob.headers(Collections.<String, Object> singletonMap("customHeader", 123L));

		BasicProperties props = bob.build();
		channel.basicPublish("", routingKey, props, body);
		logger.info("Published " + props + " / " + new String(body) + " to default exchange"
				+ " with routing key " + routingKey);
	}

	public GetResponse waitUntilGetMessageWithAmqp( String queue,  long timeout)
			throws IOException, InterruptedException
	{
		long startTime = System.currentTimeMillis();

		while (System.currentTimeMillis() - startTime < timeout)
		{
			GetResponse getResponse = getMessageWithAmqp(queue);
			if (getResponse != null)
			{
				return getResponse;
			}

			Thread.yield();
			Thread.sleep(250L);
		}

		return null;
	}

	public GetResponse getMessageWithAmqp( String queue) throws IOException
	{
		return channel.basicGet(queue, true);
	}

	public Delivery consumeMessageWithAmqp( String queue,  long timeout)
			throws IOException, InterruptedException
	{
		return consumeMessagesWithAmqp(queue, timeout, 1).get(0);
	}

	public List<Delivery> consumeMessagesWithAmqp( String queue,
			long timeout, int expectedCount) throws IOException, InterruptedException
    {
		List<Delivery> deliveries = new ArrayList<Delivery>();
		QueueingConsumer consumer = new QueueingConsumer(channel);
		String consumerTag = channel.basicConsume(queue, true, consumer);

		for (int i = 0; i < expectedCount; i++)
		{
			deliveries.add(consumer.nextDelivery(timeout));
		}

		channel.basicCancel(consumerTag);
		return deliveries;
    }

	public void assertValidReceivedMessage( String correlationId,
			byte[] body, MuleMessage receivedMessage) throws Exception
	{
		assertThat(receivedMessage, is(notNullValue()));
		assertThat(receivedMessage.getPayload(), is(instanceOf(byte[].class)));
		assertThat(receivedMessage.getPayloadAsBytes(), is(equalTo(body)));

		assertThat(receivedMessage.getCorrelationId(), is(equalTo(correlationId)));
		assertThat((String) receivedMessage.getInboundProperty(AmqpConnector.MESSAGE_PROPERTY_CORRELATION_ID), 
				is(equalTo(correlationId)));
		assertThat((Long) receivedMessage.getInboundProperty("customHeader"), is(equalTo(123L)));

		Map<String, Object> allHeaders = receivedMessage.getInboundProperty("amqp.headers");
		assertThat(allHeaders, is(notNullValue()));
		assertThat(allHeaders.entrySet(), hasSize(1));
		assertThat((Long) allHeaders.get("customHeader"), is(equalTo(123L)));
	}

	public void validateAmqpDeliveredMessage( String expectedPayload,
             String expectedCustomHeaderValue, byte[] body, BasicProperties basicProperties)
	{
		assertThat(new String(body), is(equalTo(expectedPayload)));
		assertThat(basicProperties.getHeaders().get("customHeader").toString(), is(equalTo(expectedCustomHeaderValue)));
	}
	
	public Future<MuleMessage> setupFunctionTestComponentForFlow
		(FunctionalTestComponent functionalTestComponent) throws Exception
    {
         final CountDownLatch messageReceivedLatch = new CountDownLatch(1);
         final AtomicReference<MuleMessage> receivedMessageRef = new AtomicReference<MuleMessage>(null);

        functionalTestComponent.setEventCallback(new EventCallback()
        {
            public void eventReceived( MuleEventContext context,  Object component)
                throws Exception
            {
                receivedMessageRef.set(context.getMessage());
                messageReceivedLatch.countDown();
            }
        });

        Future<MuleMessage> futureReceivedMessage = new Future<MuleMessage>()
        {
            public boolean cancel( boolean mayInterruptIfRunning)
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

            public MuleMessage get( long timeout,  TimeUnit unit)
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

}
