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

package org.gsc.db.accountstate;

import com.google.protobuf.ByteString;
import com.google.protobuf.Internal;

import java.util.Arrays;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BlockWrapper;
import org.springframework.stereotype.Component;
import org.gsc.crypto.Hash;
import org.gsc.db.Manager;
import org.gsc.db.accountstate.storetrie.AccountStateStoreTrie;

@Slf4j(topic = "AccountState")
@Component
public class TrieService {

    @Setter
    private Manager manager;

    @Setter
    private AccountStateStoreTrie accountStateStoreTrie;

    public byte[] getFullAccountStateRootHash() {
        long latestNumber = manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
        return getAccountStateRootHash(latestNumber);
    }

    public byte[] getConfirmedAccountStateRootHash() {
        long latestConfirmedNumber = manager.getDynamicPropertiesStore().getLatestConfirmedBlockNum();
        return getAccountStateRootHash(latestConfirmedNumber);
    }

    private byte[] getAccountStateRootHash(long blockNumber) {
        long latestNumber = blockNumber;
        byte[] rootHash = null;
        try {
            BlockWrapper blockWrapper = manager.getBlockByNum(latestNumber);
            ByteString value = blockWrapper.getInstance().getBlockHeader().getRawData()
                    .getAccountStateRoot();
            rootHash = value == null ? null : value.toByteArray();
            if (Arrays.equals(rootHash, Internal.EMPTY_BYTE_ARRAY)) {
                rootHash = Hash.EMPTY_TRIE_HASH;
            }
        } catch (Exception e) {
            logger.error("Get the {} block error.", latestNumber, e);
        }
        return rootHash;
    }
}
