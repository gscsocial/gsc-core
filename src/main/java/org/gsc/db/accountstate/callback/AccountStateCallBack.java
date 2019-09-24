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

package org.gsc.db.accountstate.callback;

import com.google.protobuf.ByteString;
import com.google.protobuf.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.BlockWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.crypto.Hash;
import org.gsc.utils.ByteUtil;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.utils.RLP;
import org.gsc.db.Manager;
import org.gsc.db.accountstate.AccountStateEntity;
import org.gsc.db.accountstate.storetrie.AccountStateStoreTrie;
import org.gsc.core.exception.BadBlockException;
import org.gsc.trie.TrieImpl;
import org.gsc.trie.TrieImpl.Node;
import org.gsc.trie.TrieImpl.ScanAction;

@Slf4j(topic = "AccountState")
@Component
public class AccountStateCallBack {

    private BlockWrapper blockWrapper;
    private volatile boolean execute = false;
    private volatile boolean allowGenerateRoot = false;
    private TrieImpl trie;

    @Setter
    private Manager manager;

    @Autowired
    private AccountStateStoreTrie db;

    private List<TrieEntry> trieEntryList = new ArrayList<>();

    private static class TrieEntry {

        private byte[] key;
        private byte[] data;

        public byte[] getKey() {
            return key;
        }

        public TrieEntry setKey(byte[] key) {
            this.key = key;
            return this;
        }

        public byte[] getData() {
            return data;
        }

        public TrieEntry setData(byte[] data) {
            this.data = data;
            return this;
        }

        public static TrieEntry build(byte[] key, byte[] data) {
            TrieEntry trieEntry = new TrieEntry();
            return trieEntry.setKey(key).setData(data);
        }
    }

    public void accountCallBack(byte[] key, AccountWrapper item) {
        if (!exe()) {
            return;
        }
        if (item == null) {
            return;
        }
        trieEntryList
                .add(TrieEntry.build(key, new AccountStateEntity(item.getInstance()).toByteArrays()));
    }

    public void preExeTrans() {
        trieEntryList.clear();
    }

    public void exeTransFinish() {
        for (TrieEntry trieEntry : trieEntryList) {
            trie.put(RLP.encodeElement(trieEntry.getKey()), trieEntry.getData());
        }
        trieEntryList.clear();
    }

    public void deleteAccount(byte[] key) {
        if (!exe()) {
            return;
        }
        trie.delete(RLP.encodeElement(key));
    }

    public void preExecute(BlockWrapper blockWrapper) {
        this.blockWrapper = blockWrapper;
        this.execute = true;
        this.allowGenerateRoot = manager.getDynamicPropertiesStore().allowAccountStateRoot();
        if (!exe()) {
            return;
        }
        byte[] rootHash = null;
        try {
            BlockWrapper parentBlockWrapper = manager.getBlockById(blockWrapper.getParentBlockId());
            rootHash = parentBlockWrapper.getInstance().getBlockHeader().getRawData()
                    .getAccountStateRoot().toByteArray();
        } catch (Exception e) {
            logger.error("", e);
        }
        if (Arrays.equals(Internal.EMPTY_BYTE_ARRAY, rootHash)) {
            rootHash = Hash.EMPTY_TRIE_HASH;
        }
        trie = new TrieImpl(db, rootHash);
    }

    public void executePushFinish() throws BadBlockException {
        if (!exe()) {
            return;
        }
        ByteString oldRoot = blockWrapper.getInstance().getBlockHeader().getRawData()
                .getAccountStateRoot();
        execute = false;
        //
        byte[] newRoot = trie.getRootHash();
        if (ArrayUtils.isEmpty(newRoot)) {
            newRoot = Hash.EMPTY_TRIE_HASH;
        }
        if (!oldRoot.isEmpty() && !Arrays.equals(oldRoot.toByteArray(), newRoot)) {
            logger.error("the accountStateRoot hash is error. {}, oldRoot: {}, newRoot: {}",
                    blockWrapper.getBlockId().getString(), ByteUtil.toHexString(oldRoot.toByteArray()),
                    ByteUtil.toHexString(newRoot));
            printErrorLog(trie);
            throw new BadBlockException("the accountStateRoot hash is error");
        }
    }

    public void executeGenerateFinish() {
        if (!exe()) {
            return;
        }
        //
        byte[] newRoot = trie.getRootHash();
        if (ArrayUtils.isEmpty(newRoot)) {
            newRoot = Hash.EMPTY_TRIE_HASH;
        }
        blockWrapper.setAccountStateRoot(newRoot);
        execute = false;
    }

    public void exceptionFinish() {
        execute = false;
    }

    private boolean exe() {
        if (!execute || !allowGenerateRoot) {
            //Agreement same block high to generate account state root
            execute = false;
            return false;
        }
        return true;
    }

    private void printErrorLog(TrieImpl trie) {
        trie.scanTree(new ScanAction() {
            @Override
            public void doOnNode(byte[] hash, Node node) {

            }

            @Override
            public void doOnValue(byte[] nodeHash, Node node, byte[] key, byte[] value) {
                try {
                    logger.info("account info : {}", AccountStateEntity.parse(value));
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        });
    }

}
