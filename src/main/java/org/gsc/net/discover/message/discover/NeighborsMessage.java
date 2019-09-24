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

import static org.gsc.net.discover.message.UdpMessageTypeEnum.DISCOVER_NEIGHBORS;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;

import org.gsc.net.discover.message.Message;
import org.gsc.net.node.Node;
import org.gsc.utils.ByteArray;
import org.gsc.protos.Discover;
import org.gsc.protos.Discover.Endpoint;
import org.gsc.protos.Discover.Neighbours;
import org.gsc.protos.Discover.Neighbours.Builder;

public class NeighborsMessage extends Message {

    private Discover.Neighbours neighbours;

    public NeighborsMessage(byte[] data) throws Exception {
        super(DISCOVER_NEIGHBORS, data);
        this.neighbours = Discover.Neighbours.parseFrom(data);
    }

    public NeighborsMessage(Node from, List<Node> neighbours, long sequence) {
        super(DISCOVER_NEIGHBORS, null);
        Builder builder = Neighbours.newBuilder()
                .setTimestamp(sequence);

        neighbours.forEach(neighbour -> {
            Endpoint endpoint = Endpoint.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromString(neighbour.getHost())))
                    .setPort(neighbour.getPort())
                    .setNodeId(ByteString.copyFrom(neighbour.getId()))
                    .build();

            builder.addNeighbours(endpoint);
        });

        Endpoint fromEndpoint = Endpoint.newBuilder()
                .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
                .setPort(from.getPort())
                .setNodeId(ByteString.copyFrom(from.getId()))
                .build();

        builder.setFrom(fromEndpoint);

        this.neighbours = builder.build();

        this.data = this.neighbours.toByteArray();
    }

    public List<Node> getNodes() {
        List<Node> nodes = new ArrayList<>();
        neighbours.getNeighboursList().forEach(neighbour -> nodes.add(
                new Node(neighbour.getNodeId().toByteArray(),
                        ByteArray.toStr(neighbour.getAddress().toByteArray()),
                        neighbour.getPort())));
        return nodes;
    }

    @Override
    public long getTimestamp() {
        return this.neighbours.getTimestamp();
    }

    @Override
    public Node getFrom() {
        return Message.getNode(neighbours.getFrom());
    }

    @Override
    public String toString() {
        return "[neighbours: " + neighbours;
    }

}
