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

import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.config.Parameter.AdaptiveResourceLimitConstants;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.TooBigTransactionResultException;

abstract class ResourceProcessor {

    protected Manager dbManager;
    protected long precision;
    protected long windowSize;
    protected long averageWindowSize;

    public ResourceProcessor(Manager manager) {
        this.dbManager = manager;
        this.precision = ChainConstant.PRECISION;
        this.windowSize = ChainConstant.WINDOW_SIZE_MS / ChainConstant.BLOCK_PRODUCED_INTERVAL;
        this.averageWindowSize =
                AdaptiveResourceLimitConstants.PERIODS_MS / ChainConstant.BLOCK_PRODUCED_INTERVAL;
    }

    abstract void updateUsage(AccountWrapper accountWrapper);

    abstract void consume(TransactionWrapper trx, TransactionTrace trace)
            throws ContractValidateException, AccountResourceInsufficientException, TooBigTransactionResultException;

    protected long increase(long lastUsage, long usage, long lastTime, long now) {
        return increase(lastUsage, usage, lastTime, now, windowSize);
    }

    protected long increase(long lastUsage, long usage, long lastTime, long now, long windowSize) {
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
        return getUsage(averageLastUsage, windowSize);
    }

    private long divideCeil(long numerator, long denominator) {
        return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
    }

    private long getUsage(long usage, long windowSize) {
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
