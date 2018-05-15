package org.gsc.core.chain;

import org.gsc.common.utils.Sha256Hash;
import org.gsc.protos.Protocol.Transaction;

public class TransactionWrapper {

  private Transaction transaction;

  public TransactionWrapper(Transaction trx) {
    this.transaction = trx;
  }

  public Sha256Hash getHash() {
    return Sha256Hash.of(this.transaction.toByteArray());
  }
}
