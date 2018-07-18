package org.gsc.net.message;

import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.core.exception.BadItemException;
import org.gsc.protos.Protocol.Transaction;

public class TransactionMessage extends GscMessage {

  private TransactionWrapper transactionWrapper;

  public TransactionMessage(byte[] data) throws BadItemException {
    this.transactionWrapper = new TransactionWrapper(data);
    this.data = data;
    this.type = MessageTypes.TRX.asByte();
  }

  public TransactionMessage(Transaction trx) {
    this.transactionWrapper = new TransactionWrapper(trx);
    this.type = MessageTypes.TRX.asByte();
    this.data = trx.toByteArray();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString())
        .append("messageId: ").append(super.getMessageId()).toString();
  }

  @Override
  public Sha256Hash getMessageId() {
    return this.transactionWrapper.getTransactionId();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public TransactionWrapper getTransactionWrapper() {
    return this.transactionWrapper;
  }
}
