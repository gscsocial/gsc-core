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

import static org.gsc.net.discover.message.UdpMessageTypeEnum.DISCOVER_FIND_NODE;

import com.google.protobuf.ByteString;
import org.gsc.net.discover.message.Message;
import org.gsc.net.node.Node;
import org.gsc.utils.ByteArray;
import org.gsc.protos.Discover;
import org.gsc.protos.Discover.Endpoint;
import org.gsc.protos.Discover.FindNeighbours;

public class FindNodeMessage extends Message {

    private Discover.FindNeighbours findNeighbours;

    public FindNodeMessage(byte[] data) throws Exception {
        super(DISCOVER_FIND_NODE, data);
        this.findNeighbours = Discover.FindNeighbours.parseFrom(data);
    }

    public FindNodeMessage(Node from, byte[] targetId) {
        super(DISCOVER_FIND_NODE, null);
        Endpoint fromEndpoint = Endpoint.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
                .setPort(from.getPort())
                .setNodeId(ByteString.copyFrom(from.getId()))
                .build();
        this.findNeighbours = FindNeighbours.newBuilder()
                .setFrom(fromEndpoint)
                .setTargetId(ByteString.copyFrom(targetId))
                .setTimestamp(System.currentTimeMillis())
                .build();
        this.data = this.findNeighbours.toByteArray();
    }

    public byte[] getTargetId() {
        return this.findNeighbours.getTargetId().toByteArray();
    }

    @Override
    public long getTimestamp() {
        return this.findNeighbours.getTimestamp();
    }

    @Override
    public Node getFrom() {
        return Message.getNode(findNeighbours.getFrom());
    }

    @Override
    public String toString() {
        return "[findNeighbours: " + findNeighbours;
    }
}
