package org.gsc.db.iterator;

import java.util.Iterator;
import java.util.Map.Entry;
import org.gsc.core.wrapper.ProducerWrapper;

public class WitnessIterator extends AbstractIterator<ProducerWrapper> {

  public WitnessIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected ProducerWrapper of(byte[] value) {
    return new ProducerWrapper(value);
  }
}
