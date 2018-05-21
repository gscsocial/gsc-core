package org.gsc.db;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.storage.Iterator.TransactionIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class TransactionStore extends ChainStore<TransactionWrapper> {

  @Autowired
  private TransactionStore(@Qualifier("trans") String dbName) {
    super(dbName);
  }

  @Override
  public TransactionWrapper get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionWrapper(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] transaction = dbSource.getData(key);
    logger.info("address is {}, transaction is {}", key, transaction);
    return null != transaction;
  }

  @Override
  public void put(byte[] key, TransactionWrapper item) {
    super.put(key, item);
    if (Objects.nonNull(indexHelper)) {
      //TODO
      //indexHelper.update(item.getInstance());
    }
  }

  /**
   * get total transaction.
   */
  public long getTotalTransactions() {
    return dbSource.getTotal();
  }

  private static TransactionStore instance;

  public static void destory() {
    instance = null;
  }

  public static void destroy() {
    instance = null;
  }

  /**
   * find a transaction  by it's id.
   */
  public byte[] findTransactionByHash(byte[] trxHash) {
    return dbSource.getData(trxHash);
  }

  @Override
  public Iterator<Entry<byte[], TransactionWrapper>> iterator() {
    return new TransactionIterator(dbSource.iterator());
  }

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      TransactionWrapper item = get(key);
      if (Objects.nonNull(item)) {
        //TODO
        //indexHelper.remove(item.getInstance());
      }
    }
  }
}
