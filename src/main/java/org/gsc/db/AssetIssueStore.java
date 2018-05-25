package org.gsc.db;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.db.storage.Iterator.AssetIssueIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class AssetIssueStore extends ChainStore<AssetIssueWrapper> {

  private static AssetIssueStore instance;

  @Autowired
  private AssetIssueStore(@Qualifier("asset-issue") String dbName) {
    super(dbName);
  }

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static AssetIssueStore create(String dbName) {
    if (instance == null) {
      synchronized (AssetIssueStore.class) {
        if (instance == null) {
          instance = new AssetIssueStore(dbName);
        }
      }
    }
    return instance;
  }

  @Override
  public AssetIssueWrapper get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new AssetIssueWrapper(value);
  }

  /**
   * isAssetIssusExist fun.
   *
   * @param key the address of Account
   */
  @Override
  public boolean has(byte[] key) {
    byte[] assetIssue = dbSource.getData(key);
    logger.info("name is {}, asset issue is {}", key, assetIssue);
    return null != assetIssue;
  }

  @Override
  public void put(byte[] key, AssetIssueWrapper item) {
    super.put(key, item);
    if (Objects.nonNull(indexHelper)) {
      //TODO
      //indexHelper.update(item.getInstance());
    }
  }

  /**
   * get all asset issues.
   */
  public List<AssetIssueWrapper> getAllAssetIssues() {
    return dbSource.allKeys().stream()
        .map(this::get)
        .collect(Collectors.toList());
  }

  @Override
  public Iterator<Entry<byte[], AssetIssueWrapper>> iterator() {
    return new AssetIssueIterator(dbSource.iterator());
  }

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      AssetIssueWrapper item = get(key);
      if (Objects.nonNull(item)) {
        //TODO
        //indexHelper.remove(item.getInstance());
      }
    }
  }
}
