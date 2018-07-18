package org.gsc.db;


import static org.gsc.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol.Transaction.Contract;

@Slf4j
public class BandwidthProcessor {

  private Manager dbManager;
  private long precision;
  private long windowSize;

  public BandwidthProcessor(Manager manager) {
    this.dbManager = manager;
    this.precision = ChainConstant.PRECISION;
    this.windowSize = ChainConstant.WINDOW_SIZE_MS / ChainConstant.BLOCK_PRODUCED_INTERVAL;
  }

  private long divideCeil(long numerator, long denominator) {
    return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
  }

  private long increase(long lastUsage, long usage, long lastTime, long now) {
    long averageLastUsage = divideCeil(lastUsage * precision, windowSize);
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
    return getUsage(averageLastUsage);
  }

  private long getUsage(long usage) {
    return usage * windowSize / precision;
  }

  public void updateUsage(AccountWrapper accountWrapper) {
    long now = dbManager.getWitnessController().getHeadSlot();
    updateUsage(accountWrapper, now);
  }

  public void updateUsage(AccountWrapper accountWrapper, long now) {
    long oldNetUsage = accountWrapper.getNetUsage();
    long latestConsumeTime = accountWrapper.getLatestConsumeTime();
    accountWrapper.setNetUsage(increase(oldNetUsage, 0, latestConsumeTime, now));
    long oldFreeNetUsage = accountWrapper.getFreeNetUsage();
    long latestConsumeFreeTime = accountWrapper.getLatestConsumeFreeTime();
    accountWrapper.setFreeNetUsage(increase(oldFreeNetUsage, 0, latestConsumeFreeTime, now));
    Map<String, Long> assetMap = accountWrapper.getAssetMap();
    assetMap.forEach((assetName, balance) -> {
      long oldFreeAssetNetUsage = accountWrapper.getFreeAssetNetUsage(assetName);
      long latestAssetOperationTime = accountWrapper.getLatestAssetOperationTime(assetName);
      accountWrapper.putFreeAssetNetUsage(assetName,
          increase(oldFreeAssetNetUsage, 0, latestAssetOperationTime, now));
    });
  }

  public void consumeBandwidth(TransactionWrapper trx, TransactionResultWrapper ret)
      throws ContractValidateException, AccountResourceInsufficientException {
    List<Contract> contracts =
        trx.getInstance().getRawData().getContractList();

    for (Contract contract : contracts) {
      long bytes = trx.getSerializedSize();
      logger.debug("trxId {},bandwidth cost :{}", trx.getTransactionId(), bytes);
      byte[] address = TransactionWrapper.getOwner(contract);
      AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
      if (accountWrapper == null) {
        throw new ContractValidateException("account not exists");
      }
      long now = dbManager.getWitnessController().getHeadSlot();

      if (contractCreateNewAccount(contract)) {
        consumeForCreateNewAccount(accountWrapper, bytes, now, ret);
        continue;
      }

      if (contract.getType() == TransferAssetContract) {
        if (useAssetAccountNet(contract, accountWrapper, now, bytes)) {
          continue;
        }
      }

      if (useAccountNet(accountWrapper, bytes, now)) {
        continue;
      }

      if (useFreeNet(accountWrapper, bytes, now)) {
        continue;
      }

      if (useTransactionFee(accountWrapper, bytes, ret)) {
        continue;
      }

      throw new AccountResourceInsufficientException(
          "Account Insufficient bandwidth and balance to create new account");
    }
  }

  private boolean consumeFee(AccountWrapper accountWrapper, long fee) {
    try {
      long latestOperationTime = dbManager.getHeadBlockTimeStamp();
      accountWrapper.setLatestOperationTime(latestOperationTime);
      dbManager.adjustBalance(accountWrapper, -fee);
      return true;
    } catch (BalanceInsufficientException e) {
      return false;
    }
  }

  private boolean useTransactionFee(AccountWrapper accountWrapper, long bytes,
                                    TransactionResultWrapper ret) {
    long fee = dbManager.getDynamicPropertiesStore().getTransactionFee() * bytes;
    if (consumeFee(accountWrapper, fee)) {
      ret.addFee(fee);
      dbManager.getDynamicPropertiesStore().addTotalTransactionCost(fee);
      return true;
    } else {
      return false;
    }
  }

  private void consumeForCreateNewAccount(AccountWrapper accountWrapper, long bytes,
                                          long now, TransactionResultWrapper resultCapsule)
      throws AccountResourceInsufficientException {
    boolean ret = consumeBandwidthForCreateNewAccount(accountWrapper, bytes, now);

    if (!ret) {
      ret = consumeFeeForCreateNewAccount(accountWrapper, resultCapsule);
      if (!ret) {
        throw new AccountResourceInsufficientException();
      }
    }
  }

  public boolean consumeBandwidthForCreateNewAccount(AccountWrapper accountWrapper, long bytes,
                                                     long now) {
    long netUsage = accountWrapper.getNetUsage();
    long latestConsumeTime = accountWrapper.getLatestConsumeTime();
    long netLimit = calculateGlobalNetLimit(accountWrapper.getFrozenBalance());

    long newNetUsage = increase(netUsage, 0, latestConsumeTime, now);

    if (bytes <= (netLimit - newNetUsage)) {
      latestConsumeTime = now;
      long latestOperationTime = dbManager.getHeadBlockTimeStamp();
      newNetUsage = increase(newNetUsage, bytes, latestConsumeTime, now);
      accountWrapper.setLatestConsumeTime(latestConsumeTime);
      accountWrapper.setLatestOperationTime(latestOperationTime);
      accountWrapper.setNetUsage(newNetUsage);
      dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
      return true;
    }
    return false;
  }

  public boolean consumeFeeForCreateNewAccount(AccountWrapper accountWrapper,
      TransactionResultWrapper ret) {
    long fee = dbManager.getDynamicPropertiesStore().getCreateAccountFee();
    if (consumeFee(accountWrapper, fee)) {
      ret.addFee(fee);
      dbManager.getDynamicPropertiesStore().addTotalCreateAccountCost(fee);
      return true;
    } else {
      return false;
    }
  }

  public boolean contractCreateNewAccount(Contract contract) {
    AccountWrapper toAccount;
    switch (contract.getType()) {
      case AccountCreateContract:
        return true;
      case TransferContract:
        TransferContract transferContract;
        try {
          transferContract = contract.getParameter().unpack(TransferContract.class);
        } catch (Exception ex) {
          throw new RuntimeException(ex.getMessage());
        }
        toAccount = dbManager.getAccountStore().get(transferContract.getToAddress().toByteArray());
        return toAccount == null;
      case TransferAssetContract:
        TransferAssetContract transferAssetContract;
        try {
          transferAssetContract = contract.getParameter().unpack(TransferAssetContract.class);
        } catch (Exception ex) {
          throw new RuntimeException(ex.getMessage());
        }
        toAccount = dbManager.getAccountStore()
            .get(transferAssetContract.getToAddress().toByteArray());
        return toAccount == null;
      default:
        return false;
    }
  }


  private boolean useAssetAccountNet(Contract contract, AccountWrapper accountWrapper, long now,
                                     long bytes)
      throws ContractValidateException {

    ByteString assetName;
    try {
      assetName = contract.getParameter().unpack(TransferAssetContract.class).getAssetName();
    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage());
    }
    String assetNameString = ByteArray.toStr(assetName.toByteArray());
    AssetIssueWrapper assetIssueWrapper
        = dbManager.getAssetIssueStore().get(assetName.toByteArray());
    if (assetIssueWrapper == null) {
      throw new ContractValidateException("asset not exists");
    }

    if (assetIssueWrapper.getOwnerAddress() == accountWrapper.getAddress()) {
      return useAccountNet(accountWrapper, bytes, now);
    }

    long publicFreeAssetNetLimit = assetIssueWrapper.getPublicFreeAssetNetLimit();
    long publicFreeAssetNetUsage = assetIssueWrapper.getPublicFreeAssetNetUsage();
    long publicLatestFreeNetTime = assetIssueWrapper.getPublicLatestFreeNetTime();

    long newPublicFreeAssetNetUsage = increase(publicFreeAssetNetUsage, 0,
        publicLatestFreeNetTime, now);

    if (bytes > (publicFreeAssetNetLimit - newPublicFreeAssetNetUsage)) {
      logger.debug("The " + assetNameString + " public free bandwidth is not enough");
      return false;
    }

    long freeAssetNetLimit = assetIssueWrapper.getFreeAssetNetLimit();

    long freeAssetNetUsage = accountWrapper
        .getFreeAssetNetUsage(assetNameString);
    long latestAssetOperationTime = accountWrapper
        .getLatestAssetOperationTime(assetNameString);

    long newFreeAssetNetUsage = increase(freeAssetNetUsage, 0,
        latestAssetOperationTime, now);

    if (bytes > (freeAssetNetLimit - newFreeAssetNetUsage)) {
      logger.debug("The " + assetNameString + " free bandwidth is not enough");
      return false;
    }

    AccountWrapper issuerAccountWrapper = dbManager.getAccountStore()
        .get(assetIssueWrapper.getOwnerAddress().toByteArray());

    long issuerNetUsage = issuerAccountWrapper.getNetUsage();
    long latestConsumeTime = issuerAccountWrapper.getLatestConsumeTime();
    long issuerNetLimit = calculateGlobalNetLimit(issuerAccountWrapper.getFrozenBalance());

    long newIssuerNetUsage = increase(issuerNetUsage, 0, latestConsumeTime, now);

    if (bytes > (issuerNetLimit - newIssuerNetUsage)) {
      logger.debug("The " + assetNameString + " issuer'bandwidth is not enough");
      return false;
    }

    latestConsumeTime = now;
    latestAssetOperationTime = now;
    publicLatestFreeNetTime = now;
    long latestOperationTime = dbManager.getHeadBlockTimeStamp();
    newIssuerNetUsage = increase(newIssuerNetUsage, bytes, latestConsumeTime, now);
    newFreeAssetNetUsage = increase(newFreeAssetNetUsage,
        bytes, latestAssetOperationTime, now);
    newPublicFreeAssetNetUsage = increase(newPublicFreeAssetNetUsage, bytes,
        publicLatestFreeNetTime, now);

    issuerAccountWrapper.setNetUsage(newIssuerNetUsage);
    issuerAccountWrapper.setLatestConsumeTime(latestConsumeTime);

    accountWrapper.setLatestOperationTime(latestOperationTime);
    accountWrapper.putLatestAssetOperationTimeMap(assetNameString,
        latestAssetOperationTime);
    accountWrapper.putFreeAssetNetUsage(assetNameString, newFreeAssetNetUsage);

    assetIssueWrapper.setPublicFreeAssetNetUsage(newPublicFreeAssetNetUsage);
    assetIssueWrapper.setPublicLatestFreeNetTime(publicLatestFreeNetTime);

    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    dbManager.getAccountStore().put(issuerAccountWrapper.createDbKey(),
            issuerAccountWrapper);
    dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);

    return true;

  }

  public long calculateGlobalNetLimit(long frozeBalance) {
    if (frozeBalance < 1000_000L) {
      return 0;
    }
    long netWeight = frozeBalance / 1000_000L;
    long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
    long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
    assert totalNetWeight > 0;
    return (long) (netWeight * ((double) totalNetLimit / totalNetWeight));
  }

  private boolean useAccountNet(AccountWrapper accountWrapper, long bytes, long now) {

    long netUsage = accountWrapper.getNetUsage();
    long latestConsumeTime = accountWrapper.getLatestConsumeTime();
    long netLimit = calculateGlobalNetLimit(accountWrapper.getFrozenBalance());

    long newNetUsage = increase(netUsage, 0, latestConsumeTime, now);

    if (bytes > (netLimit - newNetUsage)) {
      logger.debug("net usage is running out. now use free net usage");
      return false;
    }

    latestConsumeTime = now;
    long latestOperationTime = dbManager.getHeadBlockTimeStamp();
    newNetUsage = increase(newNetUsage, bytes, latestConsumeTime, now);
    accountWrapper.setNetUsage(newNetUsage);
    accountWrapper.setLatestOperationTime(latestOperationTime);
    accountWrapper.setLatestConsumeTime(latestConsumeTime);

    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    return true;
  }

  private boolean useFreeNet(AccountWrapper accountWrapper, long bytes, long now) {

    long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
    long freeNetUsage = accountWrapper.getFreeNetUsage();
    long latestConsumeFreeTime = accountWrapper.getLatestConsumeFreeTime();
    long newFreeNetUsage = increase(freeNetUsage, 0, latestConsumeFreeTime, now);

    if (bytes > (freeNetLimit - newFreeNetUsage)) {
      logger.debug("free net usage is running out");
      return false;
    }

    long publicNetLimit = dbManager.getDynamicPropertiesStore().getPublicNetLimit();
    long publicNetUsage = dbManager.getDynamicPropertiesStore().getPublicNetUsage();
    long publicNetTime = dbManager.getDynamicPropertiesStore().getPublicNetTime();

    long newPublicNetUsage = increase(publicNetUsage, 0, publicNetTime, now);

    if (bytes > (publicNetLimit - newPublicNetUsage)) {
      logger.debug("free public net usage is running out");
      return false;
    }

    latestConsumeFreeTime = now;
    long latestOperationTime = dbManager.getHeadBlockTimeStamp();
    publicNetTime = now;
    newFreeNetUsage = increase(newFreeNetUsage, bytes, latestConsumeFreeTime, now);
    newPublicNetUsage = increase(newPublicNetUsage, bytes, publicNetTime, now);
    accountWrapper.setFreeNetUsage(newFreeNetUsage);
    accountWrapper.setLatestConsumeFreeTime(latestConsumeFreeTime);
    accountWrapper.setLatestOperationTime(latestOperationTime);

    dbManager.getDynamicPropertiesStore().savePublicNetUsage(newPublicNetUsage);
    dbManager.getDynamicPropertiesStore().savePublicNetTime(publicNetTime);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    return true;

  }

}


