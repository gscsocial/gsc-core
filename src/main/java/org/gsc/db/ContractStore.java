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

package org.gsc.db;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.ContractWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.protos.Protocol.SmartContract;

@Slf4j(topic = "DB")
@Component
public class ContractStore extends GSCStoreWithRevoking<ContractWrapper> {

    @Autowired
    private ContractStore(@Value("contract") String dbName) {
        super(dbName);
    }

    @Override
    public ContractWrapper get(byte[] key) {
        return getUnchecked(key);
    }

    /**
     * get total transaction.
     */
    public long getTotalContracts() {
        return Streams.stream(revokingDB.iterator()).count();
    }

    /**
     * find a transaction  by it's id.
     */
    public byte[] findContractByHash(byte[] trxHash) {
        return revokingDB.getUnchecked(trxHash);
    }

    /**
     * @param contractAddress
     * @return
     */
    public SmartContract.ABI getABI(byte[] contractAddress) {
        byte[] value = revokingDB.getUnchecked(contractAddress);
        if (ArrayUtils.isEmpty(value)) {
            return null;
        }

        ContractWrapper contractWrapper = new ContractWrapper(value);
        SmartContract smartContract = contractWrapper.getInstance();
        if (smartContract == null) {
            return null;
        }

        return smartContract.getAbi();
    }

}
