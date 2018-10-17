package org.gsc.db;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.VotesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VotesStore extends GSCStoreWithRevoking<VotesWrapper> {

  @Autowired
  public VotesStore(@Value("votes") String dbName) {
    super(dbName);
  }

  @Override
  public VotesWrapper get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new VotesWrapper(value);
  }
}