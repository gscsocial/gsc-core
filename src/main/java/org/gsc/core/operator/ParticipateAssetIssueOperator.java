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
import org.gsc.utils.ByteArray;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.ParticipateAssetIssueContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class ParticipateAssetIssueOperator extends AbstractOperator {

    ParticipateAssetIssueOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            final ParticipateAssetIssueContract participateAssetIssueContract =
                    contract.unpack(Contract.ParticipateAssetIssueContract.class);
            long cost = participateAssetIssueContract.getAmount();

            //subtract from owner address
            byte[] ownerAddress = participateAssetIssueContract.getOwnerAddress().toByteArray();
            AccountWrapper ownerAccount = this.dbManager.getAccountStore().get(ownerAddress);
            long balance = Math.subtractExact(ownerAccount.getBalance(), cost);
            balance = Math.subtractExact(balance, fee);
            ownerAccount.setBalance(balance);
            byte[] key = participateAssetIssueContract.getAssetName().toByteArray();

            //calculate the exchange amount
            AssetIssueWrapper assetIssueWrapper;
            assetIssueWrapper = this.dbManager.getAssetIssueStoreFinal().get(key);

            long exchangeAmount = Math.multiplyExact(cost, assetIssueWrapper.getNum());
            exchangeAmount = Math.floorDiv(exchangeAmount, assetIssueWrapper.getGscNum());
            ownerAccount.addAssetAmountV2(key, exchangeAmount, dbManager);

            //add to to_address
            byte[] toAddress = participateAssetIssueContract.getToAddress().toByteArray();
            AccountWrapper toAccount = this.dbManager.getAccountStore().get(toAddress);
            toAccount.setBalance(Math.addExact(toAccount.getBalance(), cost));
            if (!toAccount.reduceAssetAmountV2(key, exchangeAmount, dbManager)) {
                throw new ContractExeException("reduceAssetAmount failed !");
            }

            //write to db
            dbManager.getAccountStore().put(ownerAddress, ownerAccount);
            dbManager.getAccountStore().put(toAddress, toAccount);
            ret.setStatus(fee, Protocol.Transaction.Result.code.SUCESS);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        } catch (ArithmeticException e) {
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
        if (!this.contract.is(ParticipateAssetIssueContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [ParticipateAssetIssueContract],real type[" + contract
                            .getClass() + "]");
        }

        final ParticipateAssetIssueContract participateAssetIssueContract;
        try {
            participateAssetIssueContract =
                    this.contract.unpack(ParticipateAssetIssueContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        //Parameters check
        byte[] ownerAddress = participateAssetIssueContract.getOwnerAddress().toByteArray();
        byte[] toAddress = participateAssetIssueContract.getToAddress().toByteArray();
        byte[] assetName = participateAssetIssueContract.getAssetName().toByteArray();
        long amount = participateAssetIssueContract.getAmount();

        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid ownerAddress");
        }
        if (!Wallet.addressValid(toAddress)) {
            throw new ContractValidateException("Invalid toAddress");
        }
//    if (!TransactionUtil.validAssetName(assetName)) {
//      throw new ContractValidateException("Invalid assetName");
//    }
        if (amount <= 0) {
            throw new ContractValidateException("Amount must greater than 0!");
        }

        if (Arrays.equals(ownerAddress, toAddress)) {
            throw new ContractValidateException("Cannot participate asset Issue yourself !");
        }

        //Whether the account exist
        AccountWrapper ownerAccount = this.dbManager.getAccountStore().get(ownerAddress);
        if (ownerAccount == null) {
            throw new ContractValidateException("Account does not exist!");
        }
        try {
            //Whether the balance is enough
            long fee = calcFee();
            if (ownerAccount.getBalance() < Math.addExact(amount, fee)) {
                throw new ContractValidateException("No enough balance !");
            }

            //Whether have the mapping
            AssetIssueWrapper assetIssueWrapper;
            assetIssueWrapper = this.dbManager.getAssetIssueStoreFinal().get(assetName);
            if (assetIssueWrapper == null) {
                throw new ContractValidateException("No asset named " + ByteArray.toStr(assetName));
            }

            if (!Arrays.equals(toAddress, assetIssueWrapper.getOwnerAddress().toByteArray())) {
                throw new ContractValidateException(
                        "The asset is not issued by " + ByteArray.toHexString(toAddress));
            }
            //Whether the exchange can be processed: to see if the exchange can be the exact int
            long now = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
            if (now >= assetIssueWrapper.getEndTime() || now < assetIssueWrapper
                    .getStartTime()) {
                throw new ContractValidateException("No longer valid period!");
            }

            int gscNum = assetIssueWrapper.getGscNum();
            int num = assetIssueWrapper.getNum();
            long exchangeAmount = Math.multiplyExact(amount, num);
            exchangeAmount = Math.floorDiv(exchangeAmount, gscNum);
            if (exchangeAmount <= 0) {
                throw new ContractValidateException("Can not process the exchange!");
            }

            AccountWrapper toAccount = this.dbManager.getAccountStore().get(toAddress);
            if (toAccount == null) {
                throw new ContractValidateException("To account does not exist!");
            }

            if (!toAccount.assetBalanceEnoughV2(assetName, exchangeAmount,
                    dbManager)) {
                throw new ContractValidateException("Asset balance is not enough !");
            }
        } catch (ArithmeticException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return this.contract.unpack(Contract.ParticipateAssetIssueContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }
}
