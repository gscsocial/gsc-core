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

import java.math.BigInteger;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.gsc.utils.ByteArray;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ExchangeWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.utils.TransactionUtil;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract.ExchangeInjectContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class ExchangeInjectOperator extends AbstractOperator {

    ExchangeInjectOperator(final Any contract, final Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            final ExchangeInjectContract exchangeInjectContract = this.contract
                    .unpack(ExchangeInjectContract.class);
            AccountWrapper accountWrapper = dbManager.getAccountStore()
                    .get(exchangeInjectContract.getOwnerAddress().toByteArray());

            ExchangeWrapper exchangeWrapper;
            exchangeWrapper = dbManager.getExchangeStoreFinal().
                    get(ByteArray.fromLong(exchangeInjectContract.getExchangeId()));
            byte[] firstTokenID = exchangeWrapper.getFirstTokenId();
            byte[] secondTokenID = exchangeWrapper.getSecondTokenId();
            long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
            long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

            byte[] tokenID = exchangeInjectContract.getTokenId().toByteArray();
            long tokenQuant = exchangeInjectContract.getQuant();

            byte[] anotherTokenID;
            long anotherTokenQuant;

            if (Arrays.equals(tokenID, firstTokenID)) {
                anotherTokenID = secondTokenID;
                anotherTokenQuant = Math
                        .floorDiv(Math.multiplyExact(secondTokenBalance, tokenQuant), firstTokenBalance);
                exchangeWrapper.setBalance(firstTokenBalance + tokenQuant,
                        secondTokenBalance + anotherTokenQuant);
            } else {
                anotherTokenID = firstTokenID;
                anotherTokenQuant = Math
                        .floorDiv(Math.multiplyExact(firstTokenBalance, tokenQuant), secondTokenBalance);
                exchangeWrapper.setBalance(firstTokenBalance + anotherTokenQuant,
                        secondTokenBalance + tokenQuant);
            }

            long newBalance = accountWrapper.getBalance() - calcFee();
            accountWrapper.setBalance(newBalance);

            if (Arrays.equals(tokenID, "_".getBytes())) {
                accountWrapper.setBalance(newBalance - tokenQuant);
            } else {
                accountWrapper.reduceAssetAmountV2(tokenID, tokenQuant, dbManager);
            }

            if (Arrays.equals(anotherTokenID, "_".getBytes())) {
                accountWrapper.setBalance(newBalance - anotherTokenQuant);
            } else {
                accountWrapper.reduceAssetAmountV2(anotherTokenID, anotherTokenQuant, dbManager);
            }
            dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

            dbManager.putExchangeWrapper(exchangeWrapper);

            ret.setExchangeInjectAnotherAmount(anotherTokenQuant);
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
        if (!this.contract.is(ExchangeInjectContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [ExchangeInjectContract],real type[" + contract
                            .getClass() + "]");
        }
        final ExchangeInjectContract contract;
        try {
            contract = this.contract.unpack(ExchangeInjectContract.class);
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
            throw new ContractValidateException("No enough balance for exchange inject fee!");
        }

        ExchangeWrapper exchangeWrapper;
        try {
            exchangeWrapper = dbManager.getExchangeStoreFinal().
                    get(ByteArray.fromLong(contract.getExchangeId()));

        } catch (ItemNotFoundException ex) {
            throw new ContractValidateException("Exchange[" + contract.getExchangeId() + "] not exists");
        }

        if (!accountWrapper.getAddress().equals(exchangeWrapper.getCreatorAddress())) {
            throw new ContractValidateException("account[" + readableOwnerAddress + "] is not creator");
        }

        byte[] firstTokenID = exchangeWrapper.getFirstTokenId();
        byte[] secondTokenID = exchangeWrapper.getSecondTokenId();
        long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
        long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

        byte[] tokenID = contract.getTokenId().toByteArray();
        long tokenQuant = contract.getQuant();

        byte[] anotherTokenID;
        long anotherTokenQuant;

        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1) {
            if (!Arrays.equals(tokenID, "_".getBytes()) && !TransactionUtil.isNumber(tokenID)) {
                throw new ContractValidateException("token id is not a valid number");
            }
        }

        if (!Arrays.equals(tokenID, firstTokenID) && !Arrays.equals(tokenID, secondTokenID)) {
            throw new ContractValidateException("token id is not in exchange");
        }

        if (firstTokenBalance == 0 || secondTokenBalance == 0) {
            throw new ContractValidateException("Token balance in exchange is equal with 0,"
                    + "the exchange has been closed");
        }

        if (tokenQuant <= 0) {
            throw new ContractValidateException("injected token quant must greater than zero");
        }

        BigInteger bigFirstTokenBalance = new BigInteger(String.valueOf(firstTokenBalance));
        BigInteger bigSecondTokenBalance = new BigInteger(String.valueOf(secondTokenBalance));
        BigInteger bigTokenQuant = new BigInteger(String.valueOf(tokenQuant));
        long newTokenBalance, newAnotherTokenBalance;
        if (Arrays.equals(tokenID, firstTokenID)) {
            anotherTokenID = secondTokenID;
//      anotherTokenQuant = Math
//          .floorDiv(Math.multiplyExact(secondTokenBalance, tokenQuant), firstTokenBalance);
            anotherTokenQuant = bigSecondTokenBalance.multiply(bigTokenQuant)
                    .divide(bigFirstTokenBalance).longValueExact();
            newTokenBalance = firstTokenBalance + tokenQuant;
            newAnotherTokenBalance = secondTokenBalance + anotherTokenQuant;
        } else {
            anotherTokenID = firstTokenID;
//      anotherTokenQuant = Math
//          .floorDiv(Math.multiplyExact(firstTokenBalance, tokenQuant), secondTokenBalance);
            anotherTokenQuant = bigFirstTokenBalance.multiply(bigTokenQuant)
                    .divide(bigSecondTokenBalance).longValueExact();
            newTokenBalance = secondTokenBalance + tokenQuant;
            newAnotherTokenBalance = firstTokenBalance + anotherTokenQuant;
        }

        if (anotherTokenQuant <= 0) {
            throw new ContractValidateException("the calculated token quant  must be greater than 0");
        }

        long balanceLimit = dbManager.getDynamicPropertiesStore().getExchangeBalanceLimit();
        if (newTokenBalance > balanceLimit || newAnotherTokenBalance > balanceLimit) {
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

        if (Arrays.equals(anotherTokenID, "_".getBytes())) {
            if (accountWrapper.getBalance() < (anotherTokenQuant + calcFee())) {
                throw new ContractValidateException("balance is not enough");
            }
        } else {
            if (!accountWrapper.assetBalanceEnoughV2(anotherTokenID, anotherTokenQuant, dbManager)) {
                throw new ContractValidateException("another token balance is not enough");
            }
        }

        return true;
    }


    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(ExchangeInjectContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
