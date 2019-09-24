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
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.Manager;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.AccountCreateContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class CreateAccountOperator extends AbstractOperator {

    CreateAccountOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret)
            throws ContractExeException {
        long fee = calcFee();
        try {
            AccountCreateContract accountCreateContract = contract.unpack(AccountCreateContract.class);
            boolean withDefaultPermission =
                    dbManager.getDynamicPropertiesStore().getAllowMultiSign() == 1;
            AccountWrapper accountWrapper = new AccountWrapper(accountCreateContract,
                    dbManager.getHeadBlockTimeStamp(), withDefaultPermission, dbManager);

            dbManager.getAccountStore()
                    .put(accountCreateContract.getAccountAddress().toByteArray(), accountWrapper);

            dbManager.adjustBalance(accountCreateContract.getOwnerAddress().toByteArray(), -fee);
            // Add to blackhole address
            dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().createDbKey(), fee);

            ret.setStatus(fee, code.SUCESS);
        } catch (BalanceInsufficientException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
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
        if (!contract.is(AccountCreateContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [AccountCreateContract],real type[" + contract
                            .getClass() + "]");
        }
        final AccountCreateContract contract;
        try {
            contract = this.contract.unpack(AccountCreateContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
//    if (contract.getAccountName().isEmpty()) {
//      throw new ContractValidateException("AccountName is null");
//    }
        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid ownerAddress");
        }

        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        if (accountWrapper == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }

        final long fee = calcFee();
        if (accountWrapper.getBalance() < fee) {
            throw new ContractValidateException(
                    "Validate CreateAccountOperator error, insufficient fee.");
        }

        byte[] accountAddress = contract.getAccountAddress().toByteArray();
        if (!Wallet.addressValid(accountAddress)) {
            throw new ContractValidateException("Invalid account address");
        }

//    if (contract.getType() == null) {
//      throw new ContractValidateException("Type is null");
//    }

        if (dbManager.getAccountStore().has(accountAddress)) {
            throw new ContractValidateException("Account has existed");
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(AccountCreateContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract();
    }
}
