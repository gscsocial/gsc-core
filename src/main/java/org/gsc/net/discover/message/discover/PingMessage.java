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

import com.google.protobuf.ByteString;
import org.gsc.net.discover.message.Message;
import org.gsc.net.discover.message.UdpMessageTypeEnum;
import org.gsc.net.node.Node;
import org.gsc.utils.ByteArray;
import org.gsc.config.args.Args;
import org.gsc.protos.Discover;
import org.gsc.protos.Discover.Endpoint;

public class PingMessage extends Message {

    private Discover.PingMessage pingMessage;

    public PingMessage(byte[] data) throws Exception {
        super(UdpMessageTypeEnum.DISCOVER_PING, data);
        this.pingMessage = Discover.PingMessage.parseFrom(data);
    }

    public PingMessage(Node from, Node to) {
        super(UdpMessageTypeEnum.DISCOVER_PING, null);
        Endpoint fromEndpoint = Endpoint.newBuilder()
                .setNodeId(ByteString.copyFrom(from.getId()))
                .setPort(from.getPort())
                .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
                .build();
        Endpoint toEndpoint = Endpoint.newBuilder()
                .setNodeId(ByteString.copyFrom(to.getId()))
                .setPort(to.getPort())
                .setAddress(ByteString.copyFrom(ByteArray.fromString(to.getHost())))
                .build();
        this.pingMessage = Discover.PingMessage.newBuilder()
                .setVersion(Args.getInstance().getNodeP2pVersion())
                .setFrom(fromEndpoint)
                .setTo(toEndpoint)
                .setTimestamp(System.currentTimeMillis())
                .build();
        this.data = this.pingMessage.toByteArray();
    }

    public int getVersion() {
        return this.pingMessage.getVersion();
    }

    public Node getTo() {
        Endpoint to = this.pingMessage.getTo();
        Node node = new Node(to.getNodeId().toByteArray(),
                ByteArray.toStr(to.getAddress().toByteArray()), to.getPort());
        return node;
    }

    @Override
    public long getTimestamp() {
        return this.pingMessage.getTimestamp();
    }

    @Override
    public Node getFrom() {
        return Message.getNode(pingMessage.getFrom());
    }

    @Override
    public String toString() {
        return "[pingMessage: " + pingMessage;
    }

}
