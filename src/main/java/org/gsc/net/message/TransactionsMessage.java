package org.gsc.net.message;

import java.util.List;

import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Transaction;

public class TransactionsMessage extends GSCMessage {

  private Protocol.Transactions transactions;

  public TransactionsMessage(List<Transaction> trxs) {
    Protocol.Transactions.Builder builder = Protocol.Transactions.newBuilder();
    trxs.forEach(trx -> builder.addTransactions(trx));
    this.transactions = builder.build();
    this.type = MessageTypes.TRXS.asByte();
    this.data = this.transactions.toByteArray();
  }

  public Protocol.Transactions getTransactions() {
    return transactions;
  }
  
  public TransactionsMessage(byte[] data) throws Exception {
    this.type = MessageTypes.TRXS.asByte();
    this.data = data;
    this.transactions = Protocol.Transactions.parseFrom(data);
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append("trx size: ")
        .append(this.transactions.getTransactionsList().size()).toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
