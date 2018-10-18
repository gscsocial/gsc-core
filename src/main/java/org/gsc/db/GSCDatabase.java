package org.gsc.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.db2.core.IGSCChainBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.gsc.common.storage.leveldb.LevelDbDataSourceImpl;
import org.gsc.config.args.Args;
import org.gsc.db.api.IndexHelper;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ItemNotFoundException;

@Slf4j
public abstract class GSCDatabase<T> implements IGSCChainBase<T> {

  protected LevelDbDataSourceImpl dbSource;
  @Getter
  private String dbName;

  @Autowired(required = false)
  protected IndexHelper indexHelper;

  protected GSCDatabase(String dbName) {
    this.dbName = dbName;
    dbSource = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
    dbSource.initDB();
  }

  protected GSCDatabase() {
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

  public T getUnchecked(byte[] key) {
    return null;
  }

  public abstract boolean has(byte[] key);

  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Iterator<Entry<byte[], T>> iterator() {
    throw new UnsupportedOperationException();
  }
}
