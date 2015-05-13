/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.harness;

import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

import org.apache.commons.lang.BooleanUtils;
import org.junit.Before;

public abstract class AbstractItSslCase extends AbstractItCase 
{
	@Before
    public void ensureAmqpsTestsMustRun()
    {
        assumeThat(BooleanUtils.toBoolean(System.getProperty("runAmqpsTests")), is(true));
    }
	
}
