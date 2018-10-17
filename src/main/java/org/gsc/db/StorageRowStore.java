package org.gsc.db;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.StorageRowWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StorageRowStore extends GSCStoreWithRevoking<StorageRowWrapper> {

  private static StorageRowStore instance;

  @Autowired
  private StorageRowStore(@Value("storage-row") String dbName) {
    super(dbName);
  }

  @Override
  public StorageRowWrapper get(byte[] key) {
    StorageRowWrapper row = getUnchecked(key);
    row.setRowKey(key);
    return row;
  }

  void destory() {
    instance = null;
  }
}
