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



package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.gsc.utils.ByteArray;
import org.gsc.db.Manager;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Contract.AssetIssueContract.FrozenSupply;

@Slf4j(topic = "wrapper")
public class AssetIssueWrapper implements ProtoWrapper<AssetIssueContract> {

    private AssetIssueContract assetIssueContract;

    /**
     * get asset issue contract from bytes data.
     */
    public AssetIssueWrapper(byte[] data) {
        try {
            this.assetIssueContract = AssetIssueContract.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage());
        }
    }

    public AssetIssueWrapper(AssetIssueContract assetIssueContract) {
        this.assetIssueContract = assetIssueContract;
    }

    public byte[] getData() {
        return this.assetIssueContract.toByteArray();
    }

    @Override
    public AssetIssueContract getInstance() {
        return this.assetIssueContract;
    }

    @Override
    public String toString() {
        return this.assetIssueContract.toString();
    }

    public ByteString getName() {
        return this.assetIssueContract.getName();
    }

    public void setId(String id) {
        this.assetIssueContract = this.assetIssueContract.toBuilder()
                .setId(id)
                .build();
    }

    public String getId() {
        return this.assetIssueContract.getId();
    }

    public void setPrecision(int precision) {
        this.assetIssueContract = this.assetIssueContract.toBuilder()
                .setPrecision(precision)
                .build();
    }

    public int getPrecision() {
        return this.assetIssueContract.getPrecision();
    }

    public void setOrder(long order) {
        this.assetIssueContract = this.assetIssueContract.toBuilder()
                .setOrder(order)
                .build();
    }

    public long getOrder() {
        return this.assetIssueContract.getOrder();
    }

    public byte[] createDbV2Key() {
        return ByteArray.fromString(this.assetIssueContract.getId());
    }

    public byte[] createDbKey() {
//    long order = getOrder();
//    if (order == 0) {
//      return getName().toByteArray();
//    }
//    String name = new String(getName().toByteArray(), Charset.forName("UTF-8"));
//    String nameKey = createDbKeyString(name, order);
//    return nameKey.getBytes();
        return getName().toByteArray();
    }

    public byte[] createDbKeyFinal(Manager manager) {
        if (manager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            return createDbKey();
        } else {
            return createDbV2Key();
        }
    }

    public static String createDbKeyString(String name, long order) {
        return name + "_" + order;
    }

    public int getNum() {
        return this.assetIssueContract.getNum();
    }

    public int getGscNum() {
        return this.assetIssueContract.getGscNum();
    }

    public long getStartTime() {
        return this.assetIssueContract.getStartTime();
    }

    public long getEndTime() {
        return this.assetIssueContract.getEndTime();
    }

    public ByteString getOwnerAddress() {
        return this.assetIssueContract.getOwnerAddress();
    }

    public int getFrozenSupplyCount() {
        return getInstance().getFrozenSupplyCount();
    }

    public List<FrozenSupply> getFrozenSupplyList() {
        return getInstance().getFrozenSupplyList();
    }

    public long getFrozenSupply() {
        List<FrozenSupply> frozenList = getFrozenSupplyList();
        final long[] frozenBalance = {0};
        frozenList.forEach(frozen -> frozenBalance[0] = Long.sum(frozenBalance[0],
                frozen.getFrozenAmount()));
        return frozenBalance[0];
    }

    public void setFreeAssetNetLimit(long newLimit) {
        this.assetIssueContract = this.assetIssueContract.toBuilder()
                .setFreeAssetNetLimit(newLimit).build();
    }

    public long getFreeAssetNetLimit() {
        return this.assetIssueContract.getFreeAssetNetLimit();
    }

    public long getPublicFreeAssetNetLimit() {
        return this.assetIssueContract.getPublicFreeAssetNetLimit();
    }

    public void setPublicFreeAssetNetLimit(long newPublicLimit) {
        this.assetIssueContract = this.assetIssueContract.toBuilder()
                .setPublicFreeAssetNetLimit(newPublicLimit).build();
    }

    public long getPublicFreeAssetNetUsage() {
        return this.assetIssueContract.getPublicFreeAssetNetUsage();
    }

    public void setPublicFreeAssetNetUsage(long value) {
        this.assetIssueContract = this.assetIssueContract.toBuilder()
                .setPublicFreeAssetNetUsage(value).build();
    }

    public void setPublicLatestFreeNetTime(long time) {
        this.assetIssueContract = this.assetIssueContract.toBuilder()
                .setPublicLatestFreeNetTime(time).build();
    }

    public long getPublicLatestFreeNetTime() {
        return this.assetIssueContract.getPublicLatestFreeNetTime();
    }

    public void setUrl(ByteString newUrl) {
        this.assetIssueContract = this.assetIssueContract.toBuilder()
                .setUrl(newUrl).build();
    }

    public void setDescription(ByteString description) {
        this.assetIssueContract = this.assetIssueContract.toBuilder()
                .setDescription(description).build();
    }
}
