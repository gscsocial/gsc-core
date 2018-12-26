package org.gsc.db;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TransactionWrapper;

@Slf4j
public class PendingManager implements AutoCloseable {

  @Getter
  static List<TransactionWrapper> tmpTransactions = new ArrayList<>();
  Manager dbManager;

  public PendingManager(Manager db) {

    this.dbManager = db;
    tmpTransactions.addAll(db.getPendingTransactions());
    db.getPendingTransactions().clear();
    db.getSession().reset();
  }

  @Override
  public void close() {

    for (TransactionWrapper tx : this.tmpTransactions) {
      try {
        dbManager.getRepushTransactions().put(tx);
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    tmpTransactions.clear();

    for (TransactionWrapper tx : dbManager.getPoppedTransactions()) {
      try {
        dbManager.getRepushTransactions().put(tx);
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
    dbManager.getPoppedTransactions().clear();
  }
}
