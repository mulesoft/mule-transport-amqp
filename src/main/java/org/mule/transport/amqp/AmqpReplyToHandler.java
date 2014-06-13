/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.DefaultReplyToHandler;
import org.mule.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AmqpReplyToHandler extends DefaultReplyToHandler
{
    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(AmqpReplyToHandler.class);
    private final transient AmqpConnector amqpConnector;

    public AmqpReplyToHandler(final AmqpConnector amqpConnector)
    {
        super(amqpConnector.getMuleContext());
        this.amqpConnector = amqpConnector;
    }

    @Override
    public void processReplyTo(final MuleEvent event, final MuleMessage returnMessage, final Object replyTo)
        throws MuleException
    {
        final String replyToQueueName = (String) replyTo;
        if (StringUtils.isBlank(replyToQueueName))
        {
            return;
        }

        // target the default (ie. "") exchange with a routing key equals to the
        // queue replied to
        final OutboundEndpoint outboundEndpoint = getEndpoint(event,
            amqpConnector.getProtocol() + "://?routingKey=" + urlEncode(event, replyToQueueName)
                            + "&connector=" + urlEncode(event, amqpConnector.getName()));

        final AmqpMessageDispatcher dispatcher = new AmqpMessageDispatcher(outboundEndpoint);

        AmqpMessage amqpMessage = (AmqpMessage) returnMessage.getPayload();
        // don't start a response loop
        amqpMessage.setReplyTo(null);

        final DefaultMuleEvent replyEvent = new DefaultMuleEvent(returnMessage, event);

        try
        {
            dispatcher.process(replyEvent);
            if (LOG.isDebugEnabled())
            {
                LOG.debug(String.format("Successfully replied to %s: %s", replyToQueueName, replyEvent));
            }
        }
        catch (Exception e)
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug(String.format("Could not reply to %s", replyToQueueName), e);
            }
        }
        finally {
            try
            {
                dispatcher.disconnect();
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to disconnect message dispatcher: " + dispatcher, e);
            }
        }

    }

    protected String urlEncode(final MuleEvent event, final String stringToEncode) throws MessagingException
    {
        try
        {
            return URLEncoder.encode(stringToEncode, event.getEncoding());
        }
        catch (final UnsupportedEncodingException uee)
        {
            throw new MessagingException(MessageFactory.createStaticMessage(String.format(
                "Impossible to url encode: %s", stringToEncode)), event, uee);
        }
    }
}
