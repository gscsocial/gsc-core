package org.gsc.db;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.wrapper.ProposalWrapper;
import org.gsc.core.exception.ItemNotFoundException;

@Component
public class ProposalStore extends GSCStoreWithRevoking<ProposalWrapper> {

  @Autowired
  public ProposalStore(@Value("proposal") String dbName) {
    super(dbName);
  }

  @Override
  public ProposalWrapper get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new ProposalWrapper(value);
  }

  /**
   * get all proposals.
   */
  public List<ProposalWrapper> getAllProposals() {
    return Streams.stream(iterator())
            .map(Map.Entry::getValue)
            .sorted(
                    (ProposalWrapper a, ProposalWrapper b) -> a.getCreateTime() <= b.getCreateTime() ? 1
                            : -1)
            .collect(Collectors.toList());
  }
}