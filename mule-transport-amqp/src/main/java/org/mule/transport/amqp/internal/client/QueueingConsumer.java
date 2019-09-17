/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.amqp.internal.client;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.RpcServer.RpcConsumer;
import com.rabbitmq.utility.Utility;

public class QueueingConsumer extends DefaultConsumer implements RpcConsumer {
	
    // Marker object used to signal the queue is in shutdown mode.
    // It is only there to wake up consumers. The canonical representation
    // of shutting down is the presence of _shutdown.
    // Invariant: This is never on _queue unless _shutdown != null.
    private static final Delivery POISON = new Delivery(null, null, null);
    private final BlockingQueue<Delivery> _queue;
    // When this is non-null the queue is in shutdown mode and nextDelivery should
    // throw a shutdown signal exception.
    private volatile ShutdownSignalException _shutdown;
    private volatile ConsumerCancelledException _cancelled;

    public QueueingConsumer(Channel ch) {
        this(ch, new LinkedBlockingQueue<Delivery>());
    }

    public QueueingConsumer(Channel ch, BlockingQueue<Delivery> q) {
        super(ch);
        this._queue = q;
    }

    public Delivery nextDelivery() throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
        return handle(_queue.take());
    }
    
    public Delivery nextDelivery(long timeout) throws InterruptedException, ShutdownSignalException, ConsumerCancelledException {
        return handle(_queue.poll(timeout, TimeUnit.MILLISECONDS));
    }

    @Override
    public void handleShutdownSignal(String consumerTag,
        ShutdownSignalException sig) {
        _shutdown = sig;
        _queue.add(POISON);
    }

    @Override
    public void handleCancel(String consumerTag) throws IOException {
        _cancelled = new ConsumerCancelledException();
        _queue.add(POISON);
    }

    @Override
	public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
			throws IOException {
		checkShutdown();
		this._queue.add(new Delivery(envelope, properties, body));
	}

    /**
     * Check if we are in shutdown mode and if so throw an exception.
     */
    private void checkShutdown() {
        if (_shutdown != null)
            throw Utility.fixStackTrace(_shutdown);
    }

    /**
     * If delivery is not POISON nor null, return it.
     * <p/>
     * If delivery, _shutdown and _cancelled are all null, return null.
     * <p/>
     * If delivery is POISON re-insert POISON into the queue and
     * throw an exception if POISONed for no reason.
     * <p/>
     * Otherwise, if we are in shutdown mode or cancelled,
     * throw a corresponding exception.
     */
    private Delivery handle(Delivery delivery) {
        if (delivery == POISON ||
            delivery == null && (_shutdown != null || _cancelled != null)) {
            if (delivery == POISON) {
                _queue.add(POISON);
                if (_shutdown == null && _cancelled == null) {
                    throw new IllegalStateException(
                        "POISON in queue, but null _shutdown and null _cancelled. " +
                            "This should never happen, please report as a BUG");
                }
            }
            if (null != _shutdown)
                throw Utility.fixStackTrace(_shutdown);
            if (null != _cancelled)
                throw Utility.fixStackTrace(_cancelled);
        }
        return delivery;
    }

}
