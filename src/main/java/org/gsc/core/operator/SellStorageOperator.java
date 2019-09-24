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
import org.gsc.db.StorageMarket;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.SellStorageContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class SellStorageOperator extends AbstractOperator {

    private StorageMarket storageMarket;

    SellStorageOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
        storageMarket = new StorageMarket(dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        final SellStorageContract sellStorageContract;
        try {
            sellStorageContract = contract.unpack(SellStorageContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }

        AccountWrapper accountWrapper = dbManager.getAccountStore()
                .get(sellStorageContract.getOwnerAddress().toByteArray());

        long bytes = sellStorageContract.getStorageBytes();

        storageMarket.sellStorage(accountWrapper, bytes);

        ret.setStatus(fee, code.SUCESS);

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
        if (!contract.is(SellStorageContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [SellStorageContract],real type[" + contract
                            .getClass() + "]");
        }

        final SellStorageContract sellStorageContract;
        try {
            sellStorageContract = this.contract.unpack(SellStorageContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        byte[] ownerAddress = sellStorageContract.getOwnerAddress().toByteArray();
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        if (accountWrapper == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }

        long bytes = sellStorageContract.getStorageBytes();
        if (bytes <= 0) {
            throw new ContractValidateException("bytes must be positive");
        }

        long currentStorageLimit = accountWrapper.getStorageLimit();
        long currentUnusedStorage = currentStorageLimit - accountWrapper.getStorageUsage();

        if (bytes > currentUnusedStorage) {
            throw new ContractValidateException(
                    "bytes must be less than currentUnusedStorage[" + currentUnusedStorage + "]");
        }

        long quantity = storageMarket.trySellStorage(bytes);
        if (quantity <= 1_000_000L) {
            throw new ContractValidateException(
                    "quantity must be larger than 1GSC,current quantity[" + quantity + "]");
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(SellStorageContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
