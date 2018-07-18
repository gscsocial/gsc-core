package org.gsc.db;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.core.wrapper.BytesWrapper;
import org.gsc.core.exception.ItemNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class BlockIndexStore extends GscStoreWithRevoking<BytesWrapper> {


  @Autowired
  public BlockIndexStore(@Value("block-index") String dbName) {
    super(dbName);

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
      throw new ItemNotFoundException("number: " + Arrays.toString(key) + " is not found!");
    }
    return new BytesWrapper(value);
  }


  @Override
  public boolean has(byte[] key) {
    return false;
  }
}