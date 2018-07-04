package org.gsc.api;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.exception.BadItemException;
import org.gsc.common.utils.ByteArray;
import org.gsc.db.Manager;
import org.gsc.db.api.StoreAPI;
import org.gsc.protos.Protocol.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WalletSolidity {

  @Autowired
  private StoreAPI storeAPI;
  @Autowired
  private Manager dbManager;

  public Transaction getTransactionById(ByteString id) {
    try {
      Transaction transactionById = storeAPI
          .getTransactionById(ByteArray.toHexString(id.toByteArray()));
      return transactionById;
    } catch (NonUniqueObjectException e) {
      e.printStackTrace();
    }
    return null;
  }

  public TransactionInfo getTransactionInfoById(ByteString id) {
    if (Objects.isNull(id)) {
      return null;
    }
    TransactionInfoWrapper transactionInfoWrapper = null;
    try {
      transactionInfoWrapper = dbManager.getTransactionHistoryStore()
          .get(id.toByteArray());
    } catch (BadItemException e) {
    }
    if (transactionInfoWrapper != null) {
      return transactionInfoWrapper.getInstance();
    }
    return null;
  }

  public TransactionList getTransactionsFromThis(ByteString thisAddress, long offset, long limit) {
    List<Transaction> transactionsFromThis = storeAPI
        .getTransactionsFromThis(ByteArray.toHexString(thisAddress.toByteArray()), offset, limit);
    TransactionList transactionList = TransactionList.newBuilder()
        .addAllTransaction(transactionsFromThis).build();
    return transactionList;
  }

  public TransactionList getTransactionsToThis(ByteString toAddress, long offset, long limit) {
    List<Transaction> transactionsToThis = storeAPI
        .getTransactionsToThis(ByteArray.toHexString(toAddress.toByteArray()), offset, limit);
    TransactionList transactionList = TransactionList.newBuilder()
        .addAllTransaction(transactionsToThis).build();
    return transactionList;
  }
}
