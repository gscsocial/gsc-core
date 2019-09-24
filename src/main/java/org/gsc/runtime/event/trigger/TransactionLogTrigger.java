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

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class TransactionLogTrigger extends Trigger {

    @Override
    public void setTimeStamp(long ts) {
        super.timeStamp = ts;
    }

    @Getter
    @Setter
    private String transactionId;

    @Getter
    @Setter
    private String blockHash;

    @Getter
    @Setter
    private long blockNumber = -1;

    @Getter
    @Setter
    private long cpuUsage;

    @Getter
    @Setter
    private long cpuFee;

    @Getter
    @Setter
    private long originCpuUsage;

    @Getter
    @Setter
    private long cpuUsageTotal;

    @Getter
    @Setter
    private long netUsage;

    @Getter
    @Setter
    private long netFee;

    //contract
    @Getter
    @Setter
    private String result;

    @Getter
    @Setter
    private String contractAddress;

    @Getter
    @Setter
    private String contractType;

    @Getter
    @Setter
    private long feeLimit;

    @Getter
    @Setter
    private long contractCallValue;

    @Getter
    @Setter
    private String contractResult;

    // transfer contract
    @Getter
    @Setter
    private String fromAddress;

    @Getter
    @Setter
    private String toAddress;

    @Getter
    @Setter
    private String assetName;

    @Getter
    @Setter
    private long assetAmount;

    @Getter
    @Setter
    private long latestConfirmedBlockNumber;

    //internal transaction
    @Getter
    @Setter
    private List<InternalTransactionPojo> internalTrananctionList;

    public TransactionLogTrigger() {
        setTriggerName(TRANSACTION_TRIGGER_NAME);
    }
}
