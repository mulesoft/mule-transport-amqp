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

import org.mule.util.ArrayUtils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class AmqpConstants
{
    public enum DeliveryMode
    {
        NON_PERSISTENT(1), PERSISTENT(2);

        private final int code;

        private DeliveryMode(final int code)
        {
            this.code = code;
        }

        public int getCode()
        {
            return code;
        }
    }

    public enum AckMode
    {
        AMQP_AUTO(true), MULE_AUTO(false), MANUAL(false);

        private final boolean autoAck;

        private AckMode(final boolean autoAck)
        {
            this.autoAck = autoAck;
        }

        public boolean isAutoAck()
        {
            return autoAck;
        }
    }

    // message properties names are consistent with AMQP spec
    // (cluster-id is deprecated and not supported here)
    public static final String APP_ID = "app-id";
    public static final String CONTENT_ENCODING = "content-encoding";
    public static final String CONTENT_TYPE = "content-type";
    public static final String CORRELATION_ID = "correlation-id";
    public static final String DELIVERY_MODE = "delivery_mode";
    public static final String DELIVERY_TAG = "delivery-tag";
    public static final String EXCHANGE = "exchange";
    public static final String EXPIRATION = "expiration";
    public static final String MESSAGE_ID = "message-id";
    public static final String PRIORITY = "priority";
    public static final String REDELIVER = "redelivered";
    public static final String REPLY_TO = "reply-to";
    public static final String ROUTING_KEY = "routing-key";
    public static final String TIMESTAMP = "timestamp";
    public static final String TYPE = "type";
    public static final String USER_ID = "user-id";
    public static final String NEXT_PUBLISH_SEQ_NO = "nextPublishSequenceNo";

    public static final String ALL_USER_HEADERS = AmqpConnector.AMQP + ".headers";

    private static final String[] AMQP_ENVELOPE_PROPERTY_NAMES_ARRAY = new String[]{DELIVERY_TAG, EXCHANGE,
        REDELIVER, ROUTING_KEY};

    private static final String[] AMQP_BASIC_PROPERTY_NAMES_ARRAY = new String[]{APP_ID, CONTENT_ENCODING,
        CONTENT_TYPE, CORRELATION_ID, DELIVERY_MODE, EXPIRATION, MESSAGE_ID, PRIORITY, REPLY_TO, TIMESTAMP,
        TYPE, USER_ID};

    public static final Set<String> AMQP_ENVELOPE_PROPERTY_NAMES = Collections.unmodifiableSet(new HashSet<String>(
        Arrays.asList(AMQP_ENVELOPE_PROPERTY_NAMES_ARRAY)));

    public static final Set<String> AMQP_BASIC_PROPERTY_NAMES = Collections.unmodifiableSet(new HashSet<String>(
        Arrays.asList(AMQP_BASIC_PROPERTY_NAMES_ARRAY)));

    // technical properties not intended to be messed with directly
    public static final String CONSUMER_TAG = "consumer-tag";
    public static final String CHANNEL = AmqpConnector.AMQP + ".channel";
    public static final String AMQP_DELIVERY_TAG = AmqpConnector.AMQP + ".delivery-tag";
    public static final String RETURN_LISTENER = AmqpConnector.AMQP + ".return.listener";

    public static final String RETURN_CONTEXT_PREFIX = AmqpConnector.AMQP + ".return.";
    public static final String RETURN_REPLY_CODE = RETURN_CONTEXT_PREFIX + "reply-code";
    public static final String RETURN_REPLY_TEXT = RETURN_CONTEXT_PREFIX + "reply-text";
    public static final String RETURN_EXCHANGE = RETURN_CONTEXT_PREFIX + EXCHANGE;
    public static final String RETURN_ROUTING_KEY = RETURN_CONTEXT_PREFIX + ROUTING_KEY;

    private static final String[] AMQP_TRANSPORT_TECHNICAL_PROPERTY_NAMES_ARRAY = new String[]{ALL_USER_HEADERS,
        CONSUMER_TAG, CHANNEL, AMQP_DELIVERY_TAG, RETURN_LISTENER, RETURN_REPLY_CODE, RETURN_REPLY_TEXT,
        RETURN_EXCHANGE, RETURN_ROUTING_KEY};

    public static final Set<String> AMQP_TRANSPORT_TECHNICAL_PROPERTY_NAMES = Collections.unmodifiableSet(new HashSet<String>(
        Arrays.asList(AMQP_TRANSPORT_TECHNICAL_PROPERTY_NAMES_ARRAY)));

    public static final Set<String> AMQP_ALL_PROPERTY_NAMES = Collections.unmodifiableSet(new HashSet<String>(
        Arrays.asList((String[]) ArrayUtils.addAll(
            ArrayUtils.addAll(AMQP_ENVELOPE_PROPERTY_NAMES_ARRAY, AMQP_BASIC_PROPERTY_NAMES_ARRAY),
            AMQP_TRANSPORT_TECHNICAL_PROPERTY_NAMES_ARRAY))));
    public static final Charset LONG_STRING_CHARSET = Charset.forName("UTF-8");

    public static final String DEFAULT_EXCHANGE_ALIAS = "AMQP.DEFAULT.EXCHANGE";

    public static void main(final String[] args)
    {
        // generates the properties HTML tables used in the documentation
        final StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("<tr><th>Basic Properties</th><th>Envelope Properties</th><th>Technical Properties</th></tr>\n");
        for (int i = 0; i < AMQP_BASIC_PROPERTY_NAMES_ARRAY.length; i++)
        {
            sb.append("<tr>");
            sb.append("<td>").append(AMQP_BASIC_PROPERTY_NAMES_ARRAY[i]).append("</td>");
            sb.append("<td>")
                .append(
                    i < AMQP_ENVELOPE_PROPERTY_NAMES_ARRAY.length
                                                                 ? AMQP_ENVELOPE_PROPERTY_NAMES_ARRAY[i]
                                                                 : "")
                .append("</td>");
            sb.append("<td>")
                .append(
                    i < AMQP_TRANSPORT_TECHNICAL_PROPERTY_NAMES_ARRAY.length
                                                                            ? AMQP_TRANSPORT_TECHNICAL_PROPERTY_NAMES_ARRAY[i]
                                                                            : "")
                .append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");
        System.out.println(sb.toString());
    }
}
