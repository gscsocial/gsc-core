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

import org.gsc.core.wrapper.*;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.program.Storage;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol;

public interface Deposit {

    Manager getDbManager();

    AccountWrapper createAccount(byte[] address, Protocol.AccountType type);

    AccountWrapper createAccount(byte[] address, String accountName, Protocol.AccountType type);

    AccountWrapper getAccount(byte[] address);

    WitnessWrapper getWitness(byte[] address);

    VotesWrapper getVotesWrapper(byte[] address);

    ProposalWrapper getProposalWrapper(byte[] id);

    BytesWrapper getDynamic(byte[] bytesKey);

    void deleteContract(byte[] address);

    void createContract(byte[] address, ContractWrapper contractWrapper);

    ContractWrapper getContract(byte[] address);

    void updateContract(byte[] address, ContractWrapper contractWrapper);

    void updateAccount(byte[] address, AccountWrapper accountWrapper);

    void saveCode(byte[] address, byte[] code);

    byte[] getCode(byte[] address);

    void putStorageValue(byte[] address, DataWord key, DataWord value);

    DataWord getStorageValue(byte[] address, DataWord key);

    Storage getStorage(byte[] address);

    long getBalance(byte[] address);

    long addBalance(byte[] address, long value);

    Deposit newDepositChild();

    void setParent(Deposit deposit);

    void commit();

    void putAccount(Key key, Value value);

    void putTransaction(Key key, Value value);

    void putBlock(Key key, Value value);

    void putWitness(Key key, Value value);

    void putCode(Key key, Value value);

    void putContract(Key key, Value value);

    void putStorage(Key key, Storage cache);

    void putVotes(Key key, Value value);

    void putProposal(Key key, Value value);

    void putDynamicProperties(Key key, Value value);

    void putAccountValue(byte[] address, AccountWrapper accountWrapper);

    void putVoteValue(byte[] address, VotesWrapper votesWrapper);

    void putProposalValue(byte[] address, ProposalWrapper proposalWrapper);

    void putDynamicPropertiesWithLatestProposalNum(long num);

    long getLatestProposalNum();

    long getWitnessAllowanceFrozenTime();

    long getMaintenanceTimeInterval();

    long getNextMaintenanceTime();

    long addTokenBalance(byte[] address, byte[] tokenId, long value);

    long getTokenBalance(byte[] address, byte[] tokenId);

    AssetIssueWrapper getAssetIssue(byte[] tokenId);

    TransactionWrapper getTransaction(byte[] trxHash);

    BlockWrapper getBlock(byte[] blockHash);

    byte[] getBlackHoleAddress();

}
