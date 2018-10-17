package org.gsc.common.storage;

import static org.gsc.runtime.utils.MUtil.convertTogscAddress;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.program.Storage;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.AccountStore;
import org.gsc.db.AssetIssueStore;
import org.gsc.db.BlockStore;
import org.gsc.db.CodeStore;
import org.gsc.db.ContractStore;
import org.gsc.db.Manager;
import org.gsc.db.StorageRowStore;
import org.gsc.db.TransactionStore;
import org.gsc.db.VotesStore;
import org.gsc.db.WitnessStore;
import org.gsc.core.exception.BadItemException;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.AccountType;

public class DepositImpl implements Deposit {

  private Manager dbManager;
  private Deposit parent = null;

  private HashMap<Key, Value> accountCache = new HashMap<>();
  private HashMap<Key, Value> transactionCache = new HashMap<>();
  private HashMap<Key, Value> blockCache = new HashMap<>();
  private HashMap<Key, Value> witnessCache = new HashMap<>();
  private HashMap<Key, Value> blockIndexCache = new HashMap<>();
  private HashMap<Key, Value> codeCache = new HashMap<>();
  private HashMap<Key, Value> contractCache = new HashMap<>();

  private HashMap<Key, Value> votesCache = new HashMap<>();
  private HashMap<Key, Value> accountContractIndexCache = new HashMap<>();
  private HashMap<Key, Storage> storageCache = new HashMap<>();

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

  private AccountStore getAccountStore() {
    return dbManager.getAccountStore();
  }

  private CodeStore getCodeStore() {
    return dbManager.getCodeStore();
  }

  private StorageRowStore getStorageRowStore() {
    return dbManager.getStorageRowStore();
  }

  private AssetIssueStore getAssetIssueStore() {
    return dbManager.getAssetIssueStore();
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
  public synchronized void saveCode(byte[] codeHash, byte[] code) {
    Key key = Key.create(codeHash);
    Value value = Value.create(code, Type.VALUE_TYPE_CREATE);
    codeCache.put(key, value);
  }

  @Override
  public synchronized byte[] getCode(byte[] addr) {
    Key key = Key.create(addr);
    if (codeCache.containsKey(key)) {
      return codeCache.get(key).getCode().getData();
    }

    byte[] code;
    if (parent != null) {
      code = parent.getCode(addr);
    } else {
      if (null == getCodeStore().get(addr)) {
        code = null;
      } else {
        code = getCodeStore().get(addr).getData();
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
      storage = parent.getStorage(address);
    } else {
      storage = new Storage(address, dbManager.getStorageRowStore());
    }
    return storage;
  }

  @Override
  public synchronized void putStorageValue(byte[] address, DataWord key, DataWord value) {
    address = convertTogscAddress(address);
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
    address = convertTogscAddress(address);
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
    Value V = Value.create(accountWrapper.getData(),
        Type.VALUE_TYPE_DIRTY | accountCache.get(key).getType().getType());
    accountCache.put(key, V);
    return accountWrapper.getBalance();
  }

  @Override
  public TransactionWrapper getTransaction(byte[] trxHash) {
    Key key = Key.create(trxHash);
    if (transactionCache.containsKey(key)) {
      return transactionCache.get(key).getTransaction();
    }

    TransactionWrapper transactionCapsule;
    if (parent != null) {
      transactionCapsule = parent.getTransaction(trxHash);
    } else {
      try {
        transactionCapsule = getTransactionStore().get(trxHash);
      } catch (BadItemException e) {
        transactionCapsule = null;
      }
    }

    if (transactionCapsule != null) {
      transactionCache.put(key, Value.create(transactionCapsule.getData()));
    }
    return transactionCapsule;
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
  public long computeAfterRunStorageSize() {
    AtomicLong afterRunStorageSize = new AtomicLong();
    storageCache.forEach((key, value) -> {
      afterRunStorageSize.getAndAdd(value.computeSize());
    });
    return afterRunStorageSize.get();
  }

  @Override
  public long getBeforeRunStorageSize() {
    AtomicLong beforeRunStorageSize = new AtomicLong();
    storageCache.forEach((key, value) -> {
      beforeRunStorageSize.getAndAdd(value.getBeforeUseSize());
    });
    return beforeRunStorageSize.get();
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

//  @Override
//  public void putStorage(Key key, Value value) {
//    storageCache.put(key, value);
//  }

  @Override
  public void putStorage(Key key, Storage cache) {
    storageCache.put(key, cache);
  }

  @Override
  public void putVotes(Key key, Value value) {
    votesCache.put(key, value);
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
    storageCache.forEach((key, value) -> {
      if (deposit != null) {
        // write to parent cache
        deposit.putStorage(key, value);
      } else {
        // persistence
        value.commit();
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

  @Override
  public void syncCacheFromAccountStore(byte[] address) {
    Key key = Key.create(address);
    int type;
    if (null == accountCache.get(key)) {
      type = Type.VALUE_TYPE_DIRTY;
    } else {
      type = Type.VALUE_TYPE_DIRTY | accountCache.get(key).getType().getType();
    }
    Value V = Value.create(getAccountStore().get(address).getData(), type);
    accountCache.put(key, V);
  }

  @Override
  public void syncCacheFromVotesStore(byte[] address) {
    Key key = Key.create(address);
    int type;
    if (null == votesCache.get(key)) {
      type = Type.VALUE_TYPE_DIRTY;
    } else {
      type = Type.VALUE_TYPE_DIRTY | votesCache.get(key).getType().getType();
    }
    Value V = Value.create(getVotesStore().get(address).getData(), type);
    votesCache.put(key, V);
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
    // commitAccountContractIndex(deposit);
  }

  @Override
  public void flush() {
    throw new RuntimeException("Not supported");
  }

  @Override
  public void setParent(Deposit deposit) {
    parent = deposit;
  }

  public static DepositImpl createRoot(Manager dbManager) {
    return new DepositImpl(dbManager, null);
  }
}
