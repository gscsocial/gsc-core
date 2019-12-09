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

import org.gsc.net.peer.message.MessageTypes;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.ReasonCode;

public class DisconnectMessage extends P2pMessage {

    private Protocol.DisconnectMessage disconnectMessage;

    public DisconnectMessage(ReasonCode reasonCode) {
        this.disconnectMessage = Protocol.DisconnectMessage
                .newBuilder()
                .setReason(reasonCode)
                .build();
        this.type = MessageTypes.P2P_DISCONNECT.asByte();
        this.data = this.disconnectMessage.toByteArray();
    }

    public DisconnectMessage(byte type, byte[] rawData) throws Exception {
        super(type, rawData);
        this.disconnectMessage = Protocol.DisconnectMessage.parseFrom(this.data);
    }

    public ReasonCode getReasonCode() {
        return disconnectMessage.getReason();
    }

    public int getReason() {
        return this.disconnectMessage.getReason().getNumber();
    }

    @Override
    public String toString() {
        return new StringBuilder().append(super.toString()).append("reason: ")
                .append(this.disconnectMessage.getReason()).toString();
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }
}