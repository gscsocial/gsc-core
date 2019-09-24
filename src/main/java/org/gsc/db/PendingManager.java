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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.TransactionTrace.TimeResultType;

@Slf4j(topic = "DB")
public class PendingManager implements AutoCloseable {

    @Getter
    private List<TransactionWrapper> tmpTransactions = new ArrayList<>();
    private Manager dbManager;

    public PendingManager(Manager db) {

        this.dbManager = db;
        tmpTransactions.addAll(db.getPendingTransactions());
        db.getPendingTransactions().clear();
        db.getSession().reset();
    }

    @Override
    public void close() {

        for (TransactionWrapper tx : tmpTransactions) {
            try {
                if (tx.getTrxTrace() != null &&
                        tx.getTrxTrace().getTimeResultType().equals(TimeResultType.NORMAL)) {
                    dbManager.getRepushTransactions().put(tx);
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        tmpTransactions.clear();

        for (TransactionWrapper tx : dbManager.getPoppedTransactions()) {
            try {
                if (tx.getTrxTrace() != null &&
                        tx.getTrxTrace().getTimeResultType().equals(TimeResultType.NORMAL)) {
                    dbManager.getRepushTransactions().put(tx);
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        dbManager.getPoppedTransactions().clear();
    }
}
