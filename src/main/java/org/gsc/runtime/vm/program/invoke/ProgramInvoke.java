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


package org.gsc.runtime.vm.program.invoke;

import org.gsc.runtime.vm.DataWord;
import org.gsc.db.dbsource.Deposit;
import org.gsc.core.wrapper.BlockWrapper;

/**
 * @author Roman Mandeleil
 * @since 03.06.2014
 */
public interface ProgramInvoke {

    DataWord getContractAddress();

    DataWord getBalance();

    DataWord getOriginAddress();

    DataWord getCallerAddress();

    DataWord getCallValue();

    DataWord getTokenValue();

    DataWord getTokenId();

    DataWord getDataSize();

    DataWord getDataValue(DataWord indexData);

    byte[] getDataCopy(DataWord offsetData, DataWord lengthData);

    DataWord getPrevHash();

    DataWord getCoinbase();

    DataWord getTimestamp();

    DataWord getNumber();

    DataWord getDifficulty();

    boolean byTestingSuite();

    int getCallDeep();

    Deposit getDeposit();

    boolean isStaticCall();

    long getVmShouldEndInUs();

    long getVmStartInUs();

    long getCpuLimit();

    void setStaticCall();

    BlockWrapper getBlockByNum(int index);
}
