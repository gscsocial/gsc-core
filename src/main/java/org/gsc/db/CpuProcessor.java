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

package org.gsc.db;

import static java.lang.Long.max;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.config.Parameter.AdaptiveResourceLimitConstants;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Protocol.Account.AccountResource;

@Slf4j(topic = "DB")
public class CpuProcessor extends ResourceProcessor {

    public CpuProcessor(Manager manager) {
        super(manager);
    }

    @Override
    public void updateUsage(AccountWrapper accountWrapper) {
        long now = dbManager.getWitnessController().getHeadSlot();
        updateUsage(accountWrapper, now);
    }

    private void updateUsage(AccountWrapper accountWrapper, long now) {
        AccountResource accountResource = accountWrapper.getAccountResource();

        long oldCpuUsage = accountResource.getCpuUsage();
        long latestConsumeTime = accountResource.getLatestConsumeTimeForCpu();

        accountWrapper.setCpuUsage(increase(oldCpuUsage, 0, latestConsumeTime, now));
    }

    public void updateTotalCpuAverageUsage() {
        long now = dbManager.getWitnessController().getHeadSlot();
        long blockCpuUsage = dbManager.getDynamicPropertiesStore().getBlockCpuUsage();
        long totalCpuAverageUsage = dbManager.getDynamicPropertiesStore()
                .getTotalCpuAverageUsage();
        long totalCpuAverageTime = dbManager.getDynamicPropertiesStore().getTotalCpuAverageTime();

        long newPublicCpuAverageUsage = increase(totalCpuAverageUsage, blockCpuUsage,
                totalCpuAverageTime, now, averageWindowSize);

        dbManager.getDynamicPropertiesStore().saveTotalCpuAverageUsage(newPublicCpuAverageUsage);
        dbManager.getDynamicPropertiesStore().saveTotalCpuAverageTime(now);
    }

    public void updateAdaptiveTotalCpuLimit() {
        long totalCpuAverageUsage = dbManager.getDynamicPropertiesStore()
                .getTotalCpuAverageUsage();
        long targetTotalCpuLimit = dbManager.getDynamicPropertiesStore().getTotalCpuTargetLimit();
        long totalCpuCurrentLimit = dbManager.getDynamicPropertiesStore()
                .getTotalCpuCurrentLimit();
        long totalCpuLimit = dbManager.getDynamicPropertiesStore().getTotalCpuLimit();

        long result;
        if (totalCpuAverageUsage > targetTotalCpuLimit) {
            result = totalCpuCurrentLimit * AdaptiveResourceLimitConstants.CONTRACT_RATE_NUMERATOR
                    / AdaptiveResourceLimitConstants.CONTRACT_RATE_DENOMINATOR;
            // logger.info(totalCpuAverageUsage + ">" + targetTotalCpuLimit + "\n" + result);
        } else {
            result = totalCpuCurrentLimit * AdaptiveResourceLimitConstants.EXPAND_RATE_NUMERATOR
                    / AdaptiveResourceLimitConstants.EXPAND_RATE_DENOMINATOR;
            // logger.info(totalCpuAverageUsage + "<" + targetTotalCpuLimit + "\n" + result);
        }

        result = Math.min(
                Math.max(result, totalCpuLimit),
                totalCpuLimit * AdaptiveResourceLimitConstants.LIMIT_MULTIPLIER
        );

        dbManager.getDynamicPropertiesStore().saveTotalCpuCurrentLimit(result);
        logger.debug(
                "adjust totalCpuCurrentLimit, old[" + totalCpuCurrentLimit + "], new[" + result
                        + "]");
    }

    @Override
    public void consume(TransactionWrapper trx,
                        TransactionTrace trace)
            throws ContractValidateException, AccountResourceInsufficientException {
        throw new RuntimeException("Not support");
    }


    public boolean useCpu(AccountWrapper accountWrapper, long cpu, long now) {

        long cpuUsage = accountWrapper.getCpuUsage();
        long latestConsumeTime = accountWrapper.getAccountResource().getLatestConsumeTimeForCpu();
        long cpuLimit = calculateGlobalCpuLimit(accountWrapper);

        long newCpuUsage = increase(cpuUsage, 0, latestConsumeTime, now);

        if (cpu > (cpuLimit - newCpuUsage)) {
            return false;
        }

        latestConsumeTime = now;
        long latestOperationTime = dbManager.getHeadBlockTimeStamp();
        newCpuUsage = increase(newCpuUsage, cpu, latestConsumeTime, now);
        accountWrapper.setCpuUsage(newCpuUsage);
        accountWrapper.setLatestOperationTime(latestOperationTime);
        accountWrapper.setLatestConsumeTimeForCpu(latestConsumeTime);

        dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

        if (dbManager.getDynamicPropertiesStore().getAllowAdaptiveCpu() == 1) {
            long blockCpuUsage = dbManager.getDynamicPropertiesStore().getBlockCpuUsage() + cpu;
            dbManager.getDynamicPropertiesStore().saveBlockCpuUsage(blockCpuUsage);
        }

        return true;
    }


    public long calculateGlobalCpuLimit(AccountWrapper accountWrapper) {
        long frozeBalance = accountWrapper.getAllFrozenBalanceForCpu();
        if (frozeBalance < 1_000_000L) {
            return 0;
        }

        long cpuWeight = frozeBalance / 1_000_000L;
        long totalCpuLimit = dbManager.getDynamicPropertiesStore().getTotalCpuCurrentLimit();
        long totalCpuWeight = dbManager.getDynamicPropertiesStore().getTotalCpuWeight();

        assert totalCpuWeight > 0;

        return (long) (cpuWeight * ((double) totalCpuLimit / totalCpuWeight));
    }

    public long getAccountLeftCpuFromFreeze(AccountWrapper accountWrapper) {

        long now = dbManager.getWitnessController().getHeadSlot();

        long cpuUsage = accountWrapper.getCpuUsage();
        long latestConsumeTime = accountWrapper.getAccountResource().getLatestConsumeTimeForCpu();
        long cpuLimit = calculateGlobalCpuLimit(accountWrapper);

        long newCpuUsage = increase(cpuUsage, 0, latestConsumeTime, now);

        return max(cpuLimit - newCpuUsage, 0); // us
    }

}


