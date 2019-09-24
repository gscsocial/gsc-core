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
import java.util.List;
import java.util.stream.Collectors;

import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.BlockInventory;

public class BlockInventoryMessage extends GSCMessage {

    protected BlockInventory blockInventory;

    public BlockInventoryMessage(byte[] data) throws Exception {
        super(data);
        this.type = MessageTypes.BLOCK_INVENTORY.asByte();
        this.blockInventory = Protocol.BlockInventory.parseFrom(data);
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    private BlockInventory getBlockInventory() {
        return blockInventory;
    }

    public BlockInventoryMessage(List<BlockId> blockIds, BlockInventory.Type type) {
        BlockInventory.Builder invBuilder = BlockInventory.newBuilder();
        blockIds.forEach(blockId -> {
            BlockInventory.BlockId.Builder b = BlockInventory.BlockId.newBuilder();
            b.setHash(blockId.getByteString());
            b.setNumber(blockId.getNum());
            invBuilder.addIds(b);
        });

        invBuilder.setType(type);
        blockInventory = invBuilder.build();
        this.type = MessageTypes.BLOCK_INVENTORY.asByte();
        this.data = blockInventory.toByteArray();
    }

    public List<BlockId> getBlockIds() {
        return getBlockInventory().getIdsList().stream()
                .map(blockId -> new BlockId(blockId.getHash(), blockId.getNumber()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

}
