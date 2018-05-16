package org.gsc.core.chain;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.protos.Protocol.Transaction;

@Slf4j
public class TransactionWrapper {

  private Transaction transaction;
  @Setter
  private boolean isVerified = false;
  /**
   * constructor TransactionWrapper.
   */
  public TransactionWrapper(Transaction trx) {
    this.transaction = trx;
  }

  /**
   * get account from bytes data.
   */
  public TransactionWrapper(byte[] data) {
    try {
      this.transaction = Transaction.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage());
    }
  }

  public Transaction getInstance() {return transaction;}

  public Sha256Hash getHash() {return Sha256Hash.ZERO_HASH;}


}
