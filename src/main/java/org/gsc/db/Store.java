package org.gsc.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.exception.BadItemException;
import org.gsc.common.exception.ItemNotFoundException;
import org.gsc.config.Args;
import org.gsc.db.storage.leveldb.LevelDbDataSourceImpl;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class Store<T> implements Iterable<Map.Entry<byte[], T>> {

  protected LevelDbDataSourceImpl dbSource;

//  @Autowired(required = false)
//  protected IndexHelper indexHelper;

  @Autowired
  protected Args config;

  protected Store(String dbName) {
    dbSource = new LevelDbDataSourceImpl(config.getOutputDirectory(), dbName);
    dbSource.initDB();
  }

  protected Store() {
    throw new IllegalStateException("This constructor is not allowed");
  }

  public LevelDbDataSourceImpl getDbSource() {
    return dbSource;
  }

  /**
   * reset the database.
   */
  public void reset() {
    dbSource.resetDb();
  }

  /**
   * close the database.
   */
  public void close() {
    dbSource.closeDB();
  }

  public abstract void put(byte[] key, T item);

  public abstract void delete(byte[] key);

  public abstract T get(byte[] key)
      throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

  public abstract boolean has(byte[] key);

  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Iterator<Entry<byte[], T>> iterator() {
    throw new UnsupportedOperationException();
  }
}
