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

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.P2pException.TypeEnum;
import org.gsc.net.peer.message.MessageTypes;

public class P2pMessageFactory extends MessageFactory {

    @Override
    public P2pMessage create(byte[] data) throws Exception {
        if (data.length <= 1) {
            throw new P2pException(TypeEnum.MESSAGE_WITH_WRONG_LENGTH,
                    "messageType=" + (data.length == 1 ? data[0] : "unknow"));
        }
        try {
            byte type = data[0];
            byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
            return create(type, rawData);
        } catch (Exception e) {
            if (e instanceof P2pException) {
                throw e;
            } else {
                throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED,
                        "type=" + data[0] + ", len=" + data.length);
            }
        }
    }

    private P2pMessage create(byte type, byte[] rawData) throws Exception {
        MessageTypes messageType = MessageTypes.fromByte(type);
        if (messageType == null) {
            throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
                    "type=" + type + ", len=" + rawData.length);
        }
        switch (messageType) {
            case P2P_HELLO:
                return new HelloMessage(type, rawData);
            case P2P_DISCONNECT:
                return new DisconnectMessage(type, rawData);
            case P2P_PING:
                return new PingMessage(type, rawData);
            case P2P_PONG:
                return new PongMessage(type, rawData);
            default:
                throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, messageType.toString());
        }
    }
}
