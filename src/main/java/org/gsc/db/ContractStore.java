package org.gsc.db;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.protos.Protocol.SmartContract;

@Slf4j
@Component
public class ContractStore extends GSCStoreWithRevoking<ContractWrapper> {

  @Autowired
  private ContractStore(@Value("contract") String dbName) {
    super(dbName);
  }

  @Override
  public ContractWrapper get(byte[] key) {
    return getUnchecked(key);
  }

  /**
   * get total transaction.
   */
  public long getTotalContracts() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  private static ContractStore instance;

  public static void destory() {
    instance = null;
  }

  void destroy() {
    instance = null;
  }

  /**
   * find a transaction  by it's id.
   */
  public byte[] findContractByHash(byte[] trxHash) {
    return revokingDB.getUnchecked(trxHash);
  }

  /**
   *
   * @param contractAddress
   * @return
   */
  public SmartContract.ABI getABI(byte[] contractAddress) {
    byte[] value = revokingDB.getUnchecked(contractAddress);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }

    ContractWrapper contractWrapper = new ContractWrapper(value);
    SmartContract smartContract = contractWrapper.getInstance();
    if (smartContract == null) {
      return null;
    }

    return smartContract.getAbi();
  }

}
