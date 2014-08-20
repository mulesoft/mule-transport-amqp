/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.domain;

public enum DeliveryMode
{
    NON_PERSISTENT(1), PERSISTENT(2);

    private final int code;

    DeliveryMode(final int code)
    {
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }
}
