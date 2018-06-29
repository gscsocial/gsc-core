package org.gsc.db;

import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.StoreWrapper;
import org.gsc.db.UndoStore.UndoTuple;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ChainStore<T extends StoreWrapper> extends Store<T> {

  @Autowired
  private UndoStore undoStore;

  @Autowired(required = false)
  protected Object indexHelper;

  protected ChainStore(String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, T item) {
    if (Objects.isNull(key) || Objects.isNull(item)) {
      return;
    }
    //logger.info("Address is {}, " + item.getClass().getSimpleName() + " is {}", key, item);
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isNotEmpty(value)) {
      onModify(key, value);
    }

    dbSource.putData(key, item.getData());

    if (ArrayUtils.isEmpty(value)) {
      onCreate(key);
    }
  }

  @Override
  public void delete(byte[] key) {
    onDelete(key);
    dbSource.deleteData(key);
  }

  /**
   * This should be called just after an object is created
   */
  private void onCreate(byte[] key) {
    undoStore.onCreate(new UndoTuple(dbSource, key), null);
  }

  /**
   * This should be called just before an object is modified
   */
  private void onModify(byte[] key, byte[] value) {
    undoStore.onModify(new UndoTuple(dbSource, key), value);
  }

  /**
   * This should be called just before an object is removed.
   */
  private void onDelete(byte[] key) {
    byte[] value;
    if (Objects.nonNull(value = dbSource.getData(key))) {
      undoStore.onRemove(new UndoTuple(dbSource, key), value);
    }
  }
}
