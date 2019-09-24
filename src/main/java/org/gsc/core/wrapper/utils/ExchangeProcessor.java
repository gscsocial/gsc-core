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

package org.gsc.core.wrapper.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "wrapper")
public class ExchangeProcessor {

    private long supply;

    public ExchangeProcessor(long supply) {
        this.supply = supply;
    }

    private long exchangeToSupply(long balance, long quant) {
        logger.debug("balance: " + balance);
        long newBalance = balance + quant;
        logger.debug("balance + quant: " + newBalance);

        double issuedSupply = -supply * (1.0 - Math.pow(1.0 + (double) quant / newBalance, 0.0005));
        logger.debug("issuedSupply: " + issuedSupply);
        long out = (long) issuedSupply;
        supply += out;

        return out;
    }

    private long exchangeFromSupply(long balance, long supplyQuant) {
        supply -= supplyQuant;

        double exchangeBalance =
                balance * (Math.pow(1.0 + (double) supplyQuant / supply, 2000.0) - 1.0);
        logger.debug("exchangeBalance: " + exchangeBalance);

        return (long) exchangeBalance;
    }

    public long exchange(long sellTokenBalance, long buyTokenBalance, long sellTokenQuant) {
        long relay = exchangeToSupply(sellTokenBalance, sellTokenQuant);
        return exchangeFromSupply(buyTokenBalance, relay);
    }

}
