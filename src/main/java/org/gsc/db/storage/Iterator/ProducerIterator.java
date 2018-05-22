package org.gsc.db.storage.Iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.gsc.core.wrapper.ProducerWrapper;

public class ProducerIterator extends AbstractIterator<ProducerWrapper> {

  public ProducerIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected ProducerWrapper of(byte[] value) {
    return new ProducerWrapper(value);
  }
}
