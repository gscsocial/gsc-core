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

package org.gsc.core.operator;

import com.google.protobuf.Any;
import org.gsc.db.dbsource.Deposit;
import org.gsc.db.Manager;

public abstract class AbstractOperator implements Operator {

    protected Any contract;
    protected Manager dbManager;

    public Deposit getDeposit() {
        return deposit;
    }

    public void setDeposit(Deposit deposit) {
        this.deposit = deposit;
    }

    protected Deposit deposit;

    AbstractOperator(Any contract, Manager dbManager) {
        this.contract = contract;
        this.dbManager = dbManager;
    }
}
