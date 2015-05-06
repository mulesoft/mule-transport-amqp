/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  https://github.com/mulesoft/mule-transport-amqp
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.connector;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.rabbitmq.client.Channel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.DefaultMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.DefaultReplyToHandler;
import org.mule.transport.amqp.internal.client.AmqpDeclarer;
import org.mule.transport.amqp.internal.endpoint.dispatcher.Dispatcher;
import org.mule.util.StringUtils;

public class ReplyToHandler extends DefaultReplyToHandler
{
    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(ReplyToHandler.class);
    private final transient AmqpConnector amqpConnector;
    private final ImmutableEndpoint endpoint;
    private final AmqpDeclarer declarator;

    public ReplyToHandler(final AmqpConnector amqpConnector, final ImmutableEndpoint endpoint)
    {
        super(amqpConnector.getMuleContext());
        this.amqpConnector = amqpConnector;
        this.endpoint = endpoint;
        declarator = new AmqpDeclarer();
    }

    @Override
    public void processReplyTo(final MuleEvent event, final MuleMessage returnMessage, final Object replyTo)
        throws MuleException
    {
        String replyToAddress;
        
        if (!(replyTo instanceof String))
        {
            throw new DefaultMuleException(new IllegalArgumentException(
                    MuleProperties.MULE_REPLY_TO_PROPERTY + " should be of type String"));
        }
        
        replyToAddress = (String) replyTo;

        OutboundEndpoint outboundEndpoint;
        if (replyToAddress.contains("://"))
        {
            outboundEndpoint = getEndpoint(event, replyToAddress);
        }
        else
        {
            // target the default (ie. "") exchange with a routing key equals to the
            // queue replied to
            outboundEndpoint = getEndpoint(event,
                    amqpConnector.getProtocol() + "://?routingKey=" + urlEncode(event, replyToAddress)
                            + "&connector=" + urlEncode(event, amqpConnector.getName()));
        }

        Dispatcher dispatcher = null;

        try
        {
            dispatcher = new Dispatcher(outboundEndpoint);
            DefaultMuleEvent replyEvent = new DefaultMuleEvent(returnMessage, event);
            dispatcher.initialise();
            dispatcher.process(replyEvent);
            
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Successfully replied to %s: %s", replyToAddress, replyEvent));
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to disconnect message endpoint: " + dispatcher, e);
        }
        finally 
        {
            if (dispatcher != null)
            {
                try 
                {
                    dispatcher.disconnect();
                    dispatcher.dispose();
                } 
                catch (Exception e) {}
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
