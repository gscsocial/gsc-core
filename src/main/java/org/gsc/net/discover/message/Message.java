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

package org.gsc.net.discover.message;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.net.discover.message.backup.KeepAliveMessage;
import org.gsc.net.discover.message.discover.FindNodeMessage;
import org.gsc.net.discover.message.discover.NeighborsMessage;
import org.gsc.net.discover.message.discover.PongMessage;
import org.gsc.net.discover.message.discover.PingMessage;
import org.gsc.net.node.Node;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.exception.P2pException;
import org.gsc.protos.Discover.Endpoint;

public abstract class Message {

    protected UdpMessageTypeEnum type;
    protected byte[] data;

    public Message(UdpMessageTypeEnum type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public UdpMessageTypeEnum getType() {
        return this.type;
    }

    public byte[] getData() {
        return this.data;
    }

    public byte[] getSendData() {
        return ArrayUtils.add(this.data, 0, type.getType());
    }

    public Sha256Hash getMessageId() {
        return Sha256Hash.of(getData());
    }

    public abstract Node getFrom();

    public abstract long getTimestamp();

    @Override
    public String toString() {
        return "[Message Type: " + getType() + ", len: " + (data == null ? 0 : data.length) + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return getMessageId().hashCode();
    }

    public static Node getNode(Endpoint endpoint) {
        Node node = new Node(endpoint.getNodeId().toByteArray(),
                ByteArray.toStr(endpoint.getAddress().toByteArray()), endpoint.getPort());
        return node;
    }

    public static Message parse(byte[] encode) throws Exception {
        byte type = encode[0];
        byte[] data = ArrayUtils.subarray(encode, 1, encode.length);
        switch (UdpMessageTypeEnum.fromByte(type)) {
            case DISCOVER_PING:
                return new PingMessage(data);
            case DISCOVER_PONG:
                return new PongMessage(data);
            case DISCOVER_FIND_NODE:
                return new FindNodeMessage(data);
            case DISCOVER_NEIGHBORS:
                return new NeighborsMessage(data);
            case BACKUP_KEEP_ALIVE:
                return new KeepAliveMessage(data);
            default:
                throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type=" + type);
        }
    }
}
