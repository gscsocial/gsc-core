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

package org.gsc.net.peer.message;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.ChainInventory;

public class ChainInventoryMessage extends GSCMessage {

    protected ChainInventory chainInventory;

    public ChainInventoryMessage(byte[] data) throws Exception {
        super(data);
        this.type = MessageTypes.BLOCK_CHAIN_INVENTORY.asByte();
        chainInventory = Protocol.ChainInventory.parseFrom(data);
    }

    public ChainInventoryMessage(List<BlockId> blockIds, Long remainNum) {
        ChainInventory.Builder invBuilder = ChainInventory.newBuilder();
        blockIds.forEach(blockId -> {
            ChainInventory.BlockId.Builder b = ChainInventory.BlockId.newBuilder();
            b.setHash(blockId.getByteString());
            b.setNumber(blockId.getNum());
            invBuilder.addIds(b);
        });

        invBuilder.setRemainNum(remainNum);
        chainInventory = invBuilder.build();
        this.type = MessageTypes.BLOCK_CHAIN_INVENTORY.asByte();
        this.data = chainInventory.toByteArray();
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    private ChainInventory getChainInventory() {
        return chainInventory;
    }

    public List<BlockId> getBlockIds() {

        try {
            return getChainInventory().getIdsList().stream()
                    .map(blockId -> new BlockId(blockId.getHash(), blockId.getNumber()))
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            logger.info("breakPoint");
        }
        return null;
    }

    public Long getRemainNum() {
        return getChainInventory().getRemainNum();
    }

    @Override
    public String toString() {
        Deque<BlockId> blockIdWeGet = new LinkedList<>(getBlockIds());
        StringBuilder sb = new StringBuilder(super.toString());
        int size = blockIdWeGet.size();
        sb.append("size: ").append(size);
        if (size >= 1) {
            sb.append(", first blockId: ").append(blockIdWeGet.peek().getString());
            if (size > 1) {
                sb.append(", end blockId: ").append(blockIdWeGet.peekLast().getString());
            }
        }
        return sb.toString();
    }
}
