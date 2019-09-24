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

package org.gsc.runtime.event.wrapper;

import lombok.Getter;
import lombok.Setter;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.runtime.event.EventPluginLoader;
import org.gsc.runtime.event.trigger.BlockLogTrigger;

public class BlockLogTriggerWrapper extends TriggerWrapper {

    @Getter
    @Setter
    BlockLogTrigger blockLogTrigger;

    public BlockLogTriggerWrapper(BlockWrapper block) {
        blockLogTrigger = new BlockLogTrigger();
        blockLogTrigger.setBlockHash(block.getBlockId().toString());
        blockLogTrigger.setTimeStamp(block.getTimeStamp());
        blockLogTrigger.setBlockNumber(block.getNum());
        blockLogTrigger.setTransactionSize(block.getTransactions().size());
        block.getTransactions().forEach(trx ->
                blockLogTrigger.getTransactionList().add(trx.getTransactionId().toString())
        );
    }

    public void setLatestConfirmedBlockNumber(long latestConfirmedBlockNumber) {
        blockLogTrigger.setLatestConfirmedBlockNumber(latestConfirmedBlockNumber);
    }

    @Override
    public void processTrigger() {
        EventPluginLoader.getInstance().postBlockTrigger(blockLogTrigger);
    }
}
