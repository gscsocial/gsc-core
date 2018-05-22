package org.gsc.db;

import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.utils.AddressUtil;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.db.storage.Iterator.AccountIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountStore extends ChainStore<AccountWrapper> {

  private static Map<String, byte[]> assertsAddress =
      new HashMap<String, byte[]>(); // key = name , value = address
  private static AccountStore instance;

  @Autowired
  private AccountStore(@Qualifier("account") String dbName) {
    super(dbName);
  }

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static AccountStore create(String dbName) {
    if (instance == null) {
      synchronized (AccountStore.class) {
        if (instance == null) {
          instance = new AccountStore(dbName);
        }
      }
    }
    return instance;
  }

  @Override
  public AccountWrapper get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountWrapper(value);
  }

  /**
   * isAccountExist fun.
   *
   * @param key the address of Account
   */
  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    return null != account;
  }

  @Override
  public void put(byte[] key, AccountWrapper item) {
    super.put(key, item);
    if (Objects.nonNull(indexHelper)) {
      //TODO
      //indexHelper.update(item.getInstance());
    }
  }

  /**
   * get all accounts.
   */
  public List<AccountWrapper> getAllAccounts() {
    return dbSource
        .allValues()
        .stream()
        .map(bytes -> new AccountWrapper(bytes))
        .collect(Collectors.toList());
  }

  /**
   * Max TRX account.
   */
  public AccountWrapper getPhoton() {
    byte[] data = dbSource.getData(assertsAddress.get("hoton"));
    AccountWrapper accountCapsule = new AccountWrapper(data);
    return accountCapsule;
  }

  /**
   * Min TRX account.
   */
  public AccountWrapper getBlackhole() {
    byte[] data = dbSource.getData(assertsAddress.get("Blackhole"));
    AccountWrapper accountCapsule = new AccountWrapper(data);
    return accountCapsule;
  }

  public static void setAccount(com.typesafe.config.Config config) {
    List list = config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = AddressUtil.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  @Override
  public Iterator<Entry<byte[], AccountWrapper>> iterator() {
    return new AccountIterator(dbSource.iterator());
  }

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      AccountWrapper item = get(key);
      if (Objects.nonNull(item)) {
        //TODO
        //indexHelper.remove(item.getInstance());
      }
    }
  }
}
