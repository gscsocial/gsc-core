package org.gsc.db.common.iterator;

import org.gsc.core.wrapper.AssetIssueCapsule;

import java.util.Iterator;
import java.util.Map.Entry;

public class AssetIssueIterator extends AbstractIterator<AssetIssueCapsule> {

  public AssetIssueIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected AssetIssueCapsule of(byte[] value) {
    return new AssetIssueCapsule(value);
  }
}