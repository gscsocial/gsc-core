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
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.runtime.config.VMConfig;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.AccountStore;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.ClearABIContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class ClearABIContractOperator extends AbstractOperator {

    ClearABIContractOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            ClearABIContract usContract = contract.unpack(ClearABIContract.class);

            byte[] contractAddress = usContract.getContractAddress().toByteArray();
            ContractWrapper deployedContract = dbManager.getContractStore().get(contractAddress);

            deployedContract.clearABI();
            dbManager.getContractStore().put(contractAddress, deployedContract);

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
        if (!VMConfig.allowGvmConstantinople()) {
            throw new ContractValidateException(
                    "contract type error,unexpected type [ClearABIContract]");
        }

        if (this.contract == null) {
            throw new ContractValidateException("No contract!");
        }
        if (this.dbManager == null) {
            throw new ContractValidateException("No dbManager!");
        }
        if (!this.contract.is(ClearABIContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [ClearABIContract],real type["
                            + contract
                            .getClass() + "]");
        }
        final ClearABIContract contract;
        try {
            contract = this.contract.unpack(ClearABIContract.class);
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
        return contract.unpack(ClearABIContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
