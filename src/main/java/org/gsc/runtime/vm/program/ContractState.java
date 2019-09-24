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

package org.gsc.runtime.vm.program;

import org.gsc.core.wrapper.*;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.program.invoke.ProgramInvoke;
import org.gsc.runtime.vm.program.listener.ProgramListener;
import org.gsc.runtime.vm.program.listener.ProgramListenerAware;
import org.gsc.db.dbsource.Deposit;
import org.gsc.db.dbsource.Key;
import org.gsc.db.dbsource.Value;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.AccountType;

public class ContractState implements Deposit, ProgramListenerAware {

    private Deposit deposit;
    // contract address
    private final DataWord address;
    private ProgramListener programListener;

    ContractState(ProgramInvoke programInvoke) {
        this.address = programInvoke.getContractAddress();
        this.deposit = programInvoke.getDeposit();
    }

    @Override
    public Manager getDbManager() {
        return deposit.getDbManager();
    }

    @Override
    public void setProgramListener(ProgramListener listener) {
        this.programListener = listener;
    }

    @Override
    public AccountWrapper createAccount(byte[] addr, Protocol.AccountType type) {
        return deposit.createAccount(addr, type);
    }

    @Override
    public AccountWrapper createAccount(byte[] address, String accountName, AccountType type) {
        return deposit.createAccount(address, accountName, type);
    }


    @Override
    public AccountWrapper getAccount(byte[] addr) {
        return deposit.getAccount(addr);
    }

    @Override
    public WitnessWrapper getWitness(byte[] address) {
        return deposit.getWitness(address);
    }

    @Override
    public VotesWrapper getVotesWrapper(byte[] address) {
        return deposit.getVotesWrapper(address);
    }

    @Override
    public ProposalWrapper getProposalWrapper(byte[] id) {
        return deposit.getProposalWrapper(id);
    }

    @Override
    public BytesWrapper getDynamic(byte[] bytesKey) {
        return deposit.getDynamic(bytesKey);
    }

    @Override
    public void deleteContract(byte[] address) {
        deposit.deleteContract(address);
    }

    @Override
    public void createContract(byte[] codeHash, ContractWrapper contractWrapper) {
        deposit.createContract(codeHash, contractWrapper);
    }

    @Override
    public ContractWrapper getContract(byte[] codeHash) {
        return deposit.getContract(codeHash);
    }

    @Override
    public void updateContract(byte[] address, ContractWrapper contractWrapper) {
        deposit.updateContract(address, contractWrapper);
    }

    @Override
    public void updateAccount(byte[] address, AccountWrapper accountWrapper) {
        deposit.updateAccount(address, accountWrapper);
    }

    @Override
    public void saveCode(byte[] address, byte[] code) {
        deposit.saveCode(address, code);
    }

    @Override
    public byte[] getCode(byte[] address) {
        return deposit.getCode(address);
    }

    @Override
    public void putStorageValue(byte[] addr, DataWord key, DataWord value) {
        if (canListenTrace(addr)) {
            programListener.onStoragePut(key, value);
        }
        deposit.putStorageValue(addr, key, value);
    }

    private boolean canListenTrace(byte[] address) {
        return (programListener != null) && this.address.equals(new DataWord(address));
    }

    @Override
    public DataWord getStorageValue(byte[] addr, DataWord key) {
        return deposit.getStorageValue(addr, key);
    }

    @Override
    public long getBalance(byte[] addr) {
        return deposit.getBalance(addr);
    }

    @Override
    public long addBalance(byte[] addr, long value) {
        return deposit.addBalance(addr, value);
    }

    @Override
    public Deposit newDepositChild() {
        return deposit.newDepositChild();
    }

    @Override
    public void commit() {
        deposit.commit();
    }

    @Override
    public Storage getStorage(byte[] address) {
        return deposit.getStorage(address);
    }

    @Override
    public void putAccount(Key key, Value value) {
        deposit.putAccount(key, value);
    }

    @Override
    public void putTransaction(Key key, Value value) {
        deposit.putTransaction(key, value);
    }

    @Override
    public void putBlock(Key key, Value value) {
        deposit.putBlock(key, value);
    }

    @Override
    public void putWitness(Key key, Value value) {
        deposit.putWitness(key, value);
    }

    @Override
    public void putCode(Key key, Value value) {
        deposit.putCode(key, value);
    }

    @Override
    public void putContract(Key key, Value value) {
        deposit.putContract(key, value);
    }

    @Override
    public void putStorage(Key key, Storage cache) {
        deposit.putStorage(key, cache);
    }

    @Override
    public void putVotes(Key key, Value value) {
        deposit.putVotes(key, value);
    }

    @Override
    public void putProposal(Key key, Value value) {
        deposit.putProposal(key, value);
    }

    @Override
    public void putDynamicProperties(Key key, Value value) {
        deposit.putDynamicProperties(key, value);
    }

    @Override
    public void setParent(Deposit deposit) {
        this.deposit.setParent(deposit);
    }

    @Override
    public TransactionWrapper getTransaction(byte[] trxHash) {
        return this.deposit.getTransaction(trxHash);
    }

    @Override
    public void putAccountValue(byte[] address, AccountWrapper accountWrapper) {
        this.deposit.putAccountValue(address, accountWrapper);
    }

    @Override
    public void putVoteValue(byte[] address, VotesWrapper votesWrapper) {
        this.deposit.putVoteValue(address, votesWrapper);
    }

    @Override
    public void putProposalValue(byte[] address, ProposalWrapper proposalWrapper) {
        deposit.putProposalValue(address, proposalWrapper);
    }

    @Override
    public void putDynamicPropertiesWithLatestProposalNum(long num) {
        deposit.putDynamicPropertiesWithLatestProposalNum(num);
    }

    @Override
    public long getLatestProposalNum() {
        return deposit.getLatestProposalNum();
    }

    @Override
    public long getWitnessAllowanceFrozenTime() {
        return deposit.getWitnessAllowanceFrozenTime();
    }

    @Override
    public long getMaintenanceTimeInterval() {
        return deposit.getMaintenanceTimeInterval();
    }

    @Override
    public long getNextMaintenanceTime() {
        return deposit.getNextMaintenanceTime();
    }

    @Override
    public long addTokenBalance(byte[] address, byte[] tokenId, long value) {
        return deposit.addTokenBalance(address, tokenId, value);
    }

    @Override
    public long getTokenBalance(byte[] address, byte[] tokenId) {
        return deposit.getTokenBalance(address, tokenId);
    }

    @Override
    public AssetIssueWrapper getAssetIssue(byte[] tokenId) {
        return deposit.getAssetIssue(tokenId);
    }

    @Override
    public BlockWrapper getBlock(byte[] blockHash) {
        return this.deposit.getBlock(blockHash);
    }

    @Override
    public byte[] getBlackHoleAddress() {
        return deposit.getBlackHoleAddress();
    }

}
