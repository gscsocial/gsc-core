package org.gsc.db.common.iterator;

import org.gsc.core.wrapper.WitnessWrapper;

import java.util.Iterator;
import java.util.Map.Entry;

public class WitnessIterator extends AbstractIterator<WitnessWrapper> {

  public WitnessIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected WitnessWrapper of(byte[] value) {
    return new WitnessWrapper(value);
  }
}
