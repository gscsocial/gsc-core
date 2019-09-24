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

package org.gsc.net.peer.p2p;

import org.spongycastle.util.encoders.Hex;
import org.gsc.net.peer.message.MessageTypes;

public class PingMessage extends P2pMessage {

    private static final byte[] FIXED_PAYLOAD = Hex.decode("C0");

    public PingMessage() {
        this.type = MessageTypes.P2P_PING.asByte();
        this.data = FIXED_PAYLOAD;
    }

    public PingMessage(byte type, byte[] rawData) {
        super(type, rawData);
    }

    @Override
    public byte[] getData() {
        return FIXED_PAYLOAD;
    }


    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public Class<?> getAnswerMessage() {
        return PongMessage.class;
    }

    @Override
    public MessageTypes getType() {
        return MessageTypes.fromByte(this.type);
    }
}