package org.gsc.db;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.exception.ItemNotFoundException;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.chain.BlockId;
import org.gsc.core.wrapper.BytesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlockIndexStore extends ChainStore<BytesWrapper> {


  @Autowired
  public BlockIndexStore(@Value("block-index") String dbName) {
    super(dbName);

  }

  private static BlockIndexStore instance;

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static BlockIndexStore create(String dbName) {
    if (instance == null) {
      synchronized (BlockIndexStore.class) {
        if (instance == null) {
          instance = new BlockIndexStore(dbName);
        }
      }
    }
    return instance;
  }

  public void put(BlockId id) {
    put(ByteArray.fromLong(id.getNum()), new BytesWrapper(id.getBytes()));
  }


  public BlockId get(Long num)
      throws ItemNotFoundException {
    return new BlockId(Sha256Hash.wrap(get(ByteArray.fromLong(num)).getData()),
        num);
  }

  @Override
  public BytesWrapper get(byte[] key)
      throws ItemNotFoundException {
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException("number: " + key + " is not found!");
    }
    return new BytesWrapper(value);
  }


  @Override
  public boolean has(byte[] key) {
    return false;
  }
}