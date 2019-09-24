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

package org.gsc.net.server;

import org.gsc.net.peer.p2p.Message;

public class MessageRoundtrip {

    private final Message msg;
    private long time = 0;
    private long retryTimes = 0;

    public MessageRoundtrip(Message msg) {
        this.msg = msg;
        saveTime();
    }

    public long getRetryTimes() {
        return retryTimes;
    }

    public void incRetryTimes() {
        ++retryTimes;
    }

    public void saveTime() {
        time = System.currentTimeMillis();
    }

    public long getTime() {
        return time;
    }

    public boolean hasToRetry() {
        return 20000 < System.currentTimeMillis() - time;
    }

    public Message getMsg() {
        return msg;
    }
}
