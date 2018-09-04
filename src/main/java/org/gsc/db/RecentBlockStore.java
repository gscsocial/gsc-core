package org.gsc.db;

import org.gsc.core.wrapper.BytesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.exception.ItemNotFoundException;

@Component
public class RecentBlockStore extends GSCStoreWithRevoking<BytesWrapper> {

  @Autowired
  private RecentBlockStore(@Value("recent-block") String dbName) {
    super(dbName);
  }

  @Override
  public BytesWrapper get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);

    return new BytesWrapper(value);
  }
}
