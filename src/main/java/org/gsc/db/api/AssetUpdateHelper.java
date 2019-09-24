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

package org.gsc.db.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.*;
import org.gsc.utils.ByteArray;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j(topic = "DB")
public class AssetUpdateHelper {

    private Manager dbManager;

    private HashMap<String, byte[]> assetNameToIdMap = new HashMap<>();

    public AssetUpdateHelper(Manager dbManager) {
        this.dbManager = dbManager;
    }

    public void doWork() {
        long start = System.currentTimeMillis();
        logger.info("Start updating the asset");
        init();
        updateAsset();
        updateExchange();
        updateAccount();
        finish();
        logger.info(
                "Complete the asset update,Total time：{} milliseconds", System.currentTimeMillis() - start);
    }

    public void init() {
        if (dbManager.getAssetIssueV2Store().iterator().hasNext()) {
            logger.warn("AssetIssueV2Store is not empty");
        }
        dbManager.getAssetIssueV2Store().reset();
        if (dbManager.getExchangeV2Store().iterator().hasNext()) {
            logger.warn("ExchangeV2Store is not empty");
        }
        dbManager.getExchangeV2Store().reset();
        dbManager.getDynamicPropertiesStore().saveTokenIdNum(1000000L);
    }

    public List<AssetIssueWrapper> getAllAssetIssues() {

        List<AssetIssueWrapper> result = new ArrayList<>();

        long latestBlockHeaderNumber =
                dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
        long blockNum = 1;
        while (blockNum <= latestBlockHeaderNumber) {
            if (blockNum % 100000 == 0) {
                logger.info("The number of block that have processed：{}", blockNum);
            }
            try {
                BlockWrapper block = dbManager.getBlockByNum(blockNum);
                for (TransactionWrapper transaction : block.getTransactions()) {
                    if (transaction.getInstance().getRawData().getContract(0).getType()
                            == ContractType.AssetIssueContract) {
                        AssetIssueContract obj =
                                transaction
                                        .getInstance()
                                        .getRawData()
                                        .getContract(0)
                                        .getParameter()
                                        .unpack(AssetIssueContract.class);

                        AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(obj);

                        result.add(dbManager.getAssetIssueStore().get(assetIssueWrapper.createDbKey()));
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Block not exists,num:" + blockNum);
            }

            blockNum++;
        }
        logger.info("Total block：{}", blockNum);

        if (dbManager.getAssetIssueStore().getAllAssetIssues().size() != result.size()) {
            throw new RuntimeException("Asset num is wrong!");
        }

        return result;
    }

    public void updateAsset() {
        long tokenIdNum = dbManager.getDynamicPropertiesStore().getTokenIdNum();
        long count = 0;

        List<AssetIssueWrapper> assetIssueWrapperList = getAllAssetIssues();
        for (AssetIssueWrapper assetIssueWrapper : assetIssueWrapperList) {
            tokenIdNum++;
            count++;

            assetIssueWrapper.setId(Long.toString(tokenIdNum));
            dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);
            assetIssueWrapper.setPrecision(0);
            dbManager.getAssetIssueV2Store().put(assetIssueWrapper.createDbV2Key(), assetIssueWrapper);

            assetNameToIdMap.put(
                    ByteArray.toStr(assetIssueWrapper.createDbKey()), assetIssueWrapper.createDbV2Key());
        }
        dbManager.getDynamicPropertiesStore().saveTokenIdNum(tokenIdNum);

        logger.info("Complete the asset store update,Total assets：{}", count);
    }

    public void updateExchange() {
        long count = 0;

        for (ExchangeWrapper exchangeWrapper : dbManager.getExchangeStore().getAllExchanges()) {
            count++;
            if (!Arrays.equals(exchangeWrapper.getFirstTokenId(), "_".getBytes())) {
                exchangeWrapper.setFirstTokenId(
                        assetNameToIdMap.get(ByteArray.toStr(exchangeWrapper.getFirstTokenId())));
            }

            if (!Arrays.equals(exchangeWrapper.getSecondTokenId(), "_".getBytes())) {
                exchangeWrapper.setSecondTokenId(
                        assetNameToIdMap.get(ByteArray.toStr(exchangeWrapper.getSecondTokenId())));
            }

            dbManager.getExchangeV2Store().put(exchangeWrapper.createDbKey(), exchangeWrapper);
        }

        logger.info("Complete the exchange store update,Total exchanges：{}", count);
    }

    public void updateAccount() {
        long count = 0;

        Iterator<Entry<byte[], AccountWrapper>> iterator = dbManager.getAccountStore().iterator();
        while (iterator.hasNext()) {
            AccountWrapper accountWrapper = iterator.next().getValue();

            accountWrapper.clearAssetV2();
            if (accountWrapper.getAssetMap().size() != 0) {
                HashMap<String, Long> map = new HashMap<>();
                for (Map.Entry<String, Long> entry : accountWrapper.getAssetMap().entrySet()) {
                    map.put(ByteArray.toStr(assetNameToIdMap.get(entry.getKey())), entry.getValue());
                }

                accountWrapper.addAssetMapV2(map);
            }

            accountWrapper.clearFreeAssetNetUsageV2();
            if (accountWrapper.getAllFreeAssetNetUsage().size() != 0) {
                HashMap<String, Long> map = new HashMap<>();
                for (Map.Entry<String, Long> entry : accountWrapper.getAllFreeAssetNetUsage().entrySet()) {
                    map.put(ByteArray.toStr(assetNameToIdMap.get(entry.getKey())), entry.getValue());
                }
                accountWrapper.addAllFreeAssetNetUsageV2(map);
            }

            accountWrapper.clearLatestAssetOperationTimeV2();
            if (accountWrapper.getLatestAssetOperationTimeMap().size() != 0) {
                HashMap<String, Long> map = new HashMap<>();
                for (Map.Entry<String, Long> entry :
                        accountWrapper.getLatestAssetOperationTimeMap().entrySet()) {
                    map.put(ByteArray.toStr(assetNameToIdMap.get(entry.getKey())), entry.getValue());
                }
                accountWrapper.addAllLatestAssetOperationTimeV2(map);
            }

            if (!accountWrapper.getAssetIssuedName().isEmpty()) {
                accountWrapper.setAssetIssuedID(
                        assetNameToIdMap.get(
                                ByteArray.toStr(accountWrapper.getAssetIssuedName().toByteArray())));
            }

            dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

            if (count % 50000 == 0) {
                logger.info("The number of accounts that have completed the update ：{}", count);
            }
            count++;
        }

        logger.info("Complete the account store update,Total assets：{}", count);
    }

    public void finish() {
        dbManager.getDynamicPropertiesStore().saveTokenUpdateDone(1);
        assetNameToIdMap.clear();
    }
}
