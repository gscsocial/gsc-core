package org.gsc.db;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.TransactionInfoCapsule;
import org.gsc.core.exception.BadItemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TransactionHistoryStore extends GscStoreWithRevoking<TransactionInfoCapsule> {

  @Autowired
  public TransactionHistoryStore(@Value("transactionHistoryStore") String dbName) {
    super(dbName);
  }


  @Override
  public void put(byte[] key, TransactionInfoCapsule item) {
    super.put(key, item);
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
  }

  @Override
  public TransactionInfoCapsule get(byte[] key) throws BadItemException {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionInfoCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    return null != account;
  }


}