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


package org.gsc.core.wrapper.utils;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract;

@Slf4j(topic = "wrapper")
public class TransactionUtil {

    public static Transaction newGenesisTransaction(byte[] key, long value)
            throws IllegalArgumentException {

        if (!Wallet.addressValid(key)) {
            throw new IllegalArgumentException("Invalid address");
        }
        byte[] genesisAddress = new byte[23];
        System.arraycopy(Wallet.getAddressPreFixByte(),0 , genesisAddress, 0, Wallet.getAddressPreFixByte().length);

        TransferContract transferContract = TransferContract.newBuilder()
                .setAmount(value)
//            .setOwnerAddress(ByteString.copyFrom("0xfffffffffffffffffffff".getBytes()))
                .setOwnerAddress(ByteString.copyFrom(genesisAddress))
                .setToAddress(ByteString.copyFrom(key))
                .build();

        Transaction transaction = new TransactionWrapper(transferContract,
                Contract.ContractType.TransferContract).getInstance();
//    Transaction.raw raw = transaction
//            .getRawData().toBuilder().setData(ByteString.copyFromUtf8("In Us We Trust. 2018/12/25.")).build();
//    transaction = transaction.toBuilder().setRawData(raw).build();
        return transaction;
    }

    public static boolean validAccountName(byte[] accountName) {
        if (ArrayUtils.isEmpty(accountName)) {
            return true;   //accountname can empty
        }

        return accountName.length <= 200;
    }

    public static boolean validAccountId(byte[] accountId) {
        if (ArrayUtils.isEmpty(accountId)) {
            return false;
        }

        if (accountId.length < 8) {
            return false;
        }

        if (accountId.length > 32) {
            return false;
        }
        // b must read able.
        for (byte b : accountId) {
            if (b < 0x21) {
                return false; // 0x21 = '!'
            }
            if (b > 0x7E) {
                return false; // 0x7E = '~'
            }
        }
        return true;
    }

    public static boolean validAssetName(byte[] assetName) {
        if (ArrayUtils.isEmpty(assetName)) {
            return false;
        }
        if (assetName.length > 32) {
            return false;
        }
        // b must read able.
        for (byte b : assetName) {
            if (b < 0x21) {
                return false; // 0x21 = '!'
            }
            if (b > 0x7E) {
                return false; // 0x7E = '~'
            }
        }
        return true;
    }

    public static boolean validTokenAbbrName(byte[] abbrName) {
        if (ArrayUtils.isEmpty(abbrName)) {
            return false;
        }
        if (abbrName.length > 5) {
            return false;
        }
        // b must read able.
        for (byte b : abbrName) {
            if (b < 0x21) {
                return false; // 0x21 = '!'
            }
            if (b > 0x7E) {
                return false; // 0x7E = '~'
            }
        }
        return true;
    }


    public static boolean validAssetDescription(byte[] description) {
        if (ArrayUtils.isEmpty(description)) {
            return true;   //description can empty
        }

        return description.length <= 200;
    }

    public static boolean validUrl(byte[] url) {
        if (ArrayUtils.isEmpty(url)) {
            return false;
        }
        return url.length <= 256;
    }

    public static boolean isNumber(byte[] id) {
        if (ArrayUtils.isEmpty(id)) {
            return false;
        }
        for (byte b : id) {
            if (b < '0' || b > '9') {
                return false;
            }
        }

        return !(id.length > 1 && id[0] == '0');
    }

    /**
     * Get sender.
     */
 /* public static byte[] getSender(Transaction tx) {
    byte[] pubKey = tx.getRawData().getVin(0).getRawData().getPubKey().toByteArray();
    return ECKey.computeAddress(pubKey);
  } */

}
