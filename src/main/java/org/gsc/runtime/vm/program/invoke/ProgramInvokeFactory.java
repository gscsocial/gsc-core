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
import org.gsc.runtime.vm.program.InternalTransaction;
import org.gsc.runtime.vm.program.InternalTransaction.ExecutorType;
import org.gsc.runtime.vm.program.Program;
import org.gsc.db.dbsource.Deposit;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public interface ProgramInvokeFactory {

    ProgramInvoke createProgramInvoke(InternalTransaction.TrxType trxType, ExecutorType executorType,
                                      Transaction tx, long tokenValue, long tokenId, Block block, Deposit deposit, long vmStartInUs,
                                      long vmShouldEndInUs,
                                      long cpuLimit) throws ContractValidateException;

    ProgramInvoke createProgramInvoke(Program program, DataWord toAddress, DataWord callerAddress,
                                      DataWord inValue, DataWord tokenValue, DataWord tokenId,
                                      long balanceInt, byte[] dataIn, Deposit deposit, boolean staticCall, boolean byTestingSuite,
                                      long vmStartInUs, long vmShouldEndInUs, long cpuLimit);


}
