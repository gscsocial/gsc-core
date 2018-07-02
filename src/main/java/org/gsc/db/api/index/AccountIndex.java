package org.gsc.db.api.index;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.db.Store;
import org.gsc.db.WrappedByteArray;
import org.gsc.protos.Protocol.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountIndex extends AbstractIndex<AccountWrapper, Account> {

  public static SimpleAttribute<WrappedByteArray, String> Account_ADDRESS;

  @Autowired
  public AccountIndex(@Qualifier("accountStore") final Store<AccountWrapper> database) {
    super(database);
  }

  @PostConstruct
  public void init() {
    initIndex(DiskPersistence.onPrimaryKeyInFile(Account_ADDRESS, indexPath));
//    index.addIndex(DiskIndex.onAttribute(Account_ADDRESS));
  }

  @Override
  protected void setAttribute() {
    Account_ADDRESS = attribute("account address",
        bytes -> ByteArray.toHexString(bytes.getBytes()));
  }
}
