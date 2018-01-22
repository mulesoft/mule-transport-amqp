/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.harness.rules;

import org.mule.transport.amqp.harness.TestConnectionManager;
import org.mule.transport.amqp.harness.rules.configuration.Binding;
import org.mule.transport.amqp.harness.rules.configuration.Configuration;
import org.mule.transport.amqp.harness.rules.configuration.Exchange;
import org.mule.transport.amqp.harness.rules.configuration.Queue;
import org.mule.util.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.rules.ExternalResource;

/**
 * JUnit Rule that sets up the AMQP model using a JSON file very similar to
 * the JSON file obtained exporting a RabbitMQ's model.
 */
public class AmqpModelRule extends ExternalResource
{
	protected Configuration configuration;
	
	protected String configFile;

    protected TestConnectionManager connectionFactory = new TestConnectionManager();
	
	protected Channel channel;

	public AmqpModelRule(String configFile)
	{
		this.configFile = configFile;
		configuration = parseConfiguration(configFile);
	}
	
	@Override
    protected void before() throws Throwable 
	{
		channel = connectionFactory.getChannel();
		applyConfiguration(configuration, channel);
    };

    @Override
    protected void after() 
    {
    	try
    	{
    		undoConfiguration(configuration, channel);
    		connectionFactory.disposeChannel(channel);
		}
    	catch (IOException e)
    	{
			throw new RuntimeException(e);
		}
	}
    
    protected void applyConfiguration(Configuration configuration, Channel channel) throws IOException
    {
    	applyExchangesConfiguration(configuration, channel);
    	applyQueuesConfiguration(configuration, channel);
    	applyBindingsConfiguration(configuration, channel);
    }

    protected void applyExchangesConfiguration(Configuration configuration, Channel channel) throws IOException
    {
    	List<Exchange> configurationExchanges = configuration.getExchanges();    
    	
    	for (Exchange exchange : configurationExchanges)
    	{
    		channel.exchangeDeclare(exchange.getName(), exchange.getType(), 
    				exchange.isDurable(), exchange.isAutoDelete(), new HashMap<String, Object>());
    	}
    }

    protected void applyQueuesConfiguration(Configuration configuration, Channel channel) throws IOException
    {
    	List<Queue> configurationQueues = configuration.getQueues();    
    	
    	for (Queue queue : configurationQueues)
    	{
    		channel.queueDeclare(queue.getName(), queue.isDurable(), 
    				false, queue.isAutoDelete(), new HashMap<String, Object>());
    		channel.queuePurge(queue.getName());
    	}    	
    }

    protected void applyBindingsConfiguration(Configuration configuration, Channel channel) throws IOException
    {
    	List<Binding> configurationBindings = configuration.getBindings();    
    	
    	for (Binding binding : configurationBindings)
    	{
    		if (binding.getDestinationType().equalsIgnoreCase("queue"))
    		{
    			channel.queueBind(binding.getDestination(), 
    					binding.getSource(), binding.getRoutingKey(), 
    					new HashMap<String, Object>());
    		} 
    		else if (binding.getDestinationType().equalsIgnoreCase("exchange"))
    		{
    			channel.exchangeBind(binding.getDestination(), 
    					binding.getSource(), binding.getRoutingKey(), 
    					new HashMap<String, Object>());    		
    		}
    	}
    }
    
    protected void undoConfiguration(Configuration configuration, Channel channel) throws IOException
    {
    	undoBindingsConfiguration(configuration, channel);
    	undoQueuesConfiguration(configuration, channel);
    	undoExchangesConfiguration(configuration, channel);
    }
    
    protected void undoExchangesConfiguration(Configuration configuration, Channel channel) throws IOException
    {
    	List<Exchange> configurationExchanges = configuration.getExchanges();    
    	
    	for (Exchange exchange : configurationExchanges)
    	{
    		channel.exchangeDelete(exchange.getName());
    	}
    }

    protected void undoQueuesConfiguration(Configuration configuration, Channel channel) throws IOException
    {
    	List<Queue> configurationQueues = configuration.getQueues();    
    	
    	for (Queue queue : configurationQueues)
    	{
    		channel.queueDelete(queue.getName());
    	}    	
    }

    protected void undoBindingsConfiguration(Configuration configuration, Channel channel) throws IOException
    {
    	List<Binding> configurationBindings = configuration.getBindings();    
    	
    	for (Binding binding : configurationBindings)
    	{
    		if (binding.getDestinationType().equalsIgnoreCase("queue"))
    		{
    			channel.queueUnbind(binding.getDestination(), 
    					binding.getSource(), binding.getRoutingKey(), 
    					new HashMap<String, Object>());
    			
    		} 
    		else if (binding.getDestinationType().equalsIgnoreCase("exchange"))
    		{
    			channel.exchangeUnbind(binding.getDestination(), 
    					binding.getSource(), binding.getRoutingKey(), 
    					new HashMap<String, Object>());    		
    		}
    	}
    }
    
    protected Configuration parseConfiguration(String file)
    {
    	ObjectMapper mapper = new ObjectMapper();
    	 
    	try 
    	{
    		return mapper.readValue(IOUtils.getResourceAsStream(file, AmqpModelRule.class), Configuration.class);
    	} 
    	catch (Exception e) 
    	{
    		throw new RuntimeException(e);
    	}
    }
    
}
