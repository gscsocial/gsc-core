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

  /**
   *     long PRECISION = 1000000;
   *     long WINDOW_SIZE_MS = 24 * 3600 * 1000L;
   *     BLOCK_PRODUCED_INTERVAL = 3000;
   *     windowSize 每天出块总数 28800
   *     precision GSC换算比 1000_000
   *     lastUsage 剩余免费的带宽
   *     lastTime 最后使用免费带宽的时间
   *     usage 0
   *     averageLastUsage  剩余免费的带宽可以出多少块
   *     averageUsage
   *
   *     now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - getGenesisBlock()
   *         .getTimeStamp())
   *         / ChainConstant.BLOCK_PRODUCED_INTERVAL;
   *     now = HeadSlot
   *
   *     long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit(); 5000
   *     long freeNetUsage = accountWrapper.getFreeNetUsage();                        0
   *     long latestConsumeFreeTime = accountWrapper.getLatestConsumeFreeTime();      0
   *     long newFreeNetUsage = increase(freeNetUsage, 0, latestConsumeFreeTime, now);
   *
   *     0 0 null now = 0 averageLastUsage = 0
   *     0 0 null now = 0 averageUsage = 0
   *
   */
  protected long increase(long lastUsage, long usage, long lastTime, long now) {
    System.out.println("-----------------------------------------------------------------------");
    System.out.println("lastUsage: " + lastUsage);
    System.out.println("usage: " + usage);
    System.out.println("lastTime: " + lastTime);
    System.out.println("now: " + now);

    System.out.println("precision: " + precision);
    System.out.println("windowSize: " + windowSize);

    System.out.println("----lastUsage * precision: " + lastUsage * precision);
    System.out.println("----usage * precision: " + usage * precision);

    long averageLastUsage = divideCeil(lastUsage * precision, windowSize); // 24
    long averageUsage = divideCeil(usage * precision, windowSize);

    System.out.println("averageLastUsage: " + averageLastUsage);
    System.out.println("averageUsage: " + averageUsage);

    if (lastTime != now) {
      assert now > lastTime;
      if (lastTime + windowSize > now) {
        long delta = now - lastTime;
        double decay = (windowSize - delta) / (double) windowSize;
        averageLastUsage = Math.round(averageLastUsage * decay);

        System.out.println("delta: " + delta);
        System.out.println("decay: " + decay);
        System.out.println("averageLastUsage: " + averageLastUsage);

      } else {
        averageLastUsage = 0;
      }
    }

    averageLastUsage += averageUsage;
    System.out.println("averageUsage: " + averageUsage);
    System.out.println("averageLastUsage: " + averageLastUsage);
    long getUsage = getUsage(averageLastUsage);
    System.out.println("getUsage: " + getUsage);
    System.out.println("-----------------------------------------------------------------------");
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
