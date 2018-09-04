package org.gsc.db;


import static java.lang.Long.max;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Protocol.Account.AccountResource;
import org.gsc.protos.Protocol.Transaction.Contract;

@Slf4j
public class EnergyProcessor extends ResourceProcessor {

  public EnergyProcessor(Manager manager) {
    super(manager);
  }

  @Override
  public void updateUsage(AccountWrapper accountWrapper) {
    long now = dbManager.getWitnessController().getHeadSlot();
    updateUsage(accountWrapper, now);
  }

  private void updateUsage(AccountWrapper accountWrapper, long now) {
    AccountResource accountResource = accountWrapper.getAccountResource();

    long oldEnergyUsage = accountResource.getEnergyUsage();
    long latestConsumeTime = accountResource.getLatestConsumeTimeForEnergy();

    accountWrapper.setEnergyUsage(increase(oldEnergyUsage, 0, latestConsumeTime, now));

  }

  @Override
  public void consume(TransactionWrapper trx, TransactionResultWrapper ret,
                      TransactionTrace trace)
      throws ContractValidateException, AccountResourceInsufficientException {
    List<Contract> contracts =
        trx.getInstance().getRawData().getContractList();

    for (Contract contract : contracts) {

      //todo
//      if (contract.isPrecompiled()) {
//        continue;
//      }
      //todo
//      long energy = trx.getReceipt().getEnergy();
      long energy = 100L;
      logger.debug("trxId {},energy cost :{}", trx.getTransactionId(), energy);
      byte[] address = TransactionWrapper.getOwner(contract);
      AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
      if (accountWrapper == null) {
        throw new ContractValidateException("account not exists");
      }
      long now = dbManager.getWitnessController().getHeadSlot();

      //todo
//      int creatorRatio = contract.getUserEnergyConsumeRatio();
      int creatorRatio = 50;

      long creatorEnergy = energy * creatorRatio / 100;
      AccountWrapper contractProvider = dbManager.getAccountStore()
          .get(contract.getProvider().toByteArray());

      if (!useEnergy(contractProvider, creatorEnergy, now)) {
        throw new ContractValidateException(
            "creator has not enough energy[" + creatorEnergy + "]");
      }

      long userEnergy = energy * (100 - creatorRatio) / 100;
      //1.The creator and the use of this have sufficient resources
      if (useEnergy(accountWrapper, userEnergy, now)) {
        continue;
      }

//     todo  long feeLimit = getUserFeeLimit();
      long feeLimit = 1000000;//sun
      long fee = calculateFee(userEnergy);
      if (fee > feeLimit) {
        throw new AccountResourceInsufficientException(
            "Account has Insufficient Energy[" + userEnergy + "] and feeLimit[" + feeLimit
                + "] is not enough to trigger this contract");
      }

      //2.The creator of this have sufficient resources
      if (useFee(accountWrapper, fee, ret)) {
        continue;
      }

      throw new AccountResourceInsufficientException(
          "Account has insufficient Energy[" + userEnergy + "] and balance[" + fee
              + "] to trigger this contract");
    }
  }

  private long calculateFee(long userEnergy) {
    return userEnergy * 30;// 30 drop / macroSecond, move to dynamicStore later
  }


  private boolean useFee(AccountWrapper accountWrapper, long fee,
                         TransactionResultWrapper ret) {
    if (consumeFee(accountWrapper, fee)) {
      ret.addFee(fee);
      return true;
    } else {
      return false;
    }
  }

  public boolean useEnergy(AccountWrapper accountWrapper, long energy, long now) {

    long energyUsage = accountWrapper.getEnergyUsage();
    long latestConsumeTime = accountWrapper.getAccountResource().getLatestConsumeTimeForEnergy();
    long energyLimit = calculateGlobalEnergyLimit(
        accountWrapper.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance());

    long newEnergyUsage = increase(energyUsage, 0, latestConsumeTime, now);

    if (energy > (energyLimit - newEnergyUsage)) {
      return false;
    }

    latestConsumeTime = now;
    long latestOperationTime = dbManager.getHeadBlockTimeStamp();
    newEnergyUsage = increase(newEnergyUsage, energy, latestConsumeTime, now);
    accountWrapper.setEnergyUsage(newEnergyUsage);
    accountWrapper.setLatestOperationTime(latestOperationTime);
    accountWrapper.setLatestConsumeTimeForEnergy(latestConsumeTime);

    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    return true;
  }


  public long calculateGlobalEnergyLimit(long frozeBalance) {
    if (frozeBalance < 1000_000L) {
      return 0;
    }
    long energyWeight = frozeBalance / 1000_000L;
    long totalEnergyLimit = dbManager.getDynamicPropertiesStore().getTotalEnergyLimit();
    long totalEnergyWeight = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    assert totalEnergyWeight > 0;
    return (long) (energyWeight * ((double) totalEnergyLimit / totalEnergyWeight));
  }

  public long getAccountLeftEnergyFromFreeze(AccountWrapper accountWrapper) {

    long now = dbManager.getWitnessController().getHeadSlot();

    long energyUsage = accountWrapper.getEnergyUsage();
    long latestConsumeTime = accountWrapper.getAccountResource().getLatestConsumeTimeForEnergy();
    long energyLimit = calculateGlobalEnergyLimit(
        accountWrapper.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance());

    long newEnergyUsage = increase(energyUsage, 0, latestConsumeTime, now);

    return max(energyLimit - newEnergyUsage, 0); // us
  }

}


