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

package org.gsc.db;

import static org.gsc.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;

import com.google.protobuf.ByteString;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.utils.ByteArray;
import org.gsc.core.Constant;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.TooBigTransactionResultException;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol.Transaction.Contract;

@Slf4j(topic = "DB")
public class NetProcessor extends ResourceProcessor {

    public NetProcessor(Manager manager) {
        super(manager);
    }

    @Override
    public void updateUsage(AccountWrapper accountWrapper) {
        long now = dbManager.getWitnessController().getHeadSlot();
        updateUsage(accountWrapper, now);
    }

    private void updateUsage(AccountWrapper accountWrapper, long now) {
        long oldNetUsage = accountWrapper.getNetUsage();
        long latestConsumeTime = accountWrapper.getLatestConsumeTime();
        accountWrapper.setNetUsage(increase(oldNetUsage, 0, latestConsumeTime, now));
        long oldFreeNetUsage = accountWrapper.getFreeNetUsage();
        long latestConsumeFreeTime = accountWrapper.getLatestConsumeFreeTime();
        accountWrapper.setFreeNetUsage(increase(oldFreeNetUsage, 0, latestConsumeFreeTime, now));

        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            Map<String, Long> assetMap = accountWrapper.getAssetMap();
            assetMap.forEach((assetName, balance) -> {
                long oldFreeAssetNetUsage = accountWrapper.getFreeAssetNetUsage(assetName);
                long latestAssetOperationTime = accountWrapper.getLatestAssetOperationTime(assetName);
                accountWrapper.putFreeAssetNetUsage(assetName,
                        increase(oldFreeAssetNetUsage, 0, latestAssetOperationTime, now));
            });
        }
        Map<String, Long> assetMapV2 = accountWrapper.getAssetMapV2();
        assetMapV2.forEach((assetName, balance) -> {
            long oldFreeAssetNetUsage = accountWrapper.getFreeAssetNetUsageV2(assetName);
            long latestAssetOperationTime = accountWrapper.getLatestAssetOperationTimeV2(assetName);
            accountWrapper.putFreeAssetNetUsageV2(assetName,
                    increase(oldFreeAssetNetUsage, 0, latestAssetOperationTime, now));
        });
    }

    @Override
    public void consume(TransactionWrapper trx, TransactionTrace trace)
            throws ContractValidateException, AccountResourceInsufficientException, TooBigTransactionResultException {
        List<Contract> contracts = trx.getInstance().getRawData().getContractList();
        if (trx.getResultSerializedSize() > Constant.MAX_RESULT_SIZE_IN_TX * contracts.size()) {
            throw new TooBigTransactionResultException();
        }

        long bytesSize;

        if (dbManager.getDynamicPropertiesStore().supportVM()) {
            bytesSize = trx.getInstance().toBuilder().clearRet().build().getSerializedSize();
        } else {
            bytesSize = trx.getSerializedSize();
        }

        for (Contract contract : contracts) {
            if (dbManager.getDynamicPropertiesStore().supportVM()) {
                bytesSize += Constant.MAX_RESULT_SIZE_IN_TX;
            }
            logger.debug("trxId {},net cost :{}", trx.getTransactionId(), bytesSize);
            trace.setNetBill(bytesSize, 0);
            byte[] address = TransactionWrapper.getOwner(contract);
            AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
            if (accountWrapper == null) {
                throw new ContractValidateException("account not exists");
            }
            long now = dbManager.getWitnessController().getHeadSlot();

            if (contractCreateNewAccount(contract)) {
                consumeForCreateNewAccount(accountWrapper, bytesSize, now, trace);
                continue;
            }

            if (contract.getType() == TransferAssetContract && useAssetAccountNet(contract,
                    accountWrapper, now, bytesSize)) {
                continue;
            }

            if (useAccountNet(accountWrapper, bytesSize, now)) {
                continue;
            }

            if (useFreeNet(accountWrapper, bytesSize, now)) {
                continue;
            }

            if (useTransactionFee(accountWrapper, bytesSize, trace)) {
                continue;
            }

            long fee = dbManager.getDynamicPropertiesStore().getTransactionFee() * bytesSize;
            throw new AccountResourceInsufficientException(
                    "Account Insufficient net[" + bytesSize + "] and balance["
                            + fee + "] to create new account");
        }
    }

    private boolean useTransactionFee(AccountWrapper accountWrapper, long bytes,
                                      TransactionTrace trace) {
        long fee = dbManager.getDynamicPropertiesStore().getTransactionFee() * bytes;
        if (consumeFee(accountWrapper, fee)) {
            trace.setNetBill(0, fee);
            dbManager.getDynamicPropertiesStore().addTotalTransactionCost(fee);
            return true;
        } else {
            return false;
        }
    }

    private void consumeForCreateNewAccount(AccountWrapper accountWrapper, long bytes,
                                            long now, TransactionTrace trace)
            throws AccountResourceInsufficientException {
        boolean ret = consumeNetForCreateNewAccount(accountWrapper, bytes, now);

        if (!ret) {
            ret = consumeFeeForCreateNewAccount(accountWrapper, trace);
            if (!ret) {
                throw new AccountResourceInsufficientException();
            }
        }
    }

    public boolean consumeNetForCreateNewAccount(AccountWrapper accountWrapper, long bytes,
                                                 long now) {

        long createNewAccountNetRatio = dbManager.getDynamicPropertiesStore()
                .getCreateNewAccountNetRate();

        long netUsage = accountWrapper.getNetUsage();
        long latestConsumeTime = accountWrapper.getLatestConsumeTime();
        long netLimit = calculateGlobalNetLimit(accountWrapper);

        long newNetUsage = increase(netUsage, 0, latestConsumeTime, now);

        if (bytes * createNewAccountNetRatio <= (netLimit - newNetUsage)) {
            latestConsumeTime = now;
            long latestOperationTime = dbManager.getHeadBlockTimeStamp();
            newNetUsage = increase(newNetUsage, bytes * createNewAccountNetRatio, latestConsumeTime,
                    now);
            accountWrapper.setLatestConsumeTime(latestConsumeTime);
            accountWrapper.setLatestOperationTime(latestOperationTime);
            accountWrapper.setNetUsage(newNetUsage);
            dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
            return true;
        }
        return false;
    }

    public boolean consumeFeeForCreateNewAccount(AccountWrapper accountWrapper,
                                                 TransactionTrace trace) {
        long fee = dbManager.getDynamicPropertiesStore().getCreateAccountFee();
        if (consumeFee(accountWrapper, fee)) {
            trace.setNetBill(0, fee);
            dbManager.getDynamicPropertiesStore().addTotalCreateAccountCost(fee);
            return true;
        } else {
            return false;
        }
    }

    public boolean contractCreateNewAccount(Contract contract) {
        AccountWrapper toAccount;
        switch (contract.getType()) {
            case AccountCreateContract:
                return true;
            case TransferContract:
                TransferContract transferContract;
                try {
                    transferContract = contract.getParameter().unpack(TransferContract.class);
                } catch (Exception ex) {
                    throw new RuntimeException(ex.getMessage());
                }
                toAccount = dbManager.getAccountStore().get(transferContract.getToAddress().toByteArray());
                return toAccount == null;
            case TransferAssetContract:
                TransferAssetContract transferAssetContract;
                try {
                    transferAssetContract = contract.getParameter().unpack(TransferAssetContract.class);
                } catch (Exception ex) {
                    throw new RuntimeException(ex.getMessage());
                }
                toAccount = dbManager.getAccountStore()
                        .get(transferAssetContract.getToAddress().toByteArray());
                return toAccount == null;
            default:
                return false;
        }
    }


    private boolean useAssetAccountNet(Contract contract, AccountWrapper accountWrapper, long now,
                                       long bytes)
            throws ContractValidateException {

        ByteString assetName;
        try {
            assetName = contract.getParameter().unpack(TransferAssetContract.class).getAssetName();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }

        AssetIssueWrapper assetIssueWrapper, assetIssueWrapperV2;
        assetIssueWrapper = dbManager.getAssetIssueStoreFinal().get(assetName.toByteArray());
        if (assetIssueWrapper == null) {
            throw new ContractValidateException("asset not exists");
        }

        String tokenName = ByteArray.toStr(assetName.toByteArray());
        String tokenID = assetIssueWrapper.getId();
        if (assetIssueWrapper.getOwnerAddress() == accountWrapper.getAddress()) {
            return useAccountNet(accountWrapper, bytes, now);
        }

        long publicFreeAssetNetLimit = assetIssueWrapper.getPublicFreeAssetNetLimit();
        long publicFreeAssetNetUsage = assetIssueWrapper.getPublicFreeAssetNetUsage();
        long publicLatestFreeNetTime = assetIssueWrapper.getPublicLatestFreeNetTime();

        long newPublicFreeAssetNetUsage = increase(publicFreeAssetNetUsage, 0,
                publicLatestFreeNetTime, now);

        if (bytes > (publicFreeAssetNetLimit - newPublicFreeAssetNetUsage)) {
            logger.debug("The " + tokenID + " public free net is not enough");
            return false;
        }

        long freeAssetNetLimit = assetIssueWrapper.getFreeAssetNetLimit();

        long freeAssetNetUsage, latestAssetOperationTime;
        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            freeAssetNetUsage = accountWrapper
                    .getFreeAssetNetUsage(tokenName);
            latestAssetOperationTime = accountWrapper
                    .getLatestAssetOperationTime(tokenName);
        } else {
            freeAssetNetUsage = accountWrapper.getFreeAssetNetUsageV2(tokenID);
            latestAssetOperationTime = accountWrapper.getLatestAssetOperationTimeV2(tokenID);
        }

        long newFreeAssetNetUsage = increase(freeAssetNetUsage, 0,
                latestAssetOperationTime, now);

        if (bytes > (freeAssetNetLimit - newFreeAssetNetUsage)) {
            logger.debug("The " + tokenID + " free net is not enough");
            return false;
        }

        AccountWrapper issuerAccountWrapper = dbManager.getAccountStore()
                .get(assetIssueWrapper.getOwnerAddress().toByteArray());

        long issuerNetUsage = issuerAccountWrapper.getNetUsage();
        long latestConsumeTime = issuerAccountWrapper.getLatestConsumeTime();
        long issuerNetLimit = calculateGlobalNetLimit(issuerAccountWrapper);

        long newIssuerNetUsage = increase(issuerNetUsage, 0, latestConsumeTime, now);

        if (bytes > (issuerNetLimit - newIssuerNetUsage)) {
            logger.debug("The " + tokenID + " issuer'net is not enough");
            return false;
        }

        latestConsumeTime = now;
        latestAssetOperationTime = now;
        publicLatestFreeNetTime = now;
        long latestOperationTime = dbManager.getHeadBlockTimeStamp();

        newIssuerNetUsage = increase(newIssuerNetUsage, bytes, latestConsumeTime, now);
        newFreeAssetNetUsage = increase(newFreeAssetNetUsage,
                bytes, latestAssetOperationTime, now);
        newPublicFreeAssetNetUsage = increase(newPublicFreeAssetNetUsage, bytes,
                publicLatestFreeNetTime, now);

        issuerAccountWrapper.setNetUsage(newIssuerNetUsage);
        issuerAccountWrapper.setLatestConsumeTime(latestConsumeTime);

        assetIssueWrapper.setPublicFreeAssetNetUsage(newPublicFreeAssetNetUsage);
        assetIssueWrapper.setPublicLatestFreeNetTime(publicLatestFreeNetTime);

        accountWrapper.setLatestOperationTime(latestOperationTime);
        if (dbManager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            accountWrapper.putLatestAssetOperationTimeMap(tokenName,
                    latestAssetOperationTime);
            accountWrapper.putFreeAssetNetUsage(tokenName, newFreeAssetNetUsage);
            accountWrapper.putLatestAssetOperationTimeMapV2(tokenID,
                    latestAssetOperationTime);
            accountWrapper.putFreeAssetNetUsageV2(tokenID, newFreeAssetNetUsage);

            dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);

            assetIssueWrapperV2 = dbManager.getAssetIssueV2Store().get(assetIssueWrapper.createDbV2Key());
            assetIssueWrapperV2.setPublicFreeAssetNetUsage(newPublicFreeAssetNetUsage);
            assetIssueWrapperV2.setPublicLatestFreeNetTime(publicLatestFreeNetTime);
            dbManager.getAssetIssueV2Store()
                    .put(assetIssueWrapperV2.createDbV2Key(), assetIssueWrapperV2);
        } else {
            accountWrapper.putLatestAssetOperationTimeMapV2(tokenID,
                    latestAssetOperationTime);
            accountWrapper.putFreeAssetNetUsageV2(tokenID, newFreeAssetNetUsage);
            dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);
        }

        dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
        dbManager.getAccountStore().put(issuerAccountWrapper.createDbKey(),
                issuerAccountWrapper);

        return true;

    }

    public long calculateGlobalNetLimit(AccountWrapper accountWrapper) {
        long frozeBalance = accountWrapper.getAllFrozenBalanceForNet();
        if (frozeBalance < 1_000_000L) {
            return 0;
        }
        long netWeight = frozeBalance / 1_000_000L;
        long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
        long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
        if (totalNetWeight == 0) {
            return 0;
        }
        return (long) (netWeight * ((double) totalNetLimit / totalNetWeight));
    }

    private boolean useAccountNet(AccountWrapper accountWrapper, long bytes, long now) {

        long netUsage = accountWrapper.getNetUsage();
        long latestConsumeTime = accountWrapper.getLatestConsumeTime();
        long netLimit = calculateGlobalNetLimit(accountWrapper);

        long newNetUsage = increase(netUsage, 0, latestConsumeTime, now);

        if (bytes > (netLimit - newNetUsage)) {
            logger.debug("net usage is running out. now use free net usage");
            return false;
        }

        latestConsumeTime = now;
        long latestOperationTime = dbManager.getHeadBlockTimeStamp();
        newNetUsage = increase(newNetUsage, bytes, latestConsumeTime, now);
        accountWrapper.setNetUsage(newNetUsage);
        accountWrapper.setLatestOperationTime(latestOperationTime);
        accountWrapper.setLatestConsumeTime(latestConsumeTime);

        dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
        return true;
    }

    private boolean useFreeNet(AccountWrapper accountWrapper, long bytes, long now) {

        long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
        long freeNetUsage = accountWrapper.getFreeNetUsage();
        long latestConsumeFreeTime = accountWrapper.getLatestConsumeFreeTime();
        long newFreeNetUsage = increase(freeNetUsage, 0, latestConsumeFreeTime, now);

        if (bytes > (freeNetLimit - newFreeNetUsage)) {
            logger.debug("free net usage is running out");
            return false;
        }

        long publicNetLimit = dbManager.getDynamicPropertiesStore().getPublicNetLimit();
        long publicNetUsage = dbManager.getDynamicPropertiesStore().getPublicNetUsage();
        long publicNetTime = dbManager.getDynamicPropertiesStore().getPublicNetTime();

        long newPublicNetUsage = increase(publicNetUsage, 0, publicNetTime, now);

        if (bytes > (publicNetLimit - newPublicNetUsage)) {
            logger.debug("free public net usage is running out");
            return false;
        }

        latestConsumeFreeTime = now;
        long latestOperationTime = dbManager.getHeadBlockTimeStamp();
        publicNetTime = now;
        newFreeNetUsage = increase(newFreeNetUsage, bytes, latestConsumeFreeTime, now);
        newPublicNetUsage = increase(newPublicNetUsage, bytes, publicNetTime, now);
        accountWrapper.setFreeNetUsage(newFreeNetUsage);
        accountWrapper.setLatestConsumeFreeTime(latestConsumeFreeTime);
        accountWrapper.setLatestOperationTime(latestOperationTime);

        dbManager.getDynamicPropertiesStore().savePublicNetUsage(newPublicNetUsage);
        dbManager.getDynamicPropertiesStore().savePublicNetTime(publicNetTime);
        dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
        return true;

    }

}


