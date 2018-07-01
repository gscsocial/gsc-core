package org.gsc.db;

import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.exception.ItemNotFoundException;
import org.gsc.core.wrapper.BytesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaposBlockStore extends ChainStore<BytesWrapper> {

  @Autowired
  private TaposBlockStore(@Value("recent-block") String dbName) {
    super(dbName);
  }

  private static BlockStore instance;

  public static void destroy() {
    instance = null;
  }

  @Override
  public void put(byte[] key, BytesWrapper item) {
    super.put(key, item);
  }

  @Override
  public BytesWrapper get(byte[] key) throws ItemNotFoundException {
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException();
    }
    return new BytesWrapper(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] value = dbSource.getData(key);
    return null != value;
  }
}
