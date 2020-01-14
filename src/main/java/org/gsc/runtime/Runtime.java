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

package org.gsc.runtime;

import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.runtime.vm.program.InternalTransaction;


public interface Runtime {

    void execute() throws ContractValidateException, ContractExeException, VMIllegalException;

    void go();

    InternalTransaction.TrxType getTrxType();

    void finalization();

    ProgramResult getResult();

    String getRuntimeError();

    void setEnableEventListener(boolean enableEventListener);
}
