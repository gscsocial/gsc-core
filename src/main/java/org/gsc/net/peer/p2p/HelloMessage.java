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

import com.google.protobuf.ByteString;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.net.node.Node;
import org.gsc.utils.ByteArray;
import org.gsc.config.args.Args;
import org.gsc.net.peer.message.MessageTypes;
import org.gsc.protos.Discover.Endpoint;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.HelloMessage.Builder;

public class HelloMessage extends P2pMessage {

    private Protocol.HelloMessage helloMessage;

    public HelloMessage(byte type, byte[] rawData) throws Exception {
        super(type, rawData);
        this.helloMessage = Protocol.HelloMessage.parseFrom(rawData);
    }

    public HelloMessage(Node from, long timestamp, BlockWrapper.BlockId genesisBlockId,
                        BlockWrapper.BlockId solidBlockId, BlockWrapper.BlockId headBlockId) {

        Endpoint fromEndpoint = Endpoint.newBuilder()
                .setNodeId(ByteString.copyFrom(from.getId()))
                .setPort(from.getPort())
                .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
                .build();

        Protocol.HelloMessage.BlockId gBlockId = Protocol.HelloMessage.BlockId.newBuilder()
                .setHash(genesisBlockId.getByteString())
                .setNumber(genesisBlockId.getNum())
                .build();

        Protocol.HelloMessage.BlockId sBlockId = Protocol.HelloMessage.BlockId.newBuilder()
                .setHash(solidBlockId.getByteString())
                .setNumber(solidBlockId.getNum())
                .build();

        Protocol.HelloMessage.BlockId hBlockId = Protocol.HelloMessage.BlockId.newBuilder()
                .setHash(headBlockId.getByteString())
                .setNumber(headBlockId.getNum())
                .build();

        Builder builder = Protocol.HelloMessage.newBuilder();

        builder.setFrom(fromEndpoint);
        builder.setVersion(Args.getInstance().getNodeP2pVersion());
        builder.setTimestamp(timestamp);
        builder.setGenesisBlockId(gBlockId);
        builder.setSolidBlockId(sBlockId);
        builder.setHeadBlockId(hBlockId);

        this.helloMessage = builder.build();
        this.type = MessageTypes.P2P_HELLO.asByte();
        this.data = this.helloMessage.toByteArray();
    }

    public int getVersion() {
        return this.helloMessage.getVersion();
    }

    public long getTimestamp() {
        return this.helloMessage.getTimestamp();
    }

    public Node getFrom() {
        Endpoint from = this.helloMessage.getFrom();
        return new Node(from.getNodeId().toByteArray(),
                ByteArray.toStr(from.getAddress().toByteArray()), from.getPort());
    }

    public BlockWrapper.BlockId getGenesisBlockId() {
        return new BlockWrapper.BlockId(this.helloMessage.getGenesisBlockId().getHash(),
                this.helloMessage.getGenesisBlockId().getNumber());
    }

    public BlockWrapper.BlockId getHeadBlockId() {
        return new BlockWrapper.BlockId(this.helloMessage.getHeadBlockId().getHash(),
                this.helloMessage.getHeadBlockId().getNumber());
    }

    public BlockWrapper.BlockId getSolidBlockId() {
        return new BlockWrapper.BlockId(this.helloMessage.getSolidBlockId().getHash(),
                this.helloMessage.getSolidBlockId().getNumber());
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(super.toString()).append(helloMessage.toString()).toString();
    }

}