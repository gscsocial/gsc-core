package org.gsc.db;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.wrapper.ProducerWrapper;
import org.gsc.db.storage.Iterator.ProducerIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class ProducerStore extends ChainStore<ProducerWrapper> {

  @Autowired
  protected ProducerStore(@Qualifier("witness") String dbName) {
    super(dbName);
  }

  @Override
  public ProducerWrapper get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new ProducerWrapper(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    if (account == null) {
      logger.warn(
          "producer not found ,address is {},producer is {} : ",
          key,
          ByteArray.toHexString(account));
    }
    return null != account;
  }

  @Override
  public void put(byte[] key, ProducerWrapper item) {
    super.put(key, item);
  }

  /**
   * get all witnesses.
   */
  public List<ProducerWrapper> getAllWitnesses() {
    return dbSource
        .allValues()
        .stream()
        .map(bytes -> new ProducerWrapper(bytes))
        .collect(Collectors.toList());
  }

  @Override
  public Iterator<Entry<byte[], ProducerWrapper>> iterator() {
    return new ProducerIterator(dbSource.iterator());
  }

  @Override
  public void delete(byte[] key) {
    super.delete(key);
  }
}
