package org.gsc.db.common.iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.core.exception.BadItemException;

public class TransactionIterator extends AbstractIterator<TransactionWrapper> {

  public TransactionIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected TransactionWrapper of(byte[] value) {
    try {
      return new TransactionWrapper(value);
    } catch (BadItemException e) {
      throw new RuntimeException(e);
    }
  }
}
