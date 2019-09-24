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
import org.gsc.core.wrapper.ExchangeWrapper;
import org.gsc.utils.ByteArray;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.utils.TransactionUtil;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract.ExchangeTransactionContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class ExchangeTransactionOperator extends AbstractOperator {

    ExchangeTransactionOperator(final Any contract, final Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            final ExchangeTransactionContract exchangeTransactionContract = this.contract
                    .unpack(ExchangeTransactionContract.class);
            AccountWrapper accountWrapper = dbManager.getAccountStore()
                    .get(exchangeTransactionContract.getOwnerAddress().toByteArray());

            ExchangeWrapper exchangeWrapper = dbManager.getExchangeStoreFinal().
                    get(ByteArray.fromLong(exchangeTransactionContract.getExchangeId()));

            byte[] firstTokenID = exchangeWrapper.getFirstTokenId();
            byte[] secondTokenID = exchangeWrapper.getSecondTokenId();

            byte[] tokenID = exchangeTransactionContract.getTokenId().toByteArray();
            long tokenQuant = exchangeTransactionContract.getQuant();

            byte[] anotherTokenID;
            long anotherTokenQuant = exchangeWrapper.transaction(tokenID, tokenQuant);

            if (Arrays.equals(tokenID, firstTokenID)) {
                anotherTokenID = secondTokenID;
            } else {
                anotherTokenID = firstTokenID;
            }

            long newBalance = accountWrapper.getBalance() - calcFee();
            accountWrapper.setBalance(newBalance);

            if (Arrays.equals(tokenID, "_".getBytes())) {
                accountWrapper.setBalance(newBalance - tokenQuant);
            } else {
                accountWrapper.reduceAssetAmountV2(tokenID, tokenQuant, dbManager);
            }

            if (Arrays.equals(anotherTokenID, "_".getBytes())) {
                accountWrapper.setBalance(newBalance + anotherTokenQuant);
            } else {
                accountWrapper.addAssetAmountV2(anotherTokenID, anotherTokenQuant, dbManager);
            }

            dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

            dbManager.putExchangeWrapper(exchangeWrapper);

            ret.setExchangeReceivedAmount(anotherTokenQuant);
            ret.setStatus(fee, code.SUCESS);
        } catch (ItemNotFoundException e) {
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
        if (!this.contract.is(ExchangeTransactionContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [ExchangeTransactionContract],real type[" + contract
                            .getClass() + "]");
        }
        final ExchangeTransactionContract contract;
        try {
            contract = this.contract.unpack(ExchangeTransactionContract.class);
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
            throw new ContractValidateException("No enough balance for exchange transaction fee!");
        }

        ExchangeWrapper exchangeWrapper;
        try {
            exchangeWrapper = dbManager.getExchangeStoreFinal().
                    get(ByteArray.fromLong(contract.getExchangeId()));
        } catch (ItemNotFoundException ex) {
            throw new ContractValidateException("Exchange[" + contract.getExchangeId() + "] not exists");
        }

        byte[] firstTokenID = exchangeWrapper.getFirstTokenId();
        byte[] secondTokenID = exchangeWrapper.getSecondTokenId();
        long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
        long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

        byte[] tokenID = contract.getTokenId().toByteArray();
        long tokenQuant = contract.getQuant();
        long tokenExpected = contract.getExpected();

        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1) {
            if (!Arrays.equals(tokenID, "_".getBytes()) && !TransactionUtil.isNumber(tokenID)) {
                throw new ContractValidateException("token id is not a valid number");
            }
        }
        if (!Arrays.equals(tokenID, firstTokenID) && !Arrays.equals(tokenID, secondTokenID)) {
            throw new ContractValidateException("token is not in exchange");
        }

        if (tokenQuant <= 0) {
            throw new ContractValidateException("token quant must greater than zero");
        }

        if (tokenExpected <= 0) {
            throw new ContractValidateException("token expected must greater than zero");
        }

        if (firstTokenBalance == 0 || secondTokenBalance == 0) {
            throw new ContractValidateException("Token balance in exchange is equal with 0,"
                    + "the exchange has been closed");
        }

        long balanceLimit = dbManager.getDynamicPropertiesStore().getExchangeBalanceLimit();
        long tokenBalance = (Arrays.equals(tokenID, firstTokenID) ? firstTokenBalance
                : secondTokenBalance);
        tokenBalance += tokenQuant;
        if (tokenBalance > balanceLimit) {
            throw new ContractValidateException("token balance must less than " + balanceLimit + "L");
        }

        if (Arrays.equals(tokenID, "_".getBytes())) {
            if (accountWrapper.getBalance() < (tokenQuant + calcFee())) {
                throw new ContractValidateException("balance is not enough");
            }
        } else {
            if (!accountWrapper.assetBalanceEnoughV2(tokenID, tokenQuant, dbManager)) {
                throw new ContractValidateException("token balance is not enough");
            }
        }

        long anotherTokenQuant = exchangeWrapper.transaction(tokenID, tokenQuant);
        if (anotherTokenQuant < tokenExpected) {
            throw new ContractValidateException("token required must greater than expected");
        }

        return true;
    }


    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(ExchangeTransactionContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
