package org.gsc.db;

import java.util.Objects;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.TransactionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.StoreException;

@Slf4j
@Component
public class TransactionStore extends GSCStoreWithRevoking<TransactionWrapper> {

  @Autowired
  private TransactionStore(@Value("trans") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, TransactionWrapper item) {
    super.put(key, item);
    if (Objects.nonNull(indexHelper)) {
      indexHelper.update(item.getInstance());
    }
  }

  @Override
  public TransactionWrapper get(byte[] key) throws BadItemException {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionWrapper(value);
  }

  /**
   * get total transaction.
   */
  public long getTotalTransactions() {
    return Streams.stream(iterator()).count();
  }

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      TransactionWrapper item;
      try {
        item = get(key);
        if (Objects.nonNull(item)) {
          indexHelper.remove(item.getInstance());
        }
      } catch (StoreException e) {
        return;
      }
    }
  }
}
