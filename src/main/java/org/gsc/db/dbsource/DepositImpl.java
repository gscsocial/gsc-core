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

package org.gsc.db.dbsource;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;

import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.*;
import org.spongycastle.util.Strings;
import org.spongycastle.util.encoders.Hex;
import org.gsc.crypto.Hash;
import org.gsc.runtime.config.VMConfig;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.program.Storage;
import org.gsc.utils.ByteArray;
import org.gsc.utils.ByteUtil;
import org.gsc.utils.StringUtil;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.db.AccountStore;
import org.gsc.db.BlockStore;
import org.gsc.db.CodeStore;
import org.gsc.db.ContractStore;
import org.gsc.db.DelegatedResourceStore;
import org.gsc.db.DynamicPropertiesStore;
import org.gsc.db.Manager;
import org.gsc.db.ProposalStore;
import org.gsc.db.TransactionStore;
import org.gsc.db.VotesStore;
import org.gsc.db.WitnessStore;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.runtime.utils.MUtil;

@Slf4j(topic = "deposit")
public class DepositImpl implements Deposit {

    private static final byte[] LATEST_PROPOSAL_NUM = "LATEST_PROPOSAL_NUM".getBytes();
    private static final byte[] WITNESS_ALLOWANCE_FROZEN_TIME = "WITNESS_ALLOWANCE_FROZEN_TIME"
            .getBytes();
    private static final byte[] MAINTENANCE_TIME_INTERVAL = "MAINTENANCE_TIME_INTERVAL".getBytes();
    private static final byte[] NEXT_MAINTENANCE_TIME = "NEXT_MAINTENANCE_TIME".getBytes();

    private Manager dbManager;
    private Deposit parent = null;

    private HashMap<Key, Value> accountCache = new HashMap<>();
    private HashMap<Key, Value> transactionCache = new HashMap<>();
    private HashMap<Key, Value> blockCache = new HashMap<>();
    private HashMap<Key, Value> witnessCache = new HashMap<>();
    private HashMap<Key, Value> codeCache = new HashMap<>();
    private HashMap<Key, Value> contractCache = new HashMap<>();

    private HashMap<Key, Value> votesCache = new HashMap<>();
    private HashMap<Key, Value> proposalCache = new HashMap<>();
    private HashMap<Key, Value> dynamicPropertiesCache = new HashMap<>();
    private HashMap<Key, Storage> storageCache = new HashMap<>();
    private HashMap<Key, Value> assetIssueCache = new HashMap<>();

    private DepositImpl(Manager dbManager, DepositImpl parent) {
        init(dbManager, parent);
    }

    protected void init(Manager dbManager, DepositImpl parent) {
        this.dbManager = dbManager;
        this.parent = parent;
    }

    @Override
    public Manager getDbManager() {
        return dbManager;
    }

    private BlockStore getBlockStore() {
        return dbManager.getBlockStore();
    }

    private TransactionStore getTransactionStore() {
        return dbManager.getTransactionStore();
    }

    private ContractStore getContractStore() {
        return dbManager.getContractStore();
    }

    private WitnessStore getWitnessStore() {
        return dbManager.getWitnessStore();
    }

    private VotesStore getVotesStore() {
        return dbManager.getVotesStore();
    }

    private ProposalStore getProposalStore() {
        return dbManager.getProposalStore();
    }

    private DynamicPropertiesStore getDynamicPropertiesStore() {
        return dbManager.getDynamicPropertiesStore();
    }

    private AccountStore getAccountStore() {
        return dbManager.getAccountStore();
    }

    private CodeStore getCodeStore() {
        return dbManager.getCodeStore();
    }

    private DelegatedResourceStore getDelegatedResourceStore() {
        return dbManager.getDelegatedResourceStore();
    }

    @Override
    public Deposit newDepositChild() {
        return new DepositImpl(dbManager, this);
    }

    @Override
    public synchronized AccountWrapper createAccount(byte[] address, Protocol.AccountType type) {
        Key key = new Key(address);
        AccountWrapper account = new AccountWrapper(ByteString.copyFrom(address), type);
        accountCache.put(key, new Value(account.getData(), Type.VALUE_TYPE_CREATE));
        return account;
    }

    @Override
    public AccountWrapper createAccount(byte[] address, String accountName, AccountType type) {
        Key key = new Key(address);
        AccountWrapper account = new AccountWrapper(ByteString.copyFrom(address),
                ByteString.copyFromUtf8(accountName),
                type);

        accountCache.put(key, new Value(account.getData(), Type.VALUE_TYPE_CREATE));
        return account;
    }

    @Override
    public synchronized AccountWrapper getAccount(byte[] address) {
        Key key = new Key(address);
        if (accountCache.containsKey(key)) {
            return accountCache.get(key).getAccount();
        }

        AccountWrapper accountWrapper;
        if (parent != null) {
            accountWrapper = parent.getAccount(address);
        } else {
            accountWrapper = getAccountStore().get(address);
        }

        if (accountWrapper != null) {
            accountCache.put(key, Value.create(accountWrapper.getData()));
        }
        return accountWrapper;
    }

    @Override
    public byte[] getBlackHoleAddress() {
        // using dbManager directly, black hole address should not be changed
        // when executing smart contract.
        return getAccountStore().getBlackhole().getAddress().toByteArray();
    }

    @Override
    public WitnessWrapper getWitness(byte[] address) {
        Key key = new Key(address);
        if (witnessCache.containsKey(key)) {
            return witnessCache.get(key).getWitness();
        }

        WitnessWrapper witnessWrapper;
        if (parent != null) {
            witnessWrapper = parent.getWitness(address);
        } else {
            witnessWrapper = getWitnessStore().get(address);
        }

        if (witnessWrapper != null) {
            witnessCache.put(key, Value.create(witnessWrapper.getData()));
        }
        return witnessWrapper;
    }


    @Override
    public synchronized VotesWrapper getVotesWrapper(byte[] address) {
        Key key = new Key(address);
        if (votesCache.containsKey(key)) {
            return votesCache.get(key).getVotes();
        }

        VotesWrapper votesWrapper;
        if (parent != null) {
            votesWrapper = parent.getVotesWrapper(address);
        } else {
            votesWrapper = getVotesStore().get(address);
        }

        if (votesWrapper != null) {
            votesCache.put(key, Value.create(votesWrapper.getData()));
        }
        return votesWrapper;
    }


    @Override
    public synchronized ProposalWrapper getProposalWrapper(byte[] id) {
        Key key = new Key(id);
        if (proposalCache.containsKey(key)) {
            return proposalCache.get(key).getProposal();
        }

        ProposalWrapper proposalWrapper;
        if (parent != null) {
            proposalWrapper = parent.getProposalWrapper(id);
        } else {
            try {
                proposalWrapper = getProposalStore().get(id);
            } catch (ItemNotFoundException e) {
                logger.warn("Not found proposal, id:" + Hex.toHexString(id));
                proposalWrapper = null;
            }
        }

        if (proposalWrapper != null) {
            proposalCache.put(key, Value.create(proposalWrapper.getData()));
        }
        return proposalWrapper;
    }

    // just for depositRoot
    @Override
    public void deleteContract(byte[] address) {
        getCodeStore().delete(address);
        getAccountStore().delete(address);
        getContractStore().delete(address);
    }

    @Override
    public synchronized void createContract(byte[] address, ContractWrapper contractWrapper) {
        Key key = Key.create(address);
        Value value = Value.create(contractWrapper.getData(), Type.VALUE_TYPE_CREATE);
        contractCache.put(key, value);
    }

    @Override
    public void updateContract(byte[] address, ContractWrapper contractWrapper) {
        Key key = Key.create(address);
        Value value = Value.create(contractWrapper.getData(), Type.VALUE_TYPE_DIRTY);
        contractCache.put(key, value);
    }

    @Override
    public void updateAccount(byte[] address, AccountWrapper accountWrapper) {
        Key key = Key.create(address);
        Value value = Value.create(accountWrapper.getData(), Type.VALUE_TYPE_DIRTY);
        accountCache.put(key, value);
    }

    @Override
    public synchronized ContractWrapper getContract(byte[] address) {
        Key key = Key.create(address);
        if (contractCache.containsKey(key)) {
            return contractCache.get(key).getContract();
        }

        ContractWrapper contractWrapper;
        if (parent != null) {
            contractWrapper = parent.getContract(address);
        } else {
            contractWrapper = getContractStore().get(address);
        }

        if (contractWrapper != null) {
            contractCache.put(key, Value.create(contractWrapper.getData()));
        }
        return contractWrapper;
    }

    @Override
    public synchronized void saveCode(byte[] address, byte[] code) {
        Key key = Key.create(address);
        Value value = Value.create(code, Type.VALUE_TYPE_CREATE);
        codeCache.put(key, value);

        if (VMConfig.allowGvmConstantinople()) {
            ContractWrapper contract = getContract(address);
            byte[] codeHash = Hash.sha3(code);
            contract.setCodeHash(codeHash);
            updateContract(address, contract);
        }
    }

    @Override
    public synchronized byte[] getCode(byte[] address) {
        Key key = Key.create(address);
        if (codeCache.containsKey(key)) {
            return codeCache.get(key).getCode().getData();
        }

        byte[] code;
        if (parent != null) {
            code = parent.getCode(address);
        } else {
            if (null == getCodeStore().get(address)) {
                code = null;
            } else {
                code = getCodeStore().get(address).getData();
            }
        }
        if (code != null) {
            codeCache.put(key, Value.create(code));
        }
        return code;
    }

    @Override
    public synchronized Storage getStorage(byte[] address) {
        Key key = Key.create(address);
        if (storageCache.containsKey(key)) {
            return storageCache.get(key);
        }
        Storage storage;
        if (this.parent != null) {
            Storage parentStorage = parent.getStorage(address);
//            if (VMConfig.getCpuLimitHardFork()) {
                // deep copy
                storage = new Storage(parentStorage);
//            } else {
//                storage = parentStorage;
//            }
        } else {
            storage = new Storage(address, dbManager.getStorageRowStore());
        }
        ContractWrapper contract = getContract(address);
        if (contract != null && !ByteUtil.isNullOrZeroArray(contract.getTrxHash())) {
            storage.generateAddrHash(contract.getTrxHash());
        }
        return storage;
    }

    @Override
    public synchronized AssetIssueWrapper getAssetIssue(byte[] tokenId) {
        byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);
        Key key = Key.create(tokenIdWithoutLeadingZero);
        if (assetIssueCache.containsKey(key)) {
            return assetIssueCache.get(key).getAssetIssue();
        }

        AssetIssueWrapper assetIssueWrapper;
        if (this.parent != null) {
            assetIssueWrapper = parent.getAssetIssue(tokenIdWithoutLeadingZero);
        } else {
            assetIssueWrapper = this.dbManager.getAssetIssueStoreFinal().get(tokenIdWithoutLeadingZero);
        }
        if (assetIssueWrapper != null) {
            assetIssueCache.put(key, Value.create(assetIssueWrapper.getData()));
        }
        return assetIssueWrapper;
    }

    @Override
    public synchronized void putStorageValue(byte[] address, DataWord key, DataWord value) {
        address = MUtil.convertToGSCAddress(address);
        if (getAccount(address) == null) {
            return;
        }
        Key addressKey = Key.create(address);
        Storage storage;
        if (storageCache.containsKey(addressKey)) {
            storage = storageCache.get(addressKey);
        } else {
            storage = getStorage(address);
            storageCache.put(addressKey, storage);
        }
        storage.put(key, value);
    }

    @Override
    public synchronized DataWord getStorageValue(byte[] address, DataWord key) {
        address = MUtil.convertToGSCAddress(address);
        if (getAccount(address) == null) {
            return null;
        }
        Key addressKey = Key.create(address);
        Storage storage;
        if (storageCache.containsKey(addressKey)) {
            storage = storageCache.get(addressKey);
        } else {
            storage = getStorage(address);
            storageCache.put(addressKey, storage);
        }
        return storage.getValue(key);
    }

    @Override
    public synchronized long getBalance(byte[] address) {
        AccountWrapper accountWrapper = getAccount(address);
        return accountWrapper == null ? 0L : accountWrapper.getBalance();
    }

    @Override
    public synchronized long addTokenBalance(byte[] address, byte[] tokenId, long value) {
        byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);
        AccountWrapper accountWrapper = getAccount(address);
        if (accountWrapper == null) {
            accountWrapper = createAccount(address, AccountType.Normal);
        }
        long balance = accountWrapper.getAssetMapV2()
                .getOrDefault(new String(tokenIdWithoutLeadingZero), new Long(0));
        if (value == 0) {
            return balance;
        }

        if (value < 0 && balance < -value) {
            throw new RuntimeException(
                    StringUtil.createReadableString(accountWrapper.createDbKey())
                            + " insufficient balance");
        }
        if (value >= 0) {
            accountWrapper.addAssetAmountV2(tokenIdWithoutLeadingZero, value, this.dbManager);
        } else {
            accountWrapper.reduceAssetAmountV2(tokenIdWithoutLeadingZero, -value, this.dbManager);
        }
//    accountWrapper.getAssetMap().put(new String(tokenIdWithoutLeadingZero), Math.addExact(balance, value));
        Key key = Key.create(address);
        Value V = Value.create(accountWrapper.getData(),
                Type.VALUE_TYPE_DIRTY | accountCache.get(key).getType().getType());
        accountCache.put(key, V);
//    accountWrapper.addAssetAmount(tokenIdWithoutLeadingZero, value);
        return accountWrapper.getAssetMapV2().get(new String(tokenIdWithoutLeadingZero));
    }

    @Override
    public synchronized long addBalance(byte[] address, long value) {
        AccountWrapper accountWrapper = getAccount(address);
        if (accountWrapper == null) {
            accountWrapper = createAccount(address, Protocol.AccountType.Normal);
        }

        long balance = accountWrapper.getBalance();
        if (value == 0) {
            return balance;
        }

        if (value < 0 && balance < -value) {
            throw new RuntimeException(
                    StringUtil.createReadableString(accountWrapper.createDbKey())
                            + " insufficient balance");
        }
        accountWrapper.setBalance(Math.addExact(balance, value));
        Key key = Key.create(address);
        Value val = Value.create(accountWrapper.getData(),
                Type.VALUE_TYPE_DIRTY | accountCache.get(key).getType().getType());
        accountCache.put(key, val);
        return accountWrapper.getBalance();
    }

    /**
     * @param address address
     * @param tokenId tokenIdstr in assetV2map is a string like "1000001". So before using this
     *                function, we need to do some conversion. usually we will use a DataWord as input. so the byte
     *                tokenId should be like DataWord.shortHexWithoutZeroX().getbytes().
     */
    @Override
    public synchronized long getTokenBalance(byte[] address, byte[] tokenId) {
        AccountWrapper accountWrapper = getAccount(address);
        if (accountWrapper == null) {
            return 0;
        }
        String tokenStr = new String(ByteUtil.stripLeadingZeroes(tokenId));
        return accountWrapper.getAssetMapV2().getOrDefault(tokenStr, 0L);
    }

    @Override
    public TransactionWrapper getTransaction(byte[] trxHash) {
        Key key = Key.create(trxHash);
        if (transactionCache.containsKey(key)) {
            return transactionCache.get(key).getTransaction();
        }

        TransactionWrapper transactionWrapper;
        if (parent != null) {
            transactionWrapper = parent.getTransaction(trxHash);
        } else {
            try {
                transactionWrapper = getTransactionStore().get(trxHash);
            } catch (BadItemException e) {
                transactionWrapper = null;
            }
        }

        if (transactionWrapper != null) {
            transactionCache.put(key, Value.create(transactionWrapper.getData()));
        }
        return transactionWrapper;
    }

    @Override
    public BlockWrapper getBlock(byte[] blockHash) {
        Key key = Key.create(blockHash);
        if (blockCache.containsKey(key)) {
            return blockCache.get(key).getBlock();
        }

        BlockWrapper ret;
        try {
            if (parent != null) {
                ret = parent.getBlock(blockHash);
            } else {
                ret = getBlockStore().get(blockHash);
            }
        } catch (Exception e) {
            ret = null;
        }

        if (ret != null) {
            blockCache.put(key, Value.create(ret.getData()));
        }
        return ret;
    }

    @Override
    public void putAccount(Key key, Value value) {
        accountCache.put(key, value);
    }

    @Override
    public void putTransaction(Key key, Value value) {
        transactionCache.put(key, value);
    }

    @Override
    public void putBlock(Key key, Value value) {
        blockCache.put(key, value);
    }

    @Override
    public void putWitness(Key key, Value value) {
        witnessCache.put(key, value);
    }

    @Override
    public void putCode(Key key, Value value) {
        codeCache.put(key, value);
    }

    @Override
    public void putContract(Key key, Value value) {
        contractCache.put(key, value);
    }

    @Override
    public void putStorage(Key key, Storage cache) {
        storageCache.put(key, cache);
    }

    @Override
    public void putVotes(Key key, Value value) {
        votesCache.put(key, value);
    }

    @Override
    public void putProposal(Key key, Value value) {
        proposalCache.put(key, value);
    }

    @Override
    public void putDynamicProperties(Key key, Value value) {
        dynamicPropertiesCache.put(key, value);
    }

    @Override
    public long getLatestProposalNum() {
        return Longs.fromByteArray(getDynamic(LATEST_PROPOSAL_NUM).getData());
    }

    @Override
    public long getWitnessAllowanceFrozenTime() {
        byte[] frozenTime = getDynamic(WITNESS_ALLOWANCE_FROZEN_TIME).getData();
        if (frozenTime.length >= 8) {
            return Longs.fromByteArray(getDynamic(WITNESS_ALLOWANCE_FROZEN_TIME).getData());
        }

        byte[] result = new byte[8];
        System.arraycopy(frozenTime, 0, result, 8 - frozenTime.length, frozenTime.length);
        return Longs.fromByteArray(result);

    }

    @Override
    public long getMaintenanceTimeInterval() {
        return Longs.fromByteArray(getDynamic(MAINTENANCE_TIME_INTERVAL).getData());
    }

    @Override
    public long getNextMaintenanceTime() {
        return Longs.fromByteArray(getDynamic(NEXT_MAINTENANCE_TIME).getData());
    }

    public BytesWrapper getDynamic(byte[] word) {
        Key key = Key.create(word);
        if (dynamicPropertiesCache.containsKey(key)) {
            return dynamicPropertiesCache.get(key).getDynamicProperties();
        }

        BytesWrapper bytesWrapper;
        if (parent != null) {
            bytesWrapper = parent.getDynamic(word);
        } else {
            try {
                bytesWrapper = getDynamicPropertiesStore().get(word);
            } catch (BadItemException | ItemNotFoundException e) {
                logger.warn("Not found dynamic property:" + Strings.fromUTF8ByteArray(word));
                bytesWrapper = null;
            }
        }

        if (bytesWrapper != null) {
            dynamicPropertiesCache.put(key, Value.create(bytesWrapper.getData()));
        }
        return bytesWrapper;
    }

    private void commitAccountCache(Deposit deposit) {
        accountCache.forEach((key, value) -> {
            if (value.getType().isCreate() || value.getType().isDirty()) {
                if (deposit != null) {
                    deposit.putAccount(key, value);
                } else {
                    getAccountStore().put(key.getData(), value.getAccount());
                }
            }
        });
    }

    private void commitTransactionCache(Deposit deposit) {
        transactionCache.forEach((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putTransaction(key, value);
                } else {
                    getTransactionStore().put(key.getData(), value.getTransaction());
                }
            }
        });
    }

    private void commitBlockCache(Deposit deposit) {
        blockCache.forEach(((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putBlock(key, value);
                } else {
                    getBlockStore().put(key.getData(), value.getBlock());
                }
            }
        }));
    }

    private void commitWitnessCache(Deposit deposit) {
        witnessCache.forEach(((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putWitness(key, value);
                } else {
                    getWitnessStore().put(key.getData(), value.getWitness());
                }
            }
        }));
    }

    private void commitCodeCache(Deposit deposit) {
        codeCache.forEach(((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putCode(key, value);
                } else {
                    getCodeStore().put(key.getData(), value.getCode());
                }
            }
        }));
    }

    private void commitContractCache(Deposit deposit) {
        contractCache.forEach(((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putContract(key, value);
                } else {
                    getContractStore().put(key.getData(), value.getContract());
                }
            }
        }));
    }

    private void commitStorageCache(Deposit deposit) {
        storageCache.forEach((Key address, Storage storage) -> {
            if (deposit != null) {
                // write to parent cache
                deposit.putStorage(address, storage);
            } else {
                // persistence
                storage.commit();
            }
        });

    }

    private void commitVoteCache(Deposit deposit) {
        votesCache.forEach(((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putVotes(key, value);
                } else {
                    getVotesStore().put(key.getData(), value.getVotes());
                }
            }
        }));
    }

    private void commitProposalCache(Deposit deposit) {
        proposalCache.forEach(((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putProposal(key, value);
                } else {
                    getProposalStore().put(key.getData(), value.getProposal());
                }
            }
        }));
    }

    private void commitDynamicPropertiesCache(Deposit deposit) {
        dynamicPropertiesCache.forEach(((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putDynamicProperties(key, value);
                } else {
                    getDynamicPropertiesStore().put(key.getData(), value.getDynamicProperties());
                }
            }
        }));
    }


    @Override
    public void putAccountValue(byte[] address, AccountWrapper accountWrapper) {
        Key key = new Key(address);
        accountCache.put(key, new Value(accountWrapper.getData(), Type.VALUE_TYPE_CREATE));
    }

    @Override
    public void putVoteValue(byte[] address, VotesWrapper votesWrapper) {
        Key key = new Key(address);
        votesCache.put(key, new Value(votesWrapper.getData(), Type.VALUE_TYPE_CREATE));
    }

    @Override
    public void putProposalValue(byte[] address, ProposalWrapper proposalWrapper) {
        Key key = new Key(address);
        proposalCache.put(key, new Value(proposalWrapper.getData(), Type.VALUE_TYPE_CREATE));
    }

    @Override
    public void putDynamicPropertiesWithLatestProposalNum(long num) {
        Key key = new Key(LATEST_PROPOSAL_NUM);
        dynamicPropertiesCache.put(key,
                new Value(new BytesWrapper(ByteArray.fromLong(num)).getData(), Type.VALUE_TYPE_CREATE));
    }

    @Override
    public synchronized void commit() {
        Deposit deposit = null;
        if (parent != null) {
            deposit = parent;
        }

        commitAccountCache(deposit);
        commitTransactionCache(deposit);
        commitBlockCache(deposit);
        commitWitnessCache(deposit);
        commitCodeCache(deposit);
        commitContractCache(deposit);
        commitStorageCache(deposit);
        commitVoteCache(deposit);
        commitProposalCache(deposit);
        commitDynamicPropertiesCache(deposit);
    }


    @Override
    public void setParent(Deposit deposit) {
        parent = deposit;
    }

    public static DepositImpl createRoot(Manager dbManager) {
        return new DepositImpl(dbManager, null);
    }
}
