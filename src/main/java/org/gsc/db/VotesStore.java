package org.gsc.db;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.consensus.VotesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VotesStore extends ChainStore<VotesWrapper> {

  @Autowired
  public VotesStore(@Value("votes") String dbName) {
    super(dbName);
  }

  private static VotesStore instance;

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */
  public static VotesStore create(String dbName) {
    if (instance == null) {
      synchronized (VotesStore.class) {
        if (instance == null) {
          instance = new VotesStore(dbName);
        }
      }
    }
    return instance;
  }

  @Override
  public VotesWrapper get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new VotesWrapper(value);
  }

  /**
   * isVoterExist fun.
   *
   * @param key the address of Voter Account
   */
  @Override
  public boolean has(byte[] key) {
    byte[] account = dbSource.getData(key);
    return null != account;
  }

  @Override
  public void put(byte[] key, VotesWrapper item) {
    super.put(key, item);
  }

  /**
   * get all votes.
   */
  public List<VotesWrapper> getAllVotes() {
    return dbSource
        .allValues()
        .stream()
        .map(bytes -> new VotesWrapper(bytes))
        .collect(Collectors.toList());
  }
}