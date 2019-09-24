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

package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.AccountStore;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.UpdateSettingContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class UpdateSettingContractOperator extends AbstractOperator {

    UpdateSettingContractOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            UpdateSettingContract usContract = contract
                    .unpack(UpdateSettingContract.class);
            long newPercent = usContract.getConsumeUserResourcePercent();
            byte[] contractAddress = usContract.getContractAddress().toByteArray();
            ContractWrapper deployedContract = dbManager.getContractStore().get(contractAddress);

            dbManager.getContractStore().put(contractAddress, new ContractWrapper(
                    deployedContract.getInstance().toBuilder().setConsumeUserResourcePercent(newPercent)
                            .build()));

            ret.setStatus(fee, code.SUCESS);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }
        return true;
    }

    @Override
    public boolean validate() throws ContractValidateException {
        if (this.contract == null) {
            throw new ContractValidateException("No contract!");
        }
        if (this.dbManager == null) {
            throw new ContractValidateException("No dbManager!");
        }
        if (!this.contract.is(UpdateSettingContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [UpdateSettingContract],real type["
                            + contract
                            .getClass() + "]");
        }
        final UpdateSettingContract contract;
        try {
            contract = this.contract.unpack(UpdateSettingContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        if (!Wallet.addressValid(contract.getOwnerAddress().toByteArray())) {
            throw new ContractValidateException("Invalid address");
        }
        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

        AccountStore accountStore = dbManager.getAccountStore();

        AccountWrapper accountWrapper = accountStore.get(ownerAddress);
        if (accountWrapper == null) {
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }

        long newPercent = contract.getConsumeUserResourcePercent();
        if (newPercent > 100 || newPercent < 0) {
            throw new ContractValidateException(
                    "percent not in [0, 100]");
        }

        byte[] contractAddress = contract.getContractAddress().toByteArray();
        ContractWrapper deployedContract = dbManager.getContractStore().get(contractAddress);

        if (deployedContract == null) {
            throw new ContractValidateException(
                    "Contract not exists");
        }

        byte[] deployedContractOwnerAddress = deployedContract.getInstance().getOriginAddress()
                .toByteArray();

        if (!Arrays.equals(ownerAddress, deployedContractOwnerAddress)) {
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] is not the owner of the contract");
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(UpdateSettingContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
