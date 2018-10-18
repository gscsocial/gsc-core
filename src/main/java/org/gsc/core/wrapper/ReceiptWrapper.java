package org.gsc.core.wrapper;

import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.Constant;
import org.gsc.db.EnergyProcessor;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol.ResourceReceipt;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;

public class ReceiptWrapper {

  private ResourceReceipt receipt;

  private Sha256Hash receiptAddress;

  public ReceiptWrapper(ResourceReceipt data, Sha256Hash receiptAddress) {
    this.receipt = data;
    this.receiptAddress = receiptAddress;
  }

  public ReceiptWrapper(Sha256Hash receiptAddress) {
    this.receipt = ResourceReceipt.newBuilder().build();
    this.receiptAddress = receiptAddress;
  }

  public void setReceipt(ResourceReceipt receipt) {
    this.receipt = receipt;
  }

  public ResourceReceipt getReceipt() {
    return this.receipt;
  }

  public Sha256Hash getReceiptAddress() {
    return this.receiptAddress;
  }

  public void setNetUsage(long netUsage) {
    this.receipt = this.receipt.toBuilder().setNetUsage(netUsage).build();
  }

  public void setNetFee(long netFee) {
    this.receipt = this.receipt.toBuilder().setNetFee(netFee).build();
  }

  public long getEnergyUsage() {
    return this.receipt.getEnergyUsage();
  }

  public long getEnergyFee() {
    return this.receipt.getEnergyFee();
  }

  public void setEnergyUsage(long energyUsage) {
    this.receipt = this.receipt.toBuilder().setEnergyUsage(energyUsage).build();
  }

  public void setEnergyFee(long energyFee) {
    this.receipt = this.receipt.toBuilder().setEnergyFee(energyFee).build();
  }

  public long getOriginEnergyUsage() {
    return this.receipt.getOriginEnergyUsage();
  }

  public long getEnergyUsageTotal() {
    return this.receipt.getEnergyUsageTotal();
  }

  public void setOriginEnergyUsage(long energyUsage) {
    this.receipt = this.receipt.toBuilder().setOriginEnergyUsage(energyUsage).build();
  }

  public void setEnergyUsageTotal(long energyUsage) {
    this.receipt = this.receipt.toBuilder().setEnergyUsageTotal(energyUsage).build();
  }

  public long getNetUsage() {
    return this.receipt.getNetUsage();
  }

  public long getNetFee() {
    return this.receipt.getNetFee();
  }

  /**
   * payEnergyBill pay receipt energy bill by energy processor.
   */
  public void payEnergyBill(Manager manager, AccountWrapper origin, AccountWrapper caller,
                            long percent, EnergyProcessor energyProcessor, long now) {
    if (0 == receipt.getEnergyUsageTotal()) {
      return;
    }

    if (caller.getAddress().equals(origin.getAddress())) {
      payEnergyBill(manager, caller, receipt.getEnergyUsageTotal(), energyProcessor, now);
    } else {
      long originUsage = Math.multiplyExact(receipt.getEnergyUsageTotal(), percent) / 100;
      originUsage = Math
              .min(originUsage, energyProcessor.getAccountLeftEnergyFromFreeze(origin));
      long callerUsage = receipt.getEnergyUsageTotal() - originUsage;
      energyProcessor.useEnergy(origin, originUsage, now);
      this.setOriginEnergyUsage(originUsage);
      payEnergyBill(manager, caller, callerUsage, energyProcessor, now);
    }
  }

  private void payEnergyBill(
          Manager manager,
          AccountWrapper account,
          long usage,
          EnergyProcessor energyProcessor,
          long now) {
    long accountEnergyLeft = energyProcessor.getAccountLeftEnergyFromFreeze(account);
    if (accountEnergyLeft >= usage) {
      energyProcessor.useEnergy(account, usage, now);
      this.setEnergyUsage(usage);
    } else {
      energyProcessor.useEnergy(account, accountEnergyLeft, now);
      long SUN_PER_ENERGY = manager.getDynamicPropertiesStore().getEnergyFee() == 0
              ? Constant.SUN_PER_ENERGY
              : manager.getDynamicPropertiesStore().getEnergyFee();
      long energyFee =
              (usage - accountEnergyLeft) * SUN_PER_ENERGY;
      this.setEnergyUsage(accountEnergyLeft);
      this.setEnergyFee(energyFee);
      account.setBalance(account.getBalance() - energyFee);
    }

    manager.getAccountStore().put(account.getAddress().toByteArray(), account);
  }

  public static ResourceReceipt copyReceipt(ReceiptWrapper origin) {
    return origin.getReceipt().toBuilder().build();
  }

  public void setResult(contractResult success) {
    this.receipt = receipt.toBuilder().setResult(success).build();
  }

  public contractResult getResult() {
    return this.receipt.getResult();
  }
}
