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

import java.math.BigDecimal;
import java.math.BigInteger;
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
import org.gsc.protos.Contract.ExchangeWithdrawContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class ExchangeWithdrawOperator extends AbstractOperator {

    ExchangeWithdrawOperator(final Any contract, final Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            final ExchangeWithdrawContract exchangeWithdrawContract = this.contract
                    .unpack(ExchangeWithdrawContract.class);
            AccountWrapper accountWrapper = dbManager.getAccountStore()
                    .get(exchangeWithdrawContract.getOwnerAddress().toByteArray());

            ExchangeWrapper exchangeWrapper = dbManager.getExchangeStoreFinal().
                    get(ByteArray.fromLong(exchangeWithdrawContract.getExchangeId()));

            byte[] firstTokenID = exchangeWrapper.getFirstTokenId();
            byte[] secondTokenID = exchangeWrapper.getSecondTokenId();
            long firstTokenBalance = exchangeWrapper.getFirstTokenBalance();
            long secondTokenBalance = exchangeWrapper.getSecondTokenBalance();

            byte[] tokenID = exchangeWithdrawContract.getTokenId().toByteArray();
            long tokenQuant = exchangeWithdrawContract.getQuant();

            byte[] anotherTokenID;
            long anotherTokenQuant;

            BigInteger bigFirstTokenBalance = new BigInteger(String.valueOf(firstTokenBalance));
            BigInteger bigSecondTokenBalance = new BigInteger(String.valueOf(secondTokenBalance));
            BigInteger bigTokenQuant = new BigInteger(String.valueOf(tokenQuant));
            if (Arrays.equals(tokenID, firstTokenID)) {
                anotherTokenID = secondTokenID;
//        anotherTokenQuant = Math
//            .floorDiv(Math.multiplyExact(secondTokenBalance, tokenQuant), firstTokenBalance);
                anotherTokenQuant = bigSecondTokenBalance.multiply(bigTokenQuant)
                        .divide(bigFirstTokenBalance).longValueExact();
                exchangeWrapper.setBalance(firstTokenBalance - tokenQuant,
                        secondTokenBalance - anotherTokenQuant);
            } else {
                anotherTokenID = firstTokenID;
//        anotherTokenQuant = Math
//            .floorDiv(Math.multiplyExact(firstTokenBalance, tokenQuant), secondTokenBalance);
                anotherTokenQuant = bigFirstTokenBalance.multiply(bigTokenQuant)
                        .divide(bigSecondTokenBalance).longValueExact();
                exchangeWrapper.setBalance(firstTokenBalance - anotherTokenQuant,
                        secondTokenBalance - tokenQuant);
            }

            long newBalance = accountWrapper.getBalance() - calcFee();

            if (Arrays.equals(tokenID, "_".getBytes())) {
                accountWrapper.setBalance(newBalance + tokenQuant);
            } else {
                accountWrapper.addAssetAmountV2(tokenID, tokenQuant, dbManager);
            }

            if (Arrays.equals(anotherTokenID, "_".getBytes())) {
                accountWrapper.setBalance(newBalance + anotherTokenQuant);
            } else {
                accountWrapper.addAssetAmountV2(anotherTokenID, anotherTokenQuant, dbManager);
            }

            dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

            dbManager.putExchangeWrapper(exchangeWrapper);

            ret.setExchangeWithdrawAnotherAmount(anotherTokenQuant);
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
        if (!this.contract.is(ExchangeWithdrawContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [ExchangeWithdrawContract],real type[" + contract
                            .getClass() + "]");
        }
        final ExchangeWithdrawContract contract;
        try {
            contract = this.contract.unpack(ExchangeWithdrawContract.class);
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
            throw new ContractValidateException("No enough balance for exchange withdraw fee!");
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

        long anotherTokenQuant;

        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 1) {
            if (!Arrays.equals(tokenID, "_".getBytes()) && !TransactionUtil.isNumber(tokenID)) {
                throw new ContractValidateException("token id is not a valid number");
            }
        }

        if (!Arrays.equals(tokenID, firstTokenID) && !Arrays.equals(tokenID, secondTokenID)) {
            throw new ContractValidateException("token is not in exchange");
        }

        if (tokenQuant <= 0) {
            throw new ContractValidateException("withdraw token quant must greater than zero");
        }

        if (firstTokenBalance == 0 || secondTokenBalance == 0) {
            throw new ContractValidateException("Token balance in exchange is equal with 0,"
                    + "the exchange has been closed");
        }

        BigDecimal bigFirstTokenBalance = new BigDecimal(String.valueOf(firstTokenBalance));
        BigDecimal bigSecondTokenBalance = new BigDecimal(String.valueOf(secondTokenBalance));
        BigDecimal bigTokenQuant = new BigDecimal(String.valueOf(tokenQuant));
        if (Arrays.equals(tokenID, firstTokenID)) {
//      anotherTokenQuant = Math
//          .floorDiv(Math.multiplyExact(secondTokenBalance, tokenQuant), firstTokenBalance);
            anotherTokenQuant = bigSecondTokenBalance.multiply(bigTokenQuant)
                    .divideToIntegralValue(bigFirstTokenBalance).longValueExact();
            if (firstTokenBalance < tokenQuant || secondTokenBalance < anotherTokenQuant) {
                throw new ContractValidateException("exchange balance is not enough");
            }

            if (anotherTokenQuant <= 0) {
                throw new ContractValidateException("withdraw another token quant must greater than zero");
            }

            double remainder = bigSecondTokenBalance.multiply(bigTokenQuant)
                    .divide(bigFirstTokenBalance, 4, BigDecimal.ROUND_HALF_UP).doubleValue()
                    - anotherTokenQuant;
            if (remainder / anotherTokenQuant > 0.0001) {
                throw new ContractValidateException("Not precise enough");
            }

        } else {
//      anotherTokenQuant = Math
//          .floorDiv(Math.multiplyExact(firstTokenBalance, tokenQuant), secondTokenBalance);
            anotherTokenQuant = bigFirstTokenBalance.multiply(bigTokenQuant)
                    .divideToIntegralValue(bigSecondTokenBalance).longValueExact();
            if (secondTokenBalance < tokenQuant || firstTokenBalance < anotherTokenQuant) {
                throw new ContractValidateException("exchange balance is not enough");
            }

            if (anotherTokenQuant <= 0) {
                throw new ContractValidateException("withdraw another token quant must greater than zero");
            }

            double remainder = bigFirstTokenBalance.multiply(bigTokenQuant)
                    .divide(bigSecondTokenBalance, 4, BigDecimal.ROUND_HALF_UP).doubleValue()
                    - anotherTokenQuant;
            if (remainder / anotherTokenQuant > 0.0001) {
                throw new ContractValidateException("Not precise enough");
            }
        }

        return true;
    }


    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(ExchangeWithdrawContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
