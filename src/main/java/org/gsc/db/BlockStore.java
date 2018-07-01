package org.gsc.db;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.exception.BadItemException;
import org.gsc.common.exception.ItemNotFoundException;
import org.gsc.common.exception.StoreException;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.chain.BlockId;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.db.storage.Iterator.BlockIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BlockStore extends ChainStore<BlockWrapper> {


  @Autowired
  private BlockStore(@Value("block") String dbName) {
    super(dbName);
  }

  private static BlockStore instance;

  public static void destroy() {
    instance = null;
  }

  @Override
  public void put(byte[] key, BlockWrapper item) {
    super.put(key, item);
    if (Objects.nonNull(indexHelper)) {
      //TODO
      //indexHelper.update(item.getInstance());
    }
  }

  @Override
  public BlockWrapper get(byte[] key) throws ItemNotFoundException, BadItemException {
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException();
    }
    return new BlockWrapper(value);
  }

  public List<BlockWrapper> getLimitNumber(long startNumber, long limit) {
    BlockId startBlockId = new BlockId(Sha256Hash.ZERO_HASH, startNumber);
    return dbSource.getValuesNext(startBlockId.getBytes(), limit)
        .stream().map(bytes -> {
          try {
            return new BlockWrapper(bytes);
          } catch (BadItemException e) {
            e.printStackTrace();
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public List<BlockWrapper> getBlockByLatestNum(long getNum) {

    return dbSource.getlatestValues(getNum)
        .stream().map(bytes -> {
          try {
            return new BlockWrapper(bytes);
          } catch (BadItemException e) {
            e.printStackTrace();
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public boolean has(byte[] key) {
    byte[] block = dbSource.getData(key);
    logger.info("address is {}, block is {}", key, block);
    return null != block;
  }

  @Override
  public Iterator<Entry<byte[], BlockWrapper>> iterator() {
    return new BlockIterator(dbSource.iterator());
  }

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      try {
        BlockWrapper item = get(key);
        if (Objects.nonNull(item)) {
          //TODO
          //indexHelper.remove(item.getInstance());
        }
      } catch (StoreException e) {
        return;
      }
    }
  }
}
