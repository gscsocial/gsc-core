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
import org.gsc.core.wrapper.ExchangeWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.utils.TransactionUtil;
import org.gsc.db.Manager;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.ExchangeCreateContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class ExchangeCreateOperator extends AbstractOperator {

    ExchangeCreateOperator(final Any contract, final Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            final ExchangeCreateContract exchangeCreateContract = this.contract
                    .unpack(ExchangeCreateContract.class);
            AccountWrapper accountWrapper = dbManager.getAccountStore()
                    .get(exchangeCreateContract.getOwnerAddress().toByteArray());

            byte[] firstTokenID = exchangeCreateContract.getFirstTokenId().toByteArray();
            byte[] secondTokenID = exchangeCreateContract.getSecondTokenId().toByteArray();
            long firstTokenBalance = exchangeCreateContract.getFirstTokenBalance();
            long secondTokenBalance = exchangeCreateContract.getSecondTokenBalance();

            long newBalance = accountWrapper.getBalance() - fee;

            accountWrapper.setBalance(newBalance);

            if (Arrays.equals(firstTokenID, "_".getBytes())) {
                accountWrapper.setBalance(newBalance - firstTokenBalance);
            } else {
                accountWrapper.reduceAssetAmountV2(firstTokenID, firstTokenBalance, dbManager);
            }

            if (Arrays.equals(secondTokenID, "_".getBytes())) {
                accountWrapper.setBalance(newBalance - secondTokenBalance);
            } else {
                accountWrapper.reduceAssetAmountV2(secondTokenID, secondTokenBalance, dbManager);
            }

            long id = dbManager.getDynamicPropertiesStore().getLatestExchangeNum() + 1;
            long now = dbManager.getHeadBlockTimeStamp();
            if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
                //save to old asset store
                ExchangeWrapper exchangeWrapper =
                        new ExchangeWrapper(
                                exchangeCreateContract.getOwnerAddress(),
                                id,
                                now,
                                firstTokenID,
                                secondTokenID
                        );
                exchangeWrapper.setBalance(firstTokenBalance, secondTokenBalance);
                dbManager.getExchangeStore().put(exchangeWrapper.createDbKey(), exchangeWrapper);

                //save to new asset store
                if (!Arrays.equals(firstTokenID, "_".getBytes())) {
                    String firstTokenRealID = dbManager.getAssetIssueStore().get(firstTokenID).getId();
                    firstTokenID = firstTokenRealID.getBytes();
                }
                if (!Arrays.equals(secondTokenID, "_".getBytes())) {
                    String secondTokenRealID = dbManager.getAssetIssueStore().get(secondTokenID).getId();
                    secondTokenID = secondTokenRealID.getBytes();
                }
            }

            {
                // only save to new asset store
                ExchangeWrapper exchangeWrapperV2 =
                        new ExchangeWrapper(
                                exchangeCreateContract.getOwnerAddress(),
                                id,
                                now,
                                firstTokenID,
                                secondTokenID
                        );
                exchangeWrapperV2.setBalance(firstTokenBalance, secondTokenBalance);
                dbManager.getExchangeV2Store().put(exchangeWrapperV2.createDbKey(), exchangeWrapperV2);
            }

            dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
            dbManager.getDynamicPropertiesStore().saveLatestExchangeNum(id);

            dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().createDbKey(), fee);

            ret.setExchangeId(id);
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
        if (!this.contract.is(ExchangeCreateContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [ExchangeCreateContract],real type[" + contract
                            .getClass() + "]");
        }
        final ExchangeCreateContract contract;
        try {
            contract = this.contract.unpack(ExchangeCreateContract.class);
        } catch (InvalidProtocolBufferException e) {
            throw new ContractValidateException(e.getMessage());
        }

        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        if (!this.dbManager.getAccountStore().has(ownerAddress)) {
            throw new ContractValidateException("account[" + readableOwnerAddress + "] not exists");
        }

        AccountWrapper accountWrapper = this.dbManager.getAccountStore().get(ownerAddress);

        if (accountWrapper.getBalance() < calcFee()) {
            throw new ContractValidateException("No enough balance for exchange create fee!");
        }

        byte[] firstTokenID = contract.getFirstTokenId().toByteArray();
        byte[] secondTokenID = contract.getSecondTokenId().toByteArray();
        long firstTokenBalance = contract.getFirstTokenBalance();
        long secondTokenBalance = contract.getSecondTokenBalance();

        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1) {
            if (!Arrays.equals(firstTokenID, "_".getBytes()) && !TransactionUtil.isNumber(firstTokenID)) {
                throw new ContractValidateException("first token id is not a valid number");
            }
            if (!Arrays.equals(secondTokenID, "_".getBytes()) && !TransactionUtil
                    .isNumber(secondTokenID)) {
                throw new ContractValidateException("second token id is not a valid number");
            }
        }

        if (Arrays.equals(firstTokenID, secondTokenID)) {
            throw new ContractValidateException("cannot exchange same tokens");
        }

        if (firstTokenBalance <= 0 || secondTokenBalance <= 0) {
            throw new ContractValidateException("token balance must greater than zero");
        }

        long balanceLimit = dbManager.getDynamicPropertiesStore().getExchangeBalanceLimit();
        if (firstTokenBalance > balanceLimit || secondTokenBalance > balanceLimit) {
            throw new ContractValidateException("token balance must less than " + balanceLimit + "L");
        }

        if (Arrays.equals(firstTokenID, "_".getBytes())) {
            if (accountWrapper.getBalance() < (firstTokenBalance + calcFee())) {
                throw new ContractValidateException("balance is not enough");
            }
        } else {
            if (!accountWrapper.assetBalanceEnoughV2(firstTokenID, firstTokenBalance, dbManager)) {
                throw new ContractValidateException("first token balance is not enough");
            }
        }

        if (Arrays.equals(secondTokenID, "_".getBytes())) {
            if (accountWrapper.getBalance() < (secondTokenBalance + calcFee())) {
                throw new ContractValidateException("balance is not enough");
            }
        } else {
            if (!accountWrapper.assetBalanceEnoughV2(secondTokenID, secondTokenBalance, dbManager)) {
                throw new ContractValidateException("second token balance is not enough");
            }
        }

        return true;
    }


    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(ExchangeCreateContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return dbManager.getDynamicPropertiesStore().getExchangeCreateFee();
    }

}
