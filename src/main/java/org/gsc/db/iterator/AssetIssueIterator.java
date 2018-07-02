package org.gsc.db.iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.gsc.core.wrapper.AssetIssueWrapper;

public class AssetIssueIterator extends AbstractIterator<AssetIssueWrapper> {

  public AssetIssueIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected AssetIssueWrapper of(byte[] value) {
    return new AssetIssueWrapper(value);
  }
}