/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import java.io.Serializable;

import com.rabbitmq.client.Channel;

/**
 * Message property to encapsule the channel and avoid deserialization problems.
 * 
 * @since 3.8.2
 *
 */
public class ChannelMessageProperty implements Serializable {

	private static final long serialVersionUID = 7221650028524984122L;

	private transient Channel channel;

	public ChannelMessageProperty(Channel channel) {
		this.channel = channel;
	}

	public Channel getChannel() {
		return channel;
	}

}
