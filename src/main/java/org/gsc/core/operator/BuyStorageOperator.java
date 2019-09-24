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
import org.gsc.protos.Contract.BuyStorageContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class BuyStorageOperator extends AbstractOperator {

    private StorageMarket storageMarket;

    BuyStorageOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
        storageMarket = new StorageMarket(dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        final BuyStorageContract buyStorageContract;
        try {
            buyStorageContract = contract.unpack(BuyStorageContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }

        AccountWrapper accountWrapper = dbManager.getAccountStore()
                .get(buyStorageContract.getOwnerAddress().toByteArray());
        long quant = buyStorageContract.getQuant();

        storageMarket.buyStorage(accountWrapper, quant);

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
        if (!contract.is(BuyStorageContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [BuyStorageContract],real type[" + contract
                            .getClass() + "]");
        }

        final BuyStorageContract buyStorageContract;
        try {
            buyStorageContract = this.contract.unpack(BuyStorageContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        byte[] ownerAddress = buyStorageContract.getOwnerAddress().toByteArray();
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        if (accountWrapper == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }

        long quant = buyStorageContract.getQuant();
        if (quant <= 0) {
            throw new ContractValidateException("quantity must be positive");
        }

        if (quant < 1000_000L) {
            throw new ContractValidateException("quantity must be larger than 1GSC");
        }

        if (quant > accountWrapper.getBalance()) {
            throw new ContractValidateException("quantity must be less than accountBalance");
        }
        long storage_bytes = storageMarket.tryBuyStorage(quant);
        if (storage_bytes < 1L) {
            throw new ContractValidateException(
                    "storage_bytes must be larger than 1,current storage_bytes[" + storage_bytes + "]");
        }
        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(BuyStorageContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
