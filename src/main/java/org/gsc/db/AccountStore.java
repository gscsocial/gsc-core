package org.gsc.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.typesafe.config.ConfigObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.AccountWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.Wallet;

@Slf4j
@Component
public class AccountStore extends GSCStoreWithRevoking<AccountWrapper> {

  private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

  @Autowired
  private AccountStore(@Value("account") String dbName) {
    super(dbName);
  }

  @Override
  public AccountWrapper get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountWrapper(value);
  }

  /**
   * Max TRX account.
   */
  public AccountWrapper getSun() {
    return getUnchecked(assertsAddress.get("Sun"));
  }

  /**
   * Min TRX account.
   */
  public AccountWrapper getBlackhole() {
    return getUnchecked(assertsAddress.get("Blackhole"));
  }

  /**
   * Get foundation account info.
   */
  public AccountWrapper getZion() {
    return getUnchecked(assertsAddress.get("Zion"));
  }

  /**
   * List<ConfigObject> list = config.getObjectList("genesis.block.assets");
   * add (List<ConfigObject>)
   * by kay 2018-09-4
   * @param config
   */
  public static void setAccount(com.typesafe.config.Config config) {
    List<ConfigObject> list = (List<ConfigObject>) config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = Wallet.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

}
