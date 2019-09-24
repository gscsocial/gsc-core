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
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.utils.TransactionUtil;
import org.gsc.db.AccountIdIndexStore;
import org.gsc.db.AccountStore;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.SetAccountIdContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class SetAccountIdOperator extends AbstractOperator {

    SetAccountIdOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        final SetAccountIdContract setAccountIdContract;
        final long fee = calcFee();
        try {
            setAccountIdContract = contract.unpack(SetAccountIdContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }

        byte[] ownerAddress = setAccountIdContract.getOwnerAddress().toByteArray();
        AccountStore accountStore = dbManager.getAccountStore();
        AccountIdIndexStore accountIdIndexStore = dbManager.getAccountIdIndexStore();
        AccountWrapper account = accountStore.get(ownerAddress);

        account.setAccountId(setAccountIdContract.getAccountId().toByteArray());
        accountStore.put(ownerAddress, account);
        accountIdIndexStore.put(account);
        ret.setStatus(fee, code.SUCESS);

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
        if (!this.contract.is(SetAccountIdContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [SetAccountIdContract],real type[" + contract
                            .getClass() + "]");
        }
        final SetAccountIdContract setAccountIdContract;
        try {
            setAccountIdContract = contract.unpack(SetAccountIdContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        byte[] ownerAddress = setAccountIdContract.getOwnerAddress().toByteArray();
        byte[] accountId = setAccountIdContract.getAccountId().toByteArray();
        if (!TransactionUtil.validAccountId(accountId)) {
            throw new ContractValidateException("Invalid accountId");
        }
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid ownerAddress");
        }

        AccountWrapper account = dbManager.getAccountStore().get(ownerAddress);
        if (account == null) {
            throw new ContractValidateException("Account has not existed");
        }
        if (account.getAccountId() != null && !account.getAccountId().isEmpty()) {
            throw new ContractValidateException("This account id already set");
        }
        if (dbManager.getAccountIdIndexStore().has(accountId)) {
            throw new ContractValidateException("This id has existed");
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(SetAccountIdContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }
}
