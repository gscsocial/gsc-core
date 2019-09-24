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

import lombok.Getter;
import lombok.Setter;
import org.gsc.runtime.event.wrapper.RawData;
import org.gsc.runtime.vm.LogInfo;
import org.gsc.protos.Protocol.SmartContract.ABI;

public class ContractTrigger extends Trigger {

    /**
     * unique id of this trigger. $tx_id + "_" + $index
     */
    @Getter
    @Setter
    private String uniqueId;

    /**
     * id of the transaction which produce this event.
     */
    @Getter
    @Setter
    private String transactionId;

    /**
     * address of the contract triggered by the callerAddress.
     */
    @Getter
    @Setter
    private String contractAddress;

    /**
     * caller of the transaction which produce this event.
     */
    @Getter
    @Setter
    private String callerAddress;

    /**
     * origin address of the contract which produce this event.
     */
    @Getter
    @Setter
    private String originAddress;

    /**
     * caller address of the contract which produce this event.
     */
    @Getter
    @Setter
    private String creatorAddress;

    /**
     * block number of the transaction
     */
    @Getter
    @Setter
    private Long blockNumber;

    /**
     * true if the transaction has been revoked
     */
    @Getter
    @Setter
    private boolean removed;

    @Getter
    @Setter
    private long latestConfirmedBlockNumber;

    @Getter
    @Setter
    private LogInfo logInfo;

    @Getter
    @Setter
    private RawData rawData;

    @Getter
    @Setter
    private ABI abi;
}
