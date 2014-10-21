/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.connector;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.DefaultMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.DefaultReplyToHandler;
import org.mule.transport.amqp.internal.connector.connection.ConnectorConnection;
import org.mule.transport.amqp.internal.endpoint.dispatcher.Dispatcher;
import org.mule.util.StringUtils;

public class ReplyToHandler extends DefaultReplyToHandler
{
    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(ReplyToHandler.class);
    private final transient AmqpConnector amqpConnector;

    public ReplyToHandler(final AmqpConnector amqpConnector)
    {
        super(amqpConnector.getMuleContext());
        this.amqpConnector = amqpConnector;
    }

    @Override
    public void processReplyTo(final MuleEvent event, final MuleMessage returnMessage, final Object replyTo)
        throws MuleException
    {
        String replyToQueueName = createTemporaryQueueIfNecessary((String) replyTo);

        OutboundEndpoint outboundEndpoint;
        if (replyToQueueName.contains("://"))
        {
            outboundEndpoint = getEndpoint(event, replyToQueueName);
        }
        else
        {
            // target the default (ie. "") exchange with a routing key equals to the
            // queue replied to
            outboundEndpoint = getEndpoint(event,
                    amqpConnector.getProtocol() + "://?routingKey=" + urlEncode(event, replyToQueueName)
                            + "&connector=" + urlEncode(event, amqpConnector.getName()));
        }

        final Dispatcher dispatcher = new Dispatcher(outboundEndpoint);

        final DefaultMuleEvent replyEvent = new DefaultMuleEvent(returnMessage, event);
        dispatcher.process(replyEvent);

        try
        {
            dispatcher.disconnect();
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to disconnect message endpoint: " + dispatcher, e);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Successfully replied to %s: %s", replyToQueueName, replyEvent));
        }
    }

    private String createTemporaryQueueIfNecessary(String replyToQueueName) throws MuleException {
        Channel channel = null;
        try
        {
            if (StringUtils.isEmpty(replyToQueueName))
            {
                channel = ((ConnectorConnection) amqpConnector.getConnection()).getChannel();
                final AMQP.Queue.DeclareOk declareOk = channel.queueDeclare();
                replyToQueueName = declareOk.getQueue();
            }
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
        finally {
            try
            {
                channel.close();
            }
            catch (Exception closeException)
            {
                // Silently ignore errors on close
            }
        }
        return replyToQueueName;
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
