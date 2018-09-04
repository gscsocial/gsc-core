/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.gsc.common.runtime.vm.program;

import org.gsc.common.runtime.vm.DataWord;
import org.gsc.common.runtime.vm.program.invoke.ProgramInvoke;
import org.gsc.common.runtime.vm.program.listener.ProgramListener;
import org.gsc.common.runtime.vm.program.listener.ProgramListenerAware;
import org.gsc.common.storage.Deposit;
import org.gsc.common.storage.Key;
import org.gsc.common.storage.Value;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.AccountType;

public class ContractState implements Deposit, ProgramListenerAware {

  private Deposit deposit;
  private final DataWord address;  // contract address
  private ProgramListener programListener;

  public ContractState(ProgramInvoke programInvoke) {
    this.address = programInvoke.getOwnerAddress(); // contract address
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
  public void saveCode(byte[] addr, byte[] code) {
    deposit.saveCode(addr, code);
  }

  @Override
  public byte[] getCode(byte[] addr) {
    return deposit.getCode(addr);
  }

    /*
    @Override
    public byte[] getCodeHash(byte[] addr) {
        return deposit.getCodeHash(addr);
    }
    */

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
  public void flush() {
    deposit.flush();
  }

  @Override
  public void commit() {
    deposit.commit();
  }

//  @Override
//  public StorageCapsule getContractState(byte[] address) {
//    return deposit.getContractState(address);
//  }

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
  public void setParent(Deposit deposit) {
    this.deposit.setParent(deposit);
  }

  @Override
  public TransactionWrapper getTransaction(byte[] trxHash) {
    return this.deposit.getTransaction(trxHash);
  }

  @Override
  // Do nothing
  public void syncCacheFromAccountStore(byte[] address) {
  }

  @Override
  // Do nothing
  public void syncCacheFromVotesStore(byte[] address) {
  }

  @Override
  public BlockWrapper getBlock(byte[] blockHash) {
    return this.deposit.getBlock(blockHash);
  }

  @Override
  public long computeAfterRunStorageSize() {
    return this.deposit.computeAfterRunStorageSize();
  }

  @Override
  public long getBeforeRunStorageSize() {
    return this.deposit.getBeforeRunStorageSize();
  }
}
