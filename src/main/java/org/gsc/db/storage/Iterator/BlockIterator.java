package org.gsc.db.storage.Iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.gsc.common.exception.BadItemException;
import org.gsc.core.wrapper.BlockWrapper;

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
