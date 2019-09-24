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

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.config.Parameter.ChainConstant;

@Slf4j(topic = "DB")
public class StorageMarket {

    private Manager dbManager;
    private long supply = 1_000_000_000_000_000L;

    public StorageMarket(Manager manager) {
        this.dbManager = manager;
    }

    private long exchange_to_supply(boolean GSC, long quant) {
        logger.info("isGSC: " + GSC);
        long balance = GSC ? dbManager.getDynamicPropertiesStore().getTotalStoragePool() :
                dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
        logger.info("balance: " + balance);
        long newBalance = balance + quant;
        logger.info("balance + quant: " + (balance + quant));

//    if (GSC) {
//      dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newBalance);
//    } else {
//      dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newBalance);
//    }

        double issuedSupply = -supply * (1.0 - Math.pow(1.0 + (double) quant / newBalance, 0.0005));
        logger.info("issuedSupply: " + issuedSupply);
        long out = (long) issuedSupply;
        supply += out;

        return out;
    }

    private long exchange_to_supply2(boolean isGSC, long quant) {
        logger.info("isGSC: " + isGSC);

        long balance = isGSC ? dbManager.getDynamicPropertiesStore().getTotalStoragePool() :
                dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
        logger.info("balance: " + balance);
        long newBalance = balance - quant;
        logger.info("balance - quant: " + (balance - quant));

//    if (isGSC) {
//      dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newBalance);
//    } else {
//      dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newBalance);
//    }

        double issuedSupply = -supply * (1.0 - Math.pow(1.0 + (double) quant / newBalance, 0.0005));
        logger.info("issuedSupply: " + issuedSupply);
        long out = (long) issuedSupply;
        supply += out;

        return out;
    }

    private long exchange_from_supply(boolean isGSC, long supplyQuant) {
        long balance = isGSC ? dbManager.getDynamicPropertiesStore().getTotalStoragePool() :
                dbManager.getDynamicPropertiesStore().getTotalStorageReserved();
        supply -= supplyQuant;

        double exchangeBalance =
                balance * (Math.pow(1.0 + (double) supplyQuant / supply, 2000.0) - 1.0);
        logger.info("exchangeBalance: " + exchangeBalance);
        long out = (long) exchangeBalance;

        if (isGSC) {
            out = Math.round(exchangeBalance / 100000) * 100000;
            logger.info("---out: " + out);
        }

        return out;
    }

    public long exchange(long from, boolean isGSC) {
        long relay = exchange_to_supply(isGSC, from);
        return exchange_from_supply(!isGSC, relay);
    }

    public long calculateTax(long duration, long limit) {
        // todo: Support for change by the committee
        double ratePerYear = dbManager.getDynamicPropertiesStore().getStorageExchangeTaxRate() / 100.0;
        double millisecondPerYear = (double) ChainConstant.MS_PER_YEAR;
        double feeRate = duration / millisecondPerYear * ratePerYear;
        long storageTax = (long) (limit * feeRate);
        logger.info("storageTax: " + storageTax);
        return storageTax;
    }


    public long tryPayTax(long duration, long limit) {
        long storageTax = calculateTax(duration, limit);
        long tax = exchange(storageTax, false);
        logger.info("tax: " + tax);

        long newTotalTax = dbManager.getDynamicPropertiesStore().getTotalStorageTax() + tax;
        long newTotalPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool() - tax;
        long newTotalReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
                + storageTax;
        logger.info("reserved: " + dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
        boolean eq = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
                == 128L * 1024 * 1024 * 1024;
        logger.info("reserved == 128GB: " + eq);
        logger.info("newTotalTax: " + newTotalTax + "  newTotalPool: " + newTotalPool
                + "  newTotalReserved: " + newTotalReserved);

        return storageTax;
    }

    public long payTax(long duration, long limit) {
        long storageTax = calculateTax(duration, limit);
        long tax = exchange(storageTax, false);
        logger.info("tax: " + tax);

        long newTotalTax = dbManager.getDynamicPropertiesStore().getTotalStorageTax() + tax;
        long newTotalPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool() - tax;
        long newTotalReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
                + storageTax;
        logger.info("reserved: " + dbManager.getDynamicPropertiesStore().getTotalStorageReserved());
        boolean eq = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
                == 128L * 1024 * 1024 * 1024;
        logger.info("reserved == 128GB: " + eq);
        logger.info("newTotalTax: " + newTotalTax + "  newTotalPool: " + newTotalPool
                + "  newTotalReserved: " + newTotalReserved);
        dbManager.getDynamicPropertiesStore().saveTotalStorageTax(newTotalTax);
        dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newTotalPool);
        dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newTotalReserved);

        return storageTax;
    }

    public long tryBuyStorageBytes(long storageBought) {
        long relay = exchange_to_supply2(false, storageBought);
        return exchange_from_supply(true, relay);
    }

    public long tryBuyStorage(long quant) {
        return exchange(quant, true);
    }

    public long trySellStorage(long bytes) {
        return exchange(bytes, false);
    }

    public AccountWrapper buyStorageBytes(AccountWrapper accountWrapper, long storageBought) {
        long now = dbManager.getHeadBlockTimeStamp();
        long currentStorageLimit = accountWrapper.getStorageLimit();

        long relay = exchange_to_supply2(false, storageBought);
        long quant = exchange_from_supply(true, relay);

        long newBalance = accountWrapper.getBalance() - quant;
        logger.info("newBalance： " + newBalance);

        long newStorageLimit = currentStorageLimit + storageBought;
        logger.info(
                "storageBought: " + storageBought + "  newStorageLimit: "
                        + newStorageLimit);

        accountWrapper.setLatestExchangeStorageTime(now);
        accountWrapper.setStorageLimit(newStorageLimit);
        accountWrapper.setBalance(newBalance);
        dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

        long newTotalPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool() + quant;
        long newTotalReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
                - storageBought;
        logger.info("newTotalPool: " + newTotalPool + "  newTotalReserved: " + newTotalReserved);
        dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newTotalPool);
        dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newTotalReserved);
        return accountWrapper;
    }


    public void buyStorage(AccountWrapper accountWrapper, long quant) {
        long now = dbManager.getHeadBlockTimeStamp();
        long currentStorageLimit = accountWrapper.getStorageLimit();

        long newBalance = accountWrapper.getBalance() - quant;
        logger.info("newBalance： " + newBalance);

        long storageBought = exchange(quant, true);
        long newStorageLimit = currentStorageLimit + storageBought;
        logger.info(
                "storageBought: " + storageBought + "  newStorageLimit: "
                        + newStorageLimit);

        accountWrapper.setLatestExchangeStorageTime(now);
        accountWrapper.setStorageLimit(newStorageLimit);
        accountWrapper.setBalance(newBalance);
        dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

        long newTotalPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool() + quant;
        long newTotalReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
                - storageBought;
        logger.info("newTotalPool: " + newTotalPool + "  newTotalReserved: " + newTotalReserved);
        dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newTotalPool);
        dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newTotalReserved);

    }

    public void sellStorage(AccountWrapper accountWrapper, long bytes) {
        long now = dbManager.getHeadBlockTimeStamp();
        long currentStorageLimit = accountWrapper.getStorageLimit();

        long quant = exchange(bytes, false);
        long newBalance = accountWrapper.getBalance() + quant;

        long newStorageLimit = currentStorageLimit - bytes;
        logger.info("quant: " + quant + "  newStorageLimit: " + newStorageLimit);

        accountWrapper.setLatestExchangeStorageTime(now);
        accountWrapper.setStorageLimit(newStorageLimit);
        accountWrapper.setBalance(newBalance);
        dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

        long newTotalPool = dbManager.getDynamicPropertiesStore().getTotalStoragePool() - quant;
        long newTotalReserved = dbManager.getDynamicPropertiesStore().getTotalStorageReserved()
                + bytes;
        logger.info("newTotalPool: " + newTotalPool + "  newTotalReserved: " + newTotalReserved);
        dbManager.getDynamicPropertiesStore().saveTotalStoragePool(newTotalPool);
        dbManager.getDynamicPropertiesStore().saveTotalStorageReserved(newTotalReserved);

    }

    public long getAccountLeftStorageInByteFromBought(AccountWrapper accountWrapper) {
        return accountWrapper.getStorageLimit() - accountWrapper.getStorageUsage();
    }
}
