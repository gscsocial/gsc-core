/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.runtime.event.nativequeue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Objects;

public class NativeMessageQueue {
    private ZContext context = null;
    private ZMQ.Socket publisher = null;
    private static NativeMessageQueue instance;
    private static final int DEFAULT_BIND_PORT = 5555;
    private static final int DEFAULT_QUEUE_LENGTH = 1000;

    public static NativeMessageQueue getInstance() {
        if (Objects.isNull(instance)) {
            synchronized (NativeMessageQueue.class) {
                if (Objects.isNull(instance)) {
                    instance = new NativeMessageQueue();
                }
            }
        }
        return instance;
    }

    public boolean start(int bindPort, int sendQueueLength) {
        context = new ZContext();
        publisher = context.createSocket(SocketType.PUB);

        if (Objects.isNull(publisher)) {
            return false;
        }

        if (bindPort == 0 || bindPort < 0) {
            bindPort = DEFAULT_BIND_PORT;
        }

        if (sendQueueLength < 0) {
            sendQueueLength = DEFAULT_QUEUE_LENGTH;
        }

        context.setSndHWM(sendQueueLength);

        String bindAddress = String.format("tcp://*:%d", bindPort);
        return publisher.bind(bindAddress);
    }

    public void stop() {
        if (Objects.nonNull(publisher)) {
            publisher.close();
        }

        if (Objects.nonNull(context)) {
            context.close();
        }
    }

    public void publishTrigger(String data, String topic) {
        if (Objects.isNull(publisher) || Objects.isNull(context.isClosed()) || context.isClosed()) {
            return;
        }

        publisher.sendMore(topic);
        publisher.send(data);
    }
}
