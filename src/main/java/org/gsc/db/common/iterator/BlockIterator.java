package org.gsc.db.common.iterator;

import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.exception.BadItemException;

import java.util.Iterator;
import java.util.Map.Entry;

public class BlockIterator extends AbstractIterator<BlockWrapper> {

  public BlockIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected BlockWrapper of(byte[] value) {
    try {
      return new BlockWrapper(value);
    } catch (BadItemException e) {
      throw new RuntimeException(e);
    }
  }
}
