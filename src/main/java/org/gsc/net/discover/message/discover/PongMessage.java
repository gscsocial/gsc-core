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

package org.gsc.net.discover.message.discover;

import static org.gsc.net.discover.message.UdpMessageTypeEnum.DISCOVER_PONG;

import com.google.protobuf.ByteString;
import org.gsc.net.discover.message.Message;
import org.gsc.net.node.Node;
import org.gsc.utils.ByteArray;
import org.gsc.config.args.Args;
import org.gsc.protos.Discover;
import org.gsc.protos.Discover.Endpoint;

public class PongMessage extends Message {

    private Discover.PongMessage pongMessage;

    public PongMessage(byte[] data) throws Exception {
        super(DISCOVER_PONG, data);
        this.pongMessage = Discover.PongMessage.parseFrom(data);
    }

    public PongMessage(Node from, long sequence) {
        super(DISCOVER_PONG, null);
        Endpoint toEndpoint = Endpoint.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
                .setPort(from.getPort())
                .setNodeId(ByteString.copyFrom(from.getId()))
                .build();
        this.pongMessage = Discover.PongMessage.newBuilder()
                .setFrom(toEndpoint)
                .setEcho(Args.getInstance().getNodeP2pVersion())
                .setTimestamp(sequence)
                .build();
        this.data = this.pongMessage.toByteArray();
    }

    public int getVersion() {
        return this.pongMessage.getEcho();
    }

    @Override
    public long getTimestamp() {
        return this.pongMessage.getTimestamp();
    }

    @Override
    public Node getFrom() {
        return Message.getNode(pongMessage.getFrom());
    }

    @Override
    public String toString() {
        return "[pongMessage: " + pongMessage;
    }
}
