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

package org.gsc.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.gsc.net.peer.p2p.Message;

public class SafeMessageMap {

    protected final Map<Sha256Hash, Message> storage;

    protected ReadWriteLock rwLock = new ReentrantReadWriteLock();
    protected ALock readLock = new ALock(rwLock.readLock());
    protected ALock writeLock = new ALock(rwLock.writeLock());

    public SafeMessageMap() {
        this.storage = new HashMap<>();
    }

    public void put(Sha256Hash msgId, Message msg) {
        if (msg == null) {
            delete(msgId);
        } else {
            try (ALock l = writeLock.lock()) {
                storage.put(msgId, msg);
            }
        }
    }

    public void put(Message msg) {
        put(Sha256Hash.of(msg.getData()), msg);
    }

    public void delete(Sha256Hash msgId) {
        try (ALock l = writeLock.lock()) {
            storage.remove(msgId);
        }
    }

    public Message get(Sha256Hash msgId) {
        try (ALock l = readLock.lock()) {
            return storage.get(msgId);
        }
    }


}
