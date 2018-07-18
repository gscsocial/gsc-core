package org.gsc.db;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.DupTransactionException;
import org.gsc.core.exception.TaposException;
import org.gsc.core.exception.TooBigTransactionException;
import org.gsc.core.exception.TransactionExpirationException;
import org.gsc.core.exception.ValidateSignatureException;

@Slf4j
public class PendingManager implements AutoCloseable {

  @Getter
  static List<TransactionWrapper> tmpTransactions = new ArrayList<>();
  Manager dbManager;

  public PendingManager(Manager db) {
    this.dbManager = db;
    tmpTransactions.addAll(db.getPendingTransactions());
    db.getPendingTransactions().clear();
    db.getDialog().reset();
  }

  @Override
  public void close() {
    rePush(this.tmpTransactions);
    rePush(dbManager.getPoppedTransactions());
    dbManager.getPoppedTransactions().clear();
    tmpTransactions.clear();
  }

  private void rePush(List<TransactionWrapper> txs) {
    txs.stream()
        .filter(
            trx -> {
              try {
                return
                    dbManager.getTransactionStore().get(trx.getTransactionId().getBytes()) == null;
              } catch (BadItemException e) {
                return true;
              }
            })
        .forEach(trx -> {
          try {
            dbManager.pushTransactions(trx);
          } catch (ValidateSignatureException e) {
            logger.debug(e.getMessage(), e);
          } catch (ContractValidateException e) {
            logger.debug(e.getMessage(), e);
          } catch (ContractExeException e) {
            logger.debug(e.getMessage(), e);
          } catch (AccountResourceInsufficientException e) {
            logger.debug(e.getMessage(), e);
          } catch (DupTransactionException e) {
            logger.debug("pending manager: dup trans", e);
          } catch (TaposException e) {
            logger.debug("pending manager: tapos exception", e);
          } catch (TooBigTransactionException e) {
            logger.debug("too big transaction");
          } catch (TransactionExpirationException e) {
            logger.debug("expiration transaction");
          }
        });
  }
}
