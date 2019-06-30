package org.gsc.db;

import com.google.protobuf.ByteString;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BytesWrapper;

//todo ： need Compatibility test
@Component
public class AccountIdIndexStore extends GSCStoreWithRevoking<BytesWrapper> {

  @Autowired
  public AccountIdIndexStore(@Value("accountid-index") String dbName) {
    super(dbName);
  }

  public void put(AccountWrapper accountWrapper) {
    byte[] lowerCaseAccountId = getLowerCaseAccountId(accountWrapper.getAccountId().toByteArray());
    super.put(lowerCaseAccountId, new BytesWrapper(accountWrapper.getAddress().toByteArray()));
  }

  public byte[] get(ByteString name) {
    BytesWrapper bytesWrapper = get(name.toByteArray());
    if (Objects.nonNull(bytesWrapper)) {
      return bytesWrapper.getData();
    }
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    byte[] lowerCaseKey = getLowerCaseAccountId(key);
    byte[] value = revokingDB.getUnchecked(lowerCaseKey);
    return !ArrayUtils.isEmpty(value);
  }

  @Override
  public BytesWrapper get(byte[] key) {
    byte[] lowerCaseKey = getLowerCaseAccountId(key);
    byte[] value = revokingDB.getUnchecked(lowerCaseKey);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    return new BytesWrapper(value);
  }

  private static byte[] getLowerCaseAccountId(byte[] bsAccountId) {
    return ByteString
        .copyFromUtf8(ByteString.copyFrom(bsAccountId).toStringUtf8().toLowerCase()).toByteArray();
  }

}
