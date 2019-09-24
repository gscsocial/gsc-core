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

package org.gsc.runtime.utils;

import org.gsc.core.operator.TransferAssetOperator;
import org.gsc.core.operator.TransferOperator;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.db.dbsource.Deposit;
import org.gsc.core.Wallet;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Protocol;
import org.spongycastle.util.encoders.Hex;

public class MUtil {

    private MUtil() {
    }

    public static void transfer(Deposit deposit, byte[] fromAddress, byte[] toAddress, long amount)
            throws ContractValidateException {
        if (0 == amount) {
            return;
        }
        TransferOperator.validateForSmartContract(deposit, fromAddress, toAddress, amount);
        deposit.addBalance(toAddress, amount);
        deposit.addBalance(fromAddress, -amount);
    }

    public static void transferAllToken(Deposit deposit, byte[] fromAddress, byte[] toAddress) {
        AccountWrapper fromAccountCap = deposit.getAccount(fromAddress);
        Protocol.Account.Builder fromBuilder = fromAccountCap.getInstance().toBuilder();
        AccountWrapper toAccountCap = deposit.getAccount(toAddress);
        Protocol.Account.Builder toBuilder = toAccountCap.getInstance().toBuilder();
        fromAccountCap.getAssetMapV2().forEach((tokenId, amount) -> {
            toBuilder.putAssetV2(tokenId, toBuilder.getAssetV2Map().getOrDefault(tokenId, 0L) + amount);
            fromBuilder.putAssetV2(tokenId, 0L);
        });
        deposit.putAccountValue(fromAddress, new AccountWrapper(fromBuilder.build()));
        deposit.putAccountValue(toAddress, new AccountWrapper(toBuilder.build()));
    }

    public static void transferToken(Deposit deposit, byte[] fromAddress, byte[] toAddress,
                                     String tokenId, long amount)
            throws ContractValidateException {
        if (0 == amount) {
            return;
        }
        TransferAssetOperator
                .validateForSmartContract(deposit, fromAddress, toAddress, tokenId.getBytes(), amount);
        deposit.addTokenBalance(toAddress, tokenId.getBytes(), amount);
        deposit.addTokenBalance(fromAddress, tokenId.getBytes(), -amount);
    }

    public static byte[] convertToGSCAddress(byte[] address) {
        if (address.length == 20) {
            byte[] newAddress = new byte[23];
            byte[] temp = Wallet.getAddressPreFixByte();
            System.arraycopy(temp, 0, newAddress, 0, temp.length);
            System.arraycopy(address, 0, newAddress, temp.length, address.length);
            address = newAddress;
        }
        return address;
    }
}
