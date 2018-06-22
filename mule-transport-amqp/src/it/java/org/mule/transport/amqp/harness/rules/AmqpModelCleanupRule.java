/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.harness.rules;

import org.mule.transport.amqp.harness.TestConnectionManager;

import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.rules.ExternalResource;

/**
 * JUnit Rule that deletes the given queues and exchanges before
 * and after each execution.
 */
public class AmqpModelCleanupRule extends ExternalResource
{
    protected TestConnectionManager connectionFactory = new TestConnectionManager();
    
	protected Channel channel;

	protected String[] queues;
	
	protected String[] exchanges;
	
	public AmqpModelCleanupRule(String[] queues, String[] exchanges)
	{
		this.queues = queues;
		this.exchanges = exchanges;
	}
	
	@Override
    protected void before() throws Throwable 
	{
		channel = connectionFactory.getChannel();
		cleanAll(queues, exchanges, channel);
    }

    @Override
    protected void after() 
    {
    	cleanAll(queues, exchanges, channel);
		connectionFactory.disposeChannel(channel);
	}
    
    protected void cleanAll(String[] queues, String[] exchanges, Channel channel)
    {
    	try 
    	{
    		cleanExchanges(exchanges, channel);
    		cleanQueues(queues, channel);
		} 
    	catch (Exception e) 
    	{
			throw new RuntimeException(e);
		}
    }
    
    protected void cleanQueues(String[] queues, Channel channel) throws IOException
    {
    	for (String queue : queues)
    	{
    		channel.queueDelete(queue);
    	}
    }
    
    protected void cleanExchanges(String[] exchanges, Channel channel) throws IOException
    {
    	for (String exchange : exchanges)
    	{
    		channel.exchangeDelete(exchange);
    	}
    }

}
