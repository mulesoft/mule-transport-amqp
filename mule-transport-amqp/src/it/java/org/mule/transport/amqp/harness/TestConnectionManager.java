/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.harness;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a connection to RabbitMQ and its channels. A channel count is kept in order
 * to cleanly shutdown the connection when is not needed anymore.
 * 
 * It relies on the following System properties: amqpHost, amqpPort, amqpUserName,
 * amqpPassword and amqpVirtualHost.
 */
public class TestConnectionManager 
{
	protected static Connection connection;
	
	protected static Channel channel;
	
	protected static AtomicInteger channelCount = new AtomicInteger();
	
	public Connection getConnection() throws IOException, TimeoutException
	{
		if (connection != null && connection.isOpen())
		{
			return connection;
		}
		
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(System.getProperty("amqpHost"));
		factory.setPort(Integer.valueOf(System.getProperty("amqpPort")));
	    factory.setUsername(System.getProperty("amqpUserName"));
	    factory.setPassword(System.getProperty("amqpPassword"));
	    factory.setVirtualHost(System.getProperty("amqpVirtualHost"));
	    connection = factory.newConnection();
	    
	    return connection;
	}
	
	public Channel getChannel() throws IOException, TimeoutException
	{
		if (connection == null || !connection.isOpen())
		{
			getConnection();
		}

		channelCount.incrementAndGet();
		
        return connection.createChannel();
	}

	public void disposeChannel(Channel channel)
	{
		if (channel == null)
		{
			return;
		}

		try
		{
			channel.close();
		}
		catch (Exception e)
		{
			// Ignore exception
		}
		finally
		{
			if (channelCount.decrementAndGet() == 0)
			{
				try
				{
					connection.close();
				}
				catch (Exception e)
				{
					// Ignore exception.
				}
			}
		}
	}

}
