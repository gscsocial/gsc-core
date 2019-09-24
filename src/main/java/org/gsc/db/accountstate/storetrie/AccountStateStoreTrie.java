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

package org.gsc.db.accountstate.storetrie;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.BytesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.wrapper.utils.RLP;
import org.gsc.db.GSCStoreWithRevoking;
import org.gsc.db.accountstate.AccountStateEntity;
import org.gsc.db.accountstate.TrieService;
import org.gsc.db.db2.common.DB;
import org.gsc.trie.TrieImpl;

@Slf4j(topic = "AccountState")
@Component
public class AccountStateStoreTrie extends GSCStoreWithRevoking<BytesWrapper> implements
        DB<byte[], BytesWrapper> {

    @Autowired
    private TrieService trieService;

    @Autowired
    private AccountStateStoreTrie(@Value("account_trie") String dbName) {
        super(dbName);
    }

    @PostConstruct
    public void init() {
        trieService.setAccountStateStoreTrie(this);
    }

    public AccountStateEntity getAccount(byte[] key) {
        return getAccount(key, trieService.getFullAccountStateRootHash());
    }

    public AccountStateEntity getConfirmedAccount(byte[] key) {
        return getAccount(key, trieService.getConfirmedAccountStateRootHash());
    }

    public AccountStateEntity getAccount(byte[] key, byte[] rootHash) {
        TrieImpl trie = new TrieImpl(this, rootHash);
        byte[] value = trie.get(RLP.encodeElement(key));
        return ArrayUtils.isEmpty(value) ? null : AccountStateEntity.parse(value);
    }

    @Override
    public boolean isEmpty() {
        return super.size() <= 0;
    }

    @Override
    public void remove(byte[] bytes) {
        super.delete(bytes);
    }

    @Override
    public BytesWrapper get(byte[] key) {
        return super.getUnchecked(key);
    }

    @Override
    public void put(byte[] key, BytesWrapper item) {
        super.put(key, item);
    }
}
