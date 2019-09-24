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



package org.gsc.core.wrapper;

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.Constant;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.SmartContract.ABI;
import org.gsc.protos.Protocol.Transaction;

@Slf4j(topic = "wrapper")
public class ContractWrapper implements ProtoWrapper<SmartContract> {

    private SmartContract smartContract;

    /**
     * constructor TransactionWrapper.
     */
    public ContractWrapper(SmartContract smartContract) {
        this.smartContract = smartContract;
    }

    public ContractWrapper(byte[] data) {
        try {
            this.smartContract = SmartContract.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            // logger.debug(e.getMessage());
        }
    }

    public static CreateSmartContract getSmartContractFromTransaction(Transaction trx) {
        try {
            Any any = trx.getRawData().getContract(0).getParameter();
            CreateSmartContract createSmartContract = any.unpack(CreateSmartContract.class);
            return createSmartContract;
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    public static TriggerSmartContract getTriggerContractFromTransaction(Transaction trx) {
        try {
            Any any = trx.getRawData().getContract(0).getParameter();
            TriggerSmartContract contractTriggerContract = any.unpack(TriggerSmartContract.class);
            return contractTriggerContract;
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    public byte[] getCodeHash() {
        return this.smartContract.getCodeHash().toByteArray();
    }

    public void setCodeHash(byte[] codeHash) {
        this.smartContract = this.smartContract.toBuilder().setCodeHash(ByteString.copyFrom(codeHash))
                .build();
    }

    @Override
    public byte[] getData() {
        return this.smartContract.toByteArray();
    }

    @Override
    public SmartContract getInstance() {
        return this.smartContract;
    }

    @Override
    public String toString() {
        return this.smartContract.toString();
    }

    public byte[] getOriginAddress() {
        return this.smartContract.getOriginAddress().toByteArray();
    }

    public long getConsumeUserResourcePercent() {
        long percent = this.smartContract.getConsumeUserResourcePercent();
        return max(0, min(percent, Constant.ONE_HUNDRED));
    }

    public long getOriginCpuLimit() {
        long originCpuLimit = this.smartContract.getOriginCpuLimit();
        if (originCpuLimit == Constant.PB_DEFAULT_CPU_LIMIT) {
            originCpuLimit = Constant.CREATOR_DEFAULT_CPU_LIMIT;
        }
        return originCpuLimit;
    }

    public void clearABI() {
        this.smartContract = this.smartContract.toBuilder().setAbi(ABI.getDefaultInstance()).build();
    }

    public byte[] getTrxHash() {
        return this.smartContract.getTrxHash().toByteArray();
    }
}
