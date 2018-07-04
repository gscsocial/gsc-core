package org.gsc.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.storage.leveldb.LevelDbDataSourceImpl;
import org.gsc.common.utils.Quitable;
import org.gsc.core.config.args.Args;
import org.gsc.core.db.api.IndexHelper;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ItemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class GscDatabase<T> implements Iterable<Map.Entry<byte[], T>>, Quitable {

  protected LevelDbDataSourceImpl dbSource;

  @Autowired(required = false)
  protected IndexHelper indexHelper;

  protected GscDatabase(String dbName) {
    dbSource = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
    dbSource.initDB();
  }

  protected GscDatabase() {
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
  @Override
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
