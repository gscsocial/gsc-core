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

package org.gsc.runtime.event.trigger;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class BlockLogTrigger extends Trigger {

    @Getter
    @Setter
    private long blockNumber;

    @Getter
    @Setter
    private String blockHash;

    @Getter
    @Setter
    private long transactionSize;

    @Getter
    @Setter
    private long latestConfirmedBlockNumber;

    @Getter
    @Setter
    private List<String> transactionList = new ArrayList<>();

    public BlockLogTrigger() {
        setTriggerName(Trigger.BLOCK_TRIGGER_NAME);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("triggerName: ").append(getTriggerName())
                .append("timestamp: ")
                .append(timeStamp)
                .append(", blockNumber: ")
                .append(blockNumber)
                .append(", blockhash: ")
                .append(blockHash)
                .append(", transactionSize: ")
                .append(transactionSize)
                .append(", latestConfirmedBlockNumber: ")
                .append(latestConfirmedBlockNumber)
                .append(", transactionList: ")
                .append(transactionList).toString();
    }
}
