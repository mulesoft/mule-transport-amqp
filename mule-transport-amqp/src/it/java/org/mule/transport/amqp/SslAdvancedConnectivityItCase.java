/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import org.junit.Test;
import org.mule.transport.amqp.harness.AbstractItSslCase;

public class SslAdvancedConnectivityItCase extends AbstractItSslCase
{
    @Override
    protected String getConfigResources()
    {
        return "ssl/ssl-advanced-tests-config.xml";
    }

    @Test
    public void sslDispatchingAndReceiving() throws Exception
    {
        amqpTestClient.dispatchAndReceiveAMQPS(nameFactory.getVmName("sslDispatcher"), muleContext, 
        	getFunctionalTestComponent("sslReceiver"), getTestTimeoutSecs());
    }
    
    @Test
    public void sslDispatchingAndReceivingWithoutKeystore() throws Exception
    {
    	amqpTestClient.dispatchAndReceiveAMQPS(nameFactory.getVmName("sslDispatcherWithoutKeystore"), muleContext, 
        	getFunctionalTestComponent("sslReceiverWithoutKeystore"), getTestTimeoutSecs());
    }
}
