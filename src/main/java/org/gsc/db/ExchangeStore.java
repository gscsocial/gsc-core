package org.gsc.db;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gsc.core.wrapper.ExchangeWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.exception.ItemNotFoundException;

@Component
public class ExchangeStore extends GSCStoreWithRevoking<ExchangeWrapper> {

  @Autowired
  public ExchangeStore(@Value("exchange") String dbName) {
    super(dbName);
  }

  @Override
  public ExchangeWrapper get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new ExchangeWrapper(value);
  }

  /**
   * get all exchanges.
   */
  public List<ExchangeWrapper> getAllExchanges() {
    return Streams.stream(iterator())
            .map(Map.Entry::getValue)
            .sorted(
                    (ExchangeWrapper a, ExchangeWrapper b) -> a.getCreateTime() <= b.getCreateTime() ? 1
                            : -1)
            .collect(Collectors.toList());
  }
}