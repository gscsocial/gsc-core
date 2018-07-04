package org.gsc.core.db;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.capsule.VotesCapsule;
import org.gsc.core.db.common.iterator.DBIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.capsule.VotesCapsule;

@Component
public class VotesStore extends GscStoreWithRevoking<VotesCapsule> {

  @Autowired
  public VotesStore(@Value("votes") String dbName) {
    super(dbName);
  }

  @Override
  public VotesCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new VotesCapsule(value);
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
  public void put(byte[] key, VotesCapsule item) {
    super.put(key, item);
  }

  public DBIterator getIterator() {
    return dbSource.iterator();
  }
}