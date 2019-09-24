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

package org.gsc.db.api.index;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.disk.DiskIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.gsc.utils.ByteArray;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.db.common.WrappedByteArray;
import org.gsc.db.db2.core.IGSCChainBase;
import org.gsc.protos.Contract.AssetIssueContract;

import javax.annotation.PostConstruct;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

@Component
@Slf4j(topic = "DB")
public class AssetIssueIndex extends AbstractIndex<AssetIssueWrapper, AssetIssueContract> {

    public static Attribute<WrappedByteArray, String> AssetIssue_OWNER_ADDRESS;
    public static SimpleAttribute<WrappedByteArray, String> AssetIssue_NAME;
    public static Attribute<WrappedByteArray, Long> AssetIssue_START;
    public static Attribute<WrappedByteArray, Long> AssetIssue_END;

    @Autowired
    public AssetIssueIndex(
            @Qualifier("assetIssueStore") final IGSCChainBase<AssetIssueWrapper> database) {
        super(database);
    }

    @PostConstruct
    public void init() {
        initIndex(DiskPersistence.onPrimaryKeyInFile(AssetIssue_NAME, indexPath));
        index.addIndex(DiskIndex.onAttribute(AssetIssue_OWNER_ADDRESS));
//    index.addIndex(DiskIndex.onAttribute(AssetIssue_NAME));
        index.addIndex(DiskIndex.onAttribute(AssetIssue_START));
        index.addIndex(DiskIndex.onAttribute(AssetIssue_END));
    }

    @Override
    protected void setAttribute() {
        AssetIssue_OWNER_ADDRESS =
                attribute(
                        "assetIssue owner address",
                        bytes -> {
                            AssetIssueContract assetIssue = getObject(bytes);
                            return ByteArray.toHexString(assetIssue.getOwnerAddress().toByteArray());
                        });

        AssetIssue_NAME =
                attribute("assetIssue name", bytes -> {
                    AssetIssueContract assetIssue = getObject(bytes);
                    return assetIssue.getName().toStringUtf8();
                });

        AssetIssue_START =
                attribute("assetIssue start time", bytes -> {
                    AssetIssueContract assetIssue = getObject(bytes);
                    return assetIssue.getStartTime();
                });

        AssetIssue_END =
                attribute("assetIssue end time", bytes -> {
                    AssetIssueContract assetIssue = getObject(bytes);
                    return assetIssue.getEndTime();
                });

    }
}
