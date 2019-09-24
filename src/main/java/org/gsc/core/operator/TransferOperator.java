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
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.dbsource.Deposit;
import org.gsc.core.Wallet;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.db.Manager;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class TransferOperator extends AbstractOperator {

    TransferOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            TransferContract transferContract = contract.unpack(TransferContract.class);
            long amount = transferContract.getAmount();
            byte[] toAddress = transferContract.getToAddress().toByteArray();
            byte[] ownerAddress = transferContract.getOwnerAddress().toByteArray();

            // if account with to_address does not exist, create it first.
            AccountWrapper toAccount = dbManager.getAccountStore().get(toAddress);
            if (toAccount == null) {
                boolean withDefaultPermission =
                        dbManager.getDynamicPropertiesStore().getAllowMultiSign() == 1;
                toAccount = new AccountWrapper(ByteString.copyFrom(toAddress), AccountType.Normal,
                        dbManager.getHeadBlockTimeStamp(), withDefaultPermission, dbManager);
                dbManager.getAccountStore().put(toAddress, toAccount);

                fee = fee + dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract();
            }
            dbManager.adjustBalance(ownerAddress, -fee);
            dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().createDbKey(), fee);
            ret.setStatus(fee, code.SUCESS);
            dbManager.adjustBalance(ownerAddress, -amount);
            dbManager.adjustBalance(toAddress, amount);
        } catch (BalanceInsufficientException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        } catch (ArithmeticException e) {
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
        if (!this.contract.is(TransferContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [TransferContract],real type[" + contract
                            .getClass() + "]");
        }
        long fee = calcFee();
        final TransferContract transferContract;
        try {
            transferContract = contract.unpack(TransferContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        byte[] toAddress = transferContract.getToAddress().toByteArray();
        byte[] ownerAddress = transferContract.getOwnerAddress().toByteArray();
        long amount = transferContract.getAmount();

        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid ownerAddress");
        }
        if (!Wallet.addressValid(toAddress)) {
            throw new ContractValidateException("Invalid toAddress");
        }

        if (Arrays.equals(toAddress, ownerAddress)) {
            throw new ContractValidateException("Cannot transfer gsc to yourself.");
        }

        AccountWrapper ownerAccount = dbManager.getAccountStore().get(ownerAddress);
        if (ownerAccount == null) {
            throw new ContractValidateException("Validate TransferContract error, no OwnerAccount.");
        }

        long balance = ownerAccount.getBalance();

        if (amount <= 0) {
            throw new ContractValidateException("Amount must greater than 0.");
        }

        try {
            AccountWrapper toAccount = dbManager.getAccountStore().get(toAddress);
            if (toAccount == null) {
                fee = fee + dbManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract();
            }

            if (balance < Math.addExact(amount, fee)) {
                throw new ContractValidateException(
                        "Validate TransferContract error, balance is not sufficient.");
            }

            if (toAccount != null) {
                long toAddressBalance = Math.addExact(toAccount.getBalance(), amount);
            }
        } catch (ArithmeticException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        return true;
    }

    public static boolean validateForSmartContract(Deposit deposit, byte[] ownerAddress,
                                                   byte[] toAddress, long amount) throws ContractValidateException {
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid ownerAddress");
        }
        if (!Wallet.addressValid(toAddress)) {
            throw new ContractValidateException("Invalid toAddress");
        }

        if (Arrays.equals(toAddress, ownerAddress)) {
            throw new ContractValidateException("Cannot transfer gsc to yourself.");
        }

        AccountWrapper ownerAccount = deposit.getAccount(ownerAddress);
        if (ownerAccount == null) {
            throw new ContractValidateException("Validate InternalTransfer error, no OwnerAccount.");
        }

        AccountWrapper toAccount = deposit.getAccount(toAddress);
        if (toAccount == null) {
            throw new ContractValidateException(
                    "Validate InternalTransfer error, no ToAccount. And not allowed to create account in smart contract.");
        }

        long balance = ownerAccount.getBalance();

        if (amount < 0) {
            throw new ContractValidateException("Amount must greater than or equals 0.");
        }

        try {
            if (balance < amount) {
                throw new ContractValidateException(
                        "Validate InternalTransfer error, balance is not sufficient.");
            }

            if (toAccount != null) {
                long toAddressBalance = Math.addExact(toAccount.getBalance(), amount);
            }
        } catch (ArithmeticException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(TransferContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return ChainConstant.TRANSFER_FEE;
    }

}