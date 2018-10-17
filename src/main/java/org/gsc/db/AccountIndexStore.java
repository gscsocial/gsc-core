package org.gsc.db;

import com.google.protobuf.ByteString;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BytesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountIndexStore extends GSCStoreWithRevoking<BytesWrapper> {

  @Autowired
  public AccountIndexStore(@Value("account-index") String dbName) {
    super(dbName);
  }

  public void put(AccountWrapper accountWrapper) {
    put(accountWrapper.getAccountName().toByteArray(),
        new BytesWrapper(accountWrapper.getAddress().toByteArray()));
  }

  public byte[] get(ByteString name) {
    BytesWrapper bytesWrapper = get(name.toByteArray());
    if (Objects.nonNull(bytesWrapper)) {
      return bytesWrapper.getData();
    }
    return null;
  }

  @Override
  public BytesWrapper get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    return new BytesWrapper(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return false;
    }
    return true;
  }
}