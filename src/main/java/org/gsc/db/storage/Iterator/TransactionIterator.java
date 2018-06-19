package org.gsc.db.storage.Iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.gsc.common.exception.BadItemException;
import org.gsc.core.wrapper.TransactionWrapper;

public class TransactionIterator extends AbstractIterator<TransactionWrapper> {

  public TransactionIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected TransactionWrapper of(byte[] value) {
    try {
      return new TransactionWrapper(value);
    } catch (BadItemException e) {
      e.printStackTrace();
    }
    return null;
  }
}
