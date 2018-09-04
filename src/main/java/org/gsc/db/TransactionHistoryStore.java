package org.gsc.db;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.TransactionInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.exception.BadItemException;

@Component
public class TransactionHistoryStore extends GSCStoreWithRevoking<TransactionInfoWrapper> {

  @Autowired
  public TransactionHistoryStore(@Value("transactionHistoryStore") String dbName) {
    super(dbName);
  }

  @Override
  public TransactionInfoWrapper get(byte[] key) throws BadItemException {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionInfoWrapper(value);
  }
}