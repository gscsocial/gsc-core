package org.gsc.common.storage;

import org.gsc.common.runtime.vm.DataWord;
import org.gsc.common.runtime.vm.program.Storage;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol;

public interface Deposit {

  Manager getDbManager();

  AccountWrapper createAccount(byte[] address, Protocol.AccountType type);

  AccountWrapper createAccount(byte[] address, String accountName, Protocol.AccountType type);

  AccountWrapper getAccount(byte[] address);

  void deleteContract(byte[] address);

  void createContract(byte[] address, ContractWrapper contractWrapper);

  ContractWrapper getContract(byte[] address);

  void saveCode(byte[] codeHash, byte[] code);

  byte[] getCode(byte[] codeHash);

  //byte[] getCodeHash(byte[] address);

  void putStorageValue(byte[] address, DataWord key, DataWord value);

  DataWord getStorageValue(byte[] address, DataWord key);

  Storage getStorage(byte[] address);

  long getBalance(byte[] address);

  long addBalance(byte[] address, long value);


  Deposit newDepositChild();

  void setParent(Deposit deposit);

  void flush();

  void commit();

  void putAccount(Key key, Value value);

  void putTransaction(Key key, Value value);

  void putBlock(Key key, Value value);

  void putWitness(Key key, Value value);

  void putCode(Key key, Value value);

  void putContract(Key key, Value value);

  void putStorage(Key key, Storage cache);

  void putVotes(Key key, Value value);

  void syncCacheFromAccountStore(byte[] address);

  void syncCacheFromVotesStore(byte[] address);

  TransactionWrapper getTransaction(byte[] trxHash);

  BlockWrapper getBlock(byte[] blockHash);

  long computeAfterRunStorageSize();

  long getBeforeRunStorageSize();

}
