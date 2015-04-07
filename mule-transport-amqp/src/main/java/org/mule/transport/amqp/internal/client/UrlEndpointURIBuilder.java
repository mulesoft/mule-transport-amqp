/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import java.net.URI;
import java.util.Properties;

import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.endpoint.AbstractEndpointURIBuilder;
import org.mule.util.StringUtils;

/**
 * This builder takes care of the cases where the {@link URI} has an empty host component but has
 * the authority component filled.
 */
public class UrlEndpointURIBuilder extends AbstractEndpointURIBuilder
{
    @Override
    protected void setEndpoint(final URI uri, final Properties props) throws MalformedEndpointException
    {
        address = "";

        if (uri.getAuthority() != null)
        {
            address = uri.getScheme() + "://" + uri.getAuthority();
        }
        else if (uri.getHost() != null)
        {
            // set the endpointUri to be a proper url if host and port are set
            address = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1)
            {
                address += ":" + uri.getPort();
            }
        }

        if (StringUtils.isNotBlank(uri.getRawPath()))
        {
            address += uri.getRawPath();
        }

        if (StringUtils.isNotBlank(uri.getRawQuery()))
        {
            address += "?" + uri.getRawQuery();
        }
    }
}
