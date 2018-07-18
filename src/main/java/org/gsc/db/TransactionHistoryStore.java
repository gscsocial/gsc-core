package org.gsc.db;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.TransactionInfoWrapper;
import org.gsc.core.exception.BadItemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TransactionHistoryStore extends GscStoreWithRevoking<TransactionInfoWrapper> {

  @Autowired
  public TransactionHistoryStore(@Value("transactionHistoryStore") String dbName) {
    super(dbName);
  }


  @Override
  public void put(byte[] key, TransactionInfoWrapper item) {
    super.put(key, item);
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
  }

  @Override
  public TransactionInfoWrapper get(byte[] key) throws BadItemException {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionInfoWrapper(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    return null != account;
  }


}