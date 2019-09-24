/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.core.wrapper;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import org.gsc.db.CpuProcessor;
import org.gsc.runtime.config.VMConfig;
import org.gsc.utils.Sha256Hash;
import org.gsc.utils.StringUtil;
import org.gsc.core.Constant;
import org.gsc.db.Manager;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.protos.Protocol.ResourceReceipt;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;

public class ReceiptWrapper {

    private ResourceReceipt receipt;
    @Getter
    @Setter
    private long multiSignFee;

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

    public void addNetFee(long netFee) {
        this.receipt = this.receipt.toBuilder().setNetFee(getNetFee() + netFee).build();
    }

    public long getCpuUsage() {
        return this.receipt.getCpuUsage();
    }

    public long getCpuFee() {
        return this.receipt.getCpuFee();
    }

    public void setCpuUsage(long cpuUsage) {
        this.receipt = this.receipt.toBuilder().setCpuUsage(cpuUsage).build();
    }

    public void setCpuFee(long cpuFee) {
        this.receipt = this.receipt.toBuilder().setCpuFee(cpuFee).build();
    }

    public long getOriginCpuUsage() {
        return this.receipt.getOriginCpuUsage();
    }

    public long getCpuUsageTotal() {
        return this.receipt.getCpuUsageTotal();
    }

    public void setOriginCpuUsage(long cpuUsage) {
        this.receipt = this.receipt.toBuilder().setOriginCpuUsage(cpuUsage).build();
    }

    public void setCpuUsageTotal(long cpuUsage) {
        this.receipt = this.receipt.toBuilder().setCpuUsageTotal(cpuUsage).build();
    }

    public long getNetUsage() {
        return this.receipt.getNetUsage();
    }

    public long getNetFee() {
        return this.receipt.getNetFee();
    }

    /**
     * payCpuBill pay receipt cpu bill by cpu processor.
     */
    public void payCpuBill(Manager manager, AccountWrapper origin, AccountWrapper caller,
                           long percent, long originCpuLimit, CpuProcessor cpuProcessor, long now)
            throws BalanceInsufficientException {
        if (receipt.getCpuUsageTotal() <= 0) {
            return;
        }

        if (Objects.isNull(origin) && VMConfig.allowGvmConstantinople()) {
            payCpuBill(manager, caller, receipt.getCpuUsageTotal(), cpuProcessor, now);
            return;
        }

        if (caller.getAddress().equals(origin.getAddress())) {
            payCpuBill(manager, caller, receipt.getCpuUsageTotal(), cpuProcessor, now);
        } else {
            long originUsage = Math.multiplyExact(receipt.getCpuUsageTotal(), percent) / 100;
            originUsage = getOriginUsage(manager, origin, originCpuLimit, cpuProcessor,
                    originUsage);

            long callerUsage = receipt.getCpuUsageTotal() - originUsage;
            cpuProcessor.useCpu(origin, originUsage, now);
            this.setOriginCpuUsage(originUsage);
            payCpuBill(manager, caller, callerUsage, cpuProcessor, now);
        }
    }

    private long getOriginUsage(Manager manager, AccountWrapper origin,
                                long originCpuLimit,
                                CpuProcessor cpuProcessor, long originUsage) {

//        if (VMConfig.getCpuLimitHardFork()) {
            return Math.min(originUsage,
                    Math.min(cpuProcessor.getAccountLeftCpuFromFreeze(origin), originCpuLimit));
//        }
//        return Math.min(originUsage, cpuProcessor.getAccountLeftCpuFromFreeze(origin));
    }

    private void payCpuBill(
            Manager manager,
            AccountWrapper account,
            long usage,
            CpuProcessor cpuProcessor,
            long now) throws BalanceInsufficientException {
        long accountCpuLeft = cpuProcessor.getAccountLeftCpuFromFreeze(account);
        if (accountCpuLeft >= usage) {
            cpuProcessor.useCpu(account, usage, now);
            this.setCpuUsage(usage);
        } else {
            cpuProcessor.useCpu(account, accountCpuLeft, now);
            long dotPerCpu = Constant.DOT_PER_CPU;
            long dynamicCpuFee = manager.getDynamicPropertiesStore().getCpuFee();
            if (dynamicCpuFee > 0) {
                dotPerCpu = dynamicCpuFee;
            }
            long cpuFee =
                    (usage - accountCpuLeft) * dotPerCpu;
            this.setCpuUsage(accountCpuLeft);
            this.setCpuFee(cpuFee);
            long balance = account.getBalance();
            if (balance < cpuFee) {
                throw new BalanceInsufficientException(
                        StringUtil.createReadableString(account.createDbKey()) + " insufficient balance");
            }
            account.setBalance(balance - cpuFee);

            //send to blackHole
            manager.adjustBalance(manager.getAccountStore().getBlackhole().getAddress().toByteArray(),
                    cpuFee);
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
