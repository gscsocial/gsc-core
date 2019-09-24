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
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.Manager;
import org.gsc.db.StorageMarket;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.BuyStorageBytesContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class BuyStorageBytesOperator extends AbstractOperator {

    private StorageMarket storageMarket;

    BuyStorageBytesOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
        storageMarket = new StorageMarket(dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        final BuyStorageBytesContract BuyStorageBytesContract;
        try {
            BuyStorageBytesContract = contract.unpack(BuyStorageBytesContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }

        AccountWrapper accountWrapper = dbManager.getAccountStore()
                .get(BuyStorageBytesContract.getOwnerAddress().toByteArray());
        long bytes = BuyStorageBytesContract.getBytes();

        storageMarket.buyStorageBytes(accountWrapper, bytes);

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
        if (!contract.is(BuyStorageBytesContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [BuyStorageBytesContract],real type[" + contract
                            .getClass() + "]");
        }

        final BuyStorageBytesContract BuyStorageBytesContract;
        try {
            BuyStorageBytesContract = this.contract.unpack(BuyStorageBytesContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        byte[] ownerAddress = BuyStorageBytesContract.getOwnerAddress().toByteArray();
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        if (accountWrapper == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }

        long bytes = BuyStorageBytesContract.getBytes();
        if (bytes < 0) {
            throw new ContractValidateException("bytes must be positive");
        }

        if (bytes < 1L) {
            throw new ContractValidateException(
                    "bytes must be larger than 1, current storage_bytes[" + bytes + "]");
        }

        long quant = storageMarket.tryBuyStorageBytes(bytes);

        if (quant < 1_000_000L) {
            throw new ContractValidateException("quantity must be larger than 1GSC");
        }

        if (quant > accountWrapper.getBalance()) {
            throw new ContractValidateException("quantity must be less than accountBalance");
        }

//    long storageBytes = storageMarket.exchange(quant, true);
//    if (storageBytes > dbManager.getDynamicPropertiesStore().getTotalStorageReserved()) {
//      throw new ContractValidateException("storage is not enough");
//    }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(BuyStorageBytesContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
