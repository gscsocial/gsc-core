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
import org.gsc.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.common.WrappedByteArray;
import org.gsc.db.db2.core.IGSCChainBase;
import org.gsc.protos.Protocol.Block;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

@Component
@Slf4j(topic = "DB")
public class BlockIndex extends AbstractIndex<BlockWrapper, Block> {

    public static SimpleAttribute<WrappedByteArray, String> Block_ID;
    public static Attribute<WrappedByteArray, Long> Block_NUMBER;
    public static Attribute<WrappedByteArray, String> TRANSACTIONS;
    public static Attribute<WrappedByteArray, Long> WITNESS_ID;
    public static Attribute<WrappedByteArray, String> WITNESS_ADDRESS;
    public static Attribute<WrappedByteArray, String> OWNERS;
    public static Attribute<WrappedByteArray, String> TOS;

    @Autowired
    public BlockIndex(
            @Qualifier("blockStore") final IGSCChainBase<BlockWrapper> database) {
        super(database);
    }

    @PostConstruct
    public void init() {
        initIndex(DiskPersistence.onPrimaryKeyInFile(Block_ID, indexPath));
//    index.addIndex(DiskIndex.onAttribute(Block_ID));
        index.addIndex(DiskIndex.onAttribute(Block_NUMBER));
        index.addIndex(DiskIndex.onAttribute(TRANSACTIONS));
        index.addIndex(DiskIndex.onAttribute(WITNESS_ID));
        index.addIndex(DiskIndex.onAttribute(WITNESS_ADDRESS));
        index.addIndex(DiskIndex.onAttribute(OWNERS));
        index.addIndex(DiskIndex.onAttribute(TOS));
    }

    @Override
    protected void setAttribute() {
        Block_ID =
                attribute("block id",
                        bytes -> {
                            Block block = getObject(bytes);
                            return new BlockWrapper(block).getBlockId().toString();
                        });
        Block_NUMBER =
                attribute("block number",
                        bytes -> {
                            Block block = getObject(bytes);
                            return block.getBlockHeader().getRawData().getNumber();
                        });
        TRANSACTIONS =
                attribute(String.class, "transactions",
                        bytes -> {
                            Block block = getObject(bytes);
                            return block.getTransactionsList().stream()
                                    .map(t -> Sha256Hash.of(t.getRawData().toByteArray()).toString())
                                    .collect(Collectors.toList());
                        });
        WITNESS_ID =
                attribute("witness id",
                        bytes -> {
                            Block block = getObject(bytes);
                            return block.getBlockHeader().getRawData().getWitnessId();
                        });
        WITNESS_ADDRESS =
                attribute("witness address",
                        bytes -> {
                            Block block = getObject(bytes);
                            return ByteArray.toHexString(
                                    block.getBlockHeader().getRawData().getWitnessAddress().toByteArray());
                        });

        OWNERS =
                attribute(String.class, "owner address",
                        bytes -> {
                            Block block = getObject(bytes);
                            return block.getTransactionsList().stream()
                                    .map(transaction -> transaction.getRawData().getContractList())
                                    .flatMap(List::stream)
                                    .map(TransactionWrapper::getOwner)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .map(ByteArray::toHexString)
                                    .collect(Collectors.toList());
                        });
        TOS =
                attribute(String.class, "to address",
                        bytes -> {
                            Block block = getObject(bytes);
                            return block.getTransactionsList().stream()
                                    .map(transaction -> transaction.getRawData().getContractList())
                                    .flatMap(List::stream)
                                    .map(TransactionWrapper::getToAddress)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .map(ByteArray::toHexString)
                                    .collect(Collectors.toList());
                        });
    }
}
