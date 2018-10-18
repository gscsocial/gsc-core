package org.gsc.db;

import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.BytesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.core.exception.ItemNotFoundException;

@Component
public class BlockIndexStore extends GSCStoreWithRevoking<BytesWrapper> {


  @Autowired
  public BlockIndexStore(@Value("block-index") String dbName) {
    super(dbName);

  }

  public void put(BlockId id) {
    put(ByteArray.fromLong(id.getNum()), new BytesWrapper(id.getBytes()));
  }

  public BlockId get(Long num)
          throws ItemNotFoundException {
    BytesWrapper value = getUnchecked(ByteArray.fromLong(num));
    if (value == null || value.getData() == null) {
      throw new ItemNotFoundException("number: " + num + " is not found!");
    }
    return new BlockId(Sha256Hash.wrap(value.getData()), num);
  }

  @Override
  public BytesWrapper get(byte[] key)
          throws ItemNotFoundException {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException("number: " + Arrays.toString(key) + " is not found!");
    }
    return new BytesWrapper(value);
  }
}