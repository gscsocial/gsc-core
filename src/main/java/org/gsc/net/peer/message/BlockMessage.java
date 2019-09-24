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

import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.net.peer.p2p.Message;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper.BlockId;

public class BlockMessage extends GSCMessage {

    private BlockWrapper block;

    public BlockMessage(byte[] data) throws Exception {
        super(data);
        this.type = MessageTypes.BLOCK.asByte();
        this.block = new BlockWrapper(getCodedInputStream(data));
        if (Message.isFilter()) {
            Message.compareBytes(data, block.getInstance().toByteArray());
            TransactionWrapper.validContractProto(block.getInstance().getTransactionsList());
        }
    }

    public BlockMessage(BlockWrapper block) {
        data = block.getData();
        this.type = MessageTypes.BLOCK.asByte();
        this.block = block;
    }

    public BlockId getBlockId() {
        return getBlockWrapper().getBlockId();
    }

    public BlockWrapper getBlockWrapper() {
        return block;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public Sha256Hash getMessageId() {
        return getBlockWrapper().getBlockId();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder().append(super.toString()).append(block.getBlockId().getString())
                .append(", trx size: ").append(block.getTransactions().size()).append("\n").toString();
    }
}
