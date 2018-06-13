package org.gsc.net.message.gsc;

import org.gsc.common.exception.BadItemException;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.net.message.MessageTypes;
import org.gsc.protos.Protocol.Transaction;

public class TransactionMessage extends GscMessage {

  private TransactionWrapper tx;

  public TransactionMessage(byte[] data) throws BadItemException {
    this.tx = new TransactionWrapper(data);
    this.data = data;
    this.type = MessageTypes.TRANSACTION.asByte();
  }

  public TransactionMessage(Transaction trx) {
    this.tx = new TransactionWrapper(trx);
    this.type = MessageTypes.TRANSACTION.asByte();
    this.data = trx.toByteArray();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString())
        .append("messageId: ").append(super.getMessageId()).toString();
  }

  @Override
  public Sha256Hash getMessageId() {
    return this.tx.getTransactionId();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public TransactionWrapper getTransactionWrapper() {
    return this.tx;
  }
}