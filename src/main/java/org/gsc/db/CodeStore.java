package org.gsc.db;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.wrapper.CodeWrapper;

@Slf4j
@Component
public class CodeStore extends GSCStoreWithRevoking<CodeWrapper> {

  @Autowired
  private CodeStore(@Value("code") String dbName) {
    super(dbName);
  }

  @Override
  public CodeWrapper get(byte[] key) {
    return getUnchecked(key);
  }

  public long getTotalCodes() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  private static CodeStore instance;

  public static void destory() {
    instance = null;
  }

  void destroy() {
    instance = null;
  }

  public byte[] findCodeByHash(byte[] hash) {
    return revokingDB.getUnchecked(hash);
  }
}
