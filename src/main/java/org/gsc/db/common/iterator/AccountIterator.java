package org.gsc.db.common.iterator;

import org.gsc.core.wrapper.AccountWrapper;

import java.util.Iterator;
import java.util.Map.Entry;

public class AccountIterator extends AbstractIterator<AccountWrapper> {

  public AccountIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected AccountWrapper of(byte[] value) {
    return new AccountWrapper(value);
  }
}
