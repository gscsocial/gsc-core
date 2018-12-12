package org.gsc.db;

import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.TooBigTransactionResultException;

abstract class ResourceProcessor {

  protected Manager dbManager;
  protected long precision;
  protected long windowSize;

  public ResourceProcessor(Manager manager) {
    this.dbManager = manager;
    this.precision = ChainConstant.PRECISION;
    this.windowSize = ChainConstant.WINDOW_SIZE_MS / ChainConstant.BLOCK_PRODUCED_INTERVAL;
  }

  abstract void updateUsage(AccountWrapper accountWrapper);

  abstract void consume(TransactionWrapper trx, TransactionResultWrapper ret,
                        TransactionTrace trace)
      throws ContractValidateException, AccountResourceInsufficientException, TooBigTransactionResultException;

  protected long increase(long lastUsage, long usage, long lastTime, long now) {

    long averageLastUsage = divideCeil(lastUsage * precision, windowSize); // divideCeil: (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0)
    long averageUsage = divideCeil(usage * precision, windowSize);

    if (lastTime != now) {
      assert now > lastTime;
      if (lastTime + windowSize > now) {
        long delta = now - lastTime;
        double decay = (windowSize - delta) / (double) windowSize;
        averageLastUsage = Math.round(averageLastUsage * decay);

      } else {
        averageLastUsage = 0;
      }
    }

    averageLastUsage += averageUsage;
    long getUsage = getUsage(averageLastUsage); // usage * windowSize / precision;
    return getUsage;
  }

  /**
   * numerator    带宽换乘GSC的总数
   * denominator  每天出块总数
   *
   * 0,28800
   */
  private long divideCeil(long numerator, long denominator) {
    return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
  }

  private long getUsage(long usage) {
    return usage * windowSize / precision;
  }

  protected boolean consumeFee(AccountWrapper accountWrapper, long fee) {
    try {
      long latestOperationTime = dbManager.getHeadBlockTimeStamp();
      accountWrapper.setLatestOperationTime(latestOperationTime);
      dbManager.adjustBalance(accountWrapper, -fee);
      dbManager.adjustBalance(this.dbManager.getAccountStore().getBlackhole().createDbKey(), +fee);
      return true;
    } catch (BalanceInsufficientException e) {
      return false;
    }
  }
}
