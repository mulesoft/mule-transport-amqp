/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp;

import org.junit.Test;

import java.io.IOException;

public class SslAdvancedConnectivityITCase extends AbstractSslConnectivityITCase
{
    public SslAdvancedConnectivityITCase() throws IOException
    {
        super();
    }

    @Override
    protected String getConfigResources()
    {
        return "ssl-advanced-tests-config.xml";
    }

    @Test
    public void sslDispatchingAndReceivingWithoutKeystore() throws Exception
    {
        dispatchAndReceiveAMQPS("sslReceiverWithoutKeystore", "vm://sslDispatcherWithoutKeystore.in");
    }
}
