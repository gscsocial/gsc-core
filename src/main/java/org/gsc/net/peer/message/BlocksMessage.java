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

import org.apache.commons.collections4.CollectionUtils;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Items;

public class BlocksMessage extends GSCMessage {

    private List<Block> blocks;

    public BlocksMessage(byte[] data) throws Exception {
        super(data);
        this.type = MessageTypes.BLOCKS.asByte();
        Items items = Items.parseFrom(getCodedInputStream(data));
        if (items.getType() == Items.ItemType.BLOCK) {
            blocks = items.getBlocksList();
        }
        if (isFilter() && CollectionUtils.isNotEmpty(blocks)) {
            compareBytes(data, items.toByteArray());
            for (Block block : blocks) {
                TransactionWrapper.validContractProto(block.getTransactionsList());
            }
        }
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    @Override
    public String toString() {
        return super.toString() + "size: " + (CollectionUtils.isNotEmpty(blocks) ? blocks
                .size() : 0);
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

}
