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

import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Sha256Hash;
import org.gsc.db.KhaosDatabase.KhaosBlock;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.StoreException;

@Slf4j(topic = "DB")
@Component
public class TransactionStore extends GSCStoreWithRevoking<TransactionWrapper> {

    @Autowired
    private BlockStore blockStore;

    @Autowired
    private KhaosDatabase khaosDatabase;

    @Autowired
    private TransactionStore(@Value("transaction") String dbName) {
        super(dbName);
    }

    @Override
    public void put(byte[] key, TransactionWrapper item) {
        if (Objects.isNull(item) || item.getBlockNum() == -1) {
            super.put(key, item);
        } else {
            revokingDB.put(key, ByteArray.fromLong(item.getBlockNum()));
        }

        if (Objects.nonNull(indexHelper)) {
            indexHelper.update(item.getInstance());
        }
    }

    private TransactionWrapper getTransactionFromBlockStore(byte[] key, long blockNum) {
        List<BlockWrapper> blocksList = blockStore.getLimitNumber(blockNum, 1);
        if (blocksList.size() != 0) {
            for (TransactionWrapper e : blocksList.get(0).getTransactions()) {
                if (e.getTransactionId().equals(Sha256Hash.wrap(key))) {
                    return e;
                }
            }
        }
        return null;
    }

    private TransactionWrapper getTransactionFromKhaosDatabase(byte[] key, long high) {
        List<KhaosBlock> khaosBlocks = khaosDatabase.getMiniStore().getBlockByNum(high);
        for (KhaosBlock bl : khaosBlocks) {
            for (TransactionWrapper e : bl.getBlk().getTransactions()) {
                if (e.getTransactionId().equals(Sha256Hash.wrap(key))) {
                    return e;
                }
            }
        }
        return null;
    }

    public long getBlockNumber(byte[] key) throws BadItemException {
        byte[] value = revokingDB.getUnchecked(key);
        if (ArrayUtils.isEmpty(value)) {
            return -1;
        }

        if (value.length == 8) {
            return ByteArray.toLong(value);
        }
        TransactionWrapper transactionWrapper = new TransactionWrapper(value);
        return transactionWrapper.getBlockNum();
    }

    @Override
    public TransactionWrapper get(byte[] key) throws BadItemException {
        byte[] value = revokingDB.getUnchecked(key);
        if (ArrayUtils.isEmpty(value)) {
            return null;
        }
        TransactionWrapper transactionWrapper = null;
        if (value.length == 8) {
            long blockHigh = ByteArray.toLong(value);
            transactionWrapper = getTransactionFromBlockStore(key, blockHigh);
            if (transactionWrapper == null) {
                transactionWrapper = getTransactionFromKhaosDatabase(key, blockHigh);
            }
        }

        return transactionWrapper == null ? new TransactionWrapper(value) : transactionWrapper;
    }

    @Override
    public TransactionWrapper getUnchecked(byte[] key) {
        try {
            return get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * get total transaction.
     */
    @Deprecated
    public long getTotalTransactions() {
        return 0; //Streams.stream(iterator()).count();
    }

    @Override
    public void delete(byte[] key) {
        deleteIndex(key);
        super.delete(key);
    }

    private void deleteIndex(byte[] key) {
        if (Objects.nonNull(indexHelper)) {
            TransactionWrapper item;
            try {
                item = get(key);
                if (Objects.nonNull(item)) {
                    indexHelper.remove(item.getInstance());
                }
            } catch (StoreException e) {
                logger.error("deleteIndex: ", e);
            }
        }
    }
}
