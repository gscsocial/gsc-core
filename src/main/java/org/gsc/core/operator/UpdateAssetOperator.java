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
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.utils.TransactionUtil;
import org.gsc.db.AssetIssueStore;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.AccountUpdateContract;
import org.gsc.protos.Contract.UpdateAssetContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class UpdateAssetOperator extends AbstractOperator {

    UpdateAssetOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            final UpdateAssetContract updateAssetContract = this.contract
                    .unpack(UpdateAssetContract.class);

            long newLimit = updateAssetContract.getNewLimit();
            long newPublicLimit = updateAssetContract.getNewPublicLimit();
            byte[] ownerAddress = updateAssetContract.getOwnerAddress().toByteArray();
            ByteString newUrl = updateAssetContract.getUrl();
            ByteString newDescription = updateAssetContract.getDescription();

            AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);

            AssetIssueWrapper assetIssueWrapper, assetIssueWrapperV2;

            AssetIssueStore assetIssueStoreV2 = dbManager.getAssetIssueV2Store();
            assetIssueWrapperV2 = assetIssueStoreV2.get(accountWrapper.getAssetIssuedID().toByteArray());

            assetIssueWrapperV2.setFreeAssetNetLimit(newLimit);
            assetIssueWrapperV2.setPublicFreeAssetNetLimit(newPublicLimit);
            assetIssueWrapperV2.setUrl(newUrl);
            assetIssueWrapperV2.setDescription(newDescription);

            if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
                AssetIssueStore assetIssueStore = dbManager.getAssetIssueStore();
                assetIssueWrapper = assetIssueStore.get(accountWrapper.getAssetIssuedName().toByteArray());
                assetIssueWrapper.setFreeAssetNetLimit(newLimit);
                assetIssueWrapper.setPublicFreeAssetNetLimit(newPublicLimit);
                assetIssueWrapper.setUrl(newUrl);
                assetIssueWrapper.setDescription(newDescription);

                dbManager.getAssetIssueStore()
                        .put(assetIssueWrapper.createDbKey(), assetIssueWrapper);
                dbManager.getAssetIssueV2Store()
                        .put(assetIssueWrapperV2.createDbV2Key(), assetIssueWrapperV2);
            } else {
                dbManager.getAssetIssueV2Store()
                        .put(assetIssueWrapperV2.createDbV2Key(), assetIssueWrapperV2);
            }

            ret.setStatus(fee, code.SUCESS);
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
        if (!this.contract.is(UpdateAssetContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [UpdateAssetContract],real type[" + contract
                            .getClass() + "]");
        }
        final UpdateAssetContract updateAssetContract;
        try {
            updateAssetContract = this.contract.unpack(UpdateAssetContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        long newLimit = updateAssetContract.getNewLimit();
        long newPublicLimit = updateAssetContract.getNewPublicLimit();
        byte[] ownerAddress = updateAssetContract.getOwnerAddress().toByteArray();
        ByteString newUrl = updateAssetContract.getUrl();
        ByteString newDescription = updateAssetContract.getDescription();

        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid ownerAddress");
        }

        AccountWrapper account = dbManager.getAccountStore().get(ownerAddress);
        if (account == null) {
            throw new ContractValidateException("Account has not existed");
        }

        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            if (account.getAssetIssuedName().isEmpty()) {
                throw new ContractValidateException("Account has not issue any asset");
            }

            if (dbManager.getAssetIssueStore().get(account.getAssetIssuedName().toByteArray())
                    == null) {
                throw new ContractValidateException("Asset not exists in AssetIssueStore");
            }
        } else {
            if (account.getAssetIssuedID().isEmpty()) {
                throw new ContractValidateException("Account has not issue any asset");
            }

            if (dbManager.getAssetIssueV2Store().get(account.getAssetIssuedID().toByteArray())
                    == null) {
                throw new ContractValidateException("Asset not exists  in AssetIssueV2Store");
            }
        }

        if (!TransactionUtil.validUrl(newUrl.toByteArray())) {
            throw new ContractValidateException("Invalid url");
        }

        if (!TransactionUtil.validAssetDescription(newDescription.toByteArray())) {
            throw new ContractValidateException("Invalid description");
        }

        if (newLimit < 0 || newLimit >= dbManager.getDynamicPropertiesStore().getOneDayNetLimit()) {
            throw new ContractValidateException("Invalid FreeAssetNetLimit");
        }

        if (newPublicLimit < 0 || newPublicLimit >=
                dbManager.getDynamicPropertiesStore().getOneDayNetLimit()) {
            throw new ContractValidateException("Invalid PublicFreeAssetNetLimit");
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(AccountUpdateContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }
}
