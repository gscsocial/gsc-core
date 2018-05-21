package org.gsc.db.storage.Iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.gsc.core.wrapper.AccountWrapper;


public class AccountIterator extends AbstractIterator<AccountWrapper> {

  public AccountIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected AccountWrapper of(byte[] value) {
    return new AccountWrapper(value);
  }
}
