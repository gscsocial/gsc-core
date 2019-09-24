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

import java.util.List;

import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.protos.Protocol.BlockInventory.Type;

public class SyncBlockChainMessage extends BlockInventoryMessage {

    public SyncBlockChainMessage(byte[] packed) throws Exception {
        super(packed);
        this.type = MessageTypes.SYNC_BLOCK_CHAIN.asByte();
    }

    public SyncBlockChainMessage(List<BlockId> blockIds) {
        super(blockIds, Type.SYNC);
        this.type = MessageTypes.SYNC_BLOCK_CHAIN.asByte();
    }

    @Override
    public String toString() {
        List<BlockId> blockIdList = getBlockIds();
        StringBuilder sb = new StringBuilder();
        int size = blockIdList.size();
        sb.append(super.toString()).append("size: ").append(size);
        if (size >= 1) {
            sb.append(", start block: " + blockIdList.get(0).getString());
            if (size > 1) {
                sb.append(", end block " + blockIdList.get(blockIdList.size() - 1).getString());
            }
        }
        return sb.toString();
    }

    @Override
    public Class<?> getAnswerMessage() {
        return ChainInventoryMessage.class;
    }
}
