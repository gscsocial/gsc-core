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

import static org.gsc.core.operator.OperatorConstant.ACCOUNT_EXCEPTION_STR;

import com.google.common.math.LongMath;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.WithdrawBalanceContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class WithdrawBalanceOperator extends AbstractOperator {

    WithdrawBalanceOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }


    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        final WithdrawBalanceContract withdrawBalanceContract;
        try {
            withdrawBalanceContract = contract.unpack(WithdrawBalanceContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }

        AccountWrapper accountWrapper = (Objects.isNull(getDeposit())) ? dbManager.getAccountStore().
                get(withdrawBalanceContract.getOwnerAddress().toByteArray())
                : getDeposit().getAccount(withdrawBalanceContract.getOwnerAddress().toByteArray());
        long oldBalance = accountWrapper.getBalance();
        long allowance = accountWrapper.getAllowance();

        long now = dbManager.getHeadBlockTimeStamp();
        accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
                .setBalance(oldBalance + allowance)
                .setAllowance(0L)
                .setLatestWithdrawTime(now)
                .build());
        if (Objects.isNull(getDeposit())) {
            dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
        } else {
            // cache
            deposit.putAccountValue(accountWrapper.createDbKey(), accountWrapper);
        }

        ret.setWithdrawAmount(allowance);
        ret.setStatus(fee, code.SUCESS);

        return true;
    }

    @Override
    public boolean validate() throws ContractValidateException {
        if (this.contract == null) {
            throw new ContractValidateException("No contract!");
        }
        if (dbManager == null && (getDeposit() == null || getDeposit().getDbManager() == null)) {
            throw new ContractValidateException("No dbManager!");
        }
        if (!this.contract.is(WithdrawBalanceContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [WithdrawBalanceContract],real type[" + contract
                            .getClass() + "]");
        }
        final WithdrawBalanceContract withdrawBalanceContract;
        try {
            withdrawBalanceContract = this.contract.unpack(WithdrawBalanceContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        byte[] ownerAddress = withdrawBalanceContract.getOwnerAddress().toByteArray();
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountWrapper accountWrapper =
                Objects.isNull(getDeposit()) ? dbManager.getAccountStore().get(ownerAddress)
                        : getDeposit().getAccount(ownerAddress);
        if (accountWrapper == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] not exists");
        }

        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
        if (!dbManager.getWitnessStore().has(ownerAddress)) {
            throw new ContractValidateException(
                    ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] is not a witnessAccount");
        }

        boolean isGP = Args.getInstance().getGenesisBlock().getWitnesses().stream().anyMatch(witness ->
                Arrays.equals(ownerAddress, witness.getAddress()));
        if (isGP) {
            throw new ContractValidateException(
                    ACCOUNT_EXCEPTION_STR + readableOwnerAddress
                            + "] is a guard representative and is not allowed to withdraw Balance");
        }

        long latestWithdrawTime = accountWrapper.getLatestWithdrawTime();
        long now = dbManager.getHeadBlockTimeStamp();
        long witnessAllowanceFrozenTime = Objects.isNull(getDeposit()) ?
                dbManager.getDynamicPropertiesStore().getWitnessAllowanceFrozenTime() * 86_400_000L :
                getDeposit().getWitnessAllowanceFrozenTime() * 86_400_000L;

        if (now - latestWithdrawTime < witnessAllowanceFrozenTime) {
            throw new ContractValidateException("The last withdraw time is "
                    + latestWithdrawTime + ",less than 24 hours");
        }

        if (accountWrapper.getAllowance() <= 0) {
            throw new ContractValidateException("witnessAccount does not have any allowance");
        }
        try {
            LongMath.checkedAdd(accountWrapper.getBalance(), accountWrapper.getAllowance());
        } catch (ArithmeticException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(WithdrawBalanceContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
