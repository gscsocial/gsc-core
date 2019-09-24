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

import javax.annotation.Resource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.*;
import org.gsc.core.wrapper.WitnessWrapper;
import org.gsc.db.api.index.Index;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Witness;

@Slf4j(topic = "DB")
public class IndexHelper {

    @Getter
    @Resource
    private Index.Iface<Transaction> transactionIndex;
    @Getter
    @Resource
    private Index.Iface<Block> blockIndex;
    @Getter
    @Resource
    private Index.Iface<Witness> witnessIndex;
    @Getter
    @Resource
    private Index.Iface<Account> accountIndex;
    @Getter
    @Resource
    private Index.Iface<AssetIssueContract> assetIssueIndex;

    //@PostConstruct
    public void init() {
        transactionIndex.fill();
        //blockIndex.fill();
        //witnessIndex.fill();
        //accountIndex.fill();
        //assetIssueIndex.fill();
    }

    private <T> void add(Index.Iface<T> index, byte[] bytes) {
        index.add(bytes);
    }

    public void add(Transaction t) {
        add(transactionIndex, getKey(t));
    }

    public void add(Block b) {
        //add(blockIndex, getKey(b));
    }

    public void add(Witness w) {
        //add(witnessIndex, getKey(w));
    }

    public void add(Account a) {
        //add(accountIndex, getKey(a));
    }

    public void add(AssetIssueContract a) {
        //add(assetIssueIndex, getKey(a));
    }

    private <T> void update(Index.Iface<T> index, byte[] bytes) {
        index.update(bytes);
    }

    public void update(Transaction t) {
        update(transactionIndex, getKey(t));
    }

    public void update(Block b) {
        // update(blockIndex, getKey(b));
    }

    public void update(Witness w) {
        //update(witnessIndex, getKey(w));
    }

    public void update(Account a) {
        //update(accountIndex, getKey(a));
    }

    public void update(AssetIssueContract a) {
        //update(assetIssueIndex, getKey(a));
    }

    private <T> void remove(Index.Iface<T> index, byte[] bytes) {
        index.remove(bytes);
    }

    public void remove(Transaction t) {
        remove(transactionIndex, getKey(t));
    }

    public void remove(Block b) {
        //remove(blockIndex, getKey(b));
    }

    public void remove(Witness w) {
        //remove(witnessIndex, getKey(w));
    }

    public void remove(Account a) {
        //remove(accountIndex, getKey(a));
    }

    public void remove(AssetIssueContract a) {
        //remove(assetIssueIndex, getKey(a));
    }

    private byte[] getKey(Transaction t) {
        return new TransactionWrapper(t).getTransactionId().getBytes();
    }

    private byte[] getKey(Block b) {
        return new BlockWrapper(b).getBlockId().getBytes();
    }

    private byte[] getKey(Witness w) {
        return new WitnessWrapper(w).createDbKey();
    }

    private byte[] getKey(Account a) {
        return new AccountWrapper(a).createDbKey();
    }

    private byte[] getKey(AssetIssueContract a) {
        return new AssetIssueWrapper(a).createDbKey();
    }
}
