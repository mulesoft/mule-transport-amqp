/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.harness;

import org.junit.After;
import org.junit.Before;
import org.mule.tck.junit4.FunctionalTestCase;

import com.rabbitmq.client.Channel;

public abstract class AbstractItCase extends FunctionalTestCase 
{
	protected NameFactory nameFactory;

	protected TestConnectionManager testConnectionManager;
	
	protected VmTestClient vmTestClient;
	
	protected AmqpTestClient amqpTestClient;
	
	protected Channel channel;
	
	public AbstractItCase() 
	{
		super();
        setStartContext(false);
	}
	
	@Before
    public void prepareAbstractITCase() throws Exception
    {
		nameFactory = new NameFactory();
		testConnectionManager = new TestConnectionManager();
		channel = testConnectionManager.getChannel();
    	amqpTestClient = new AmqpTestClient(channel);
    	vmTestClient = new VmTestClient(muleContext, amqpTestClient);
    	if (!muleContext.getLifecycleManager().getState().isStarted())
    	{
    		muleContext.start();
    	}
    }
	
	@After
    public void tearDownAbstractITCase() throws Exception
    {
		testConnectionManager.disposeChannel(channel);
    }
}
