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

import static org.gsc.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.gsc.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;

import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.Arrays;
import org.springframework.stereotype.Component;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.program.InternalTransaction;
import org.gsc.runtime.vm.program.InternalTransaction.ExecutorType;
import org.gsc.runtime.vm.program.Program;
import org.gsc.db.dbsource.Deposit;
import org.gsc.utils.ByteUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;

/**
 * @author Roman Mandeleil
 * @since 08.06.2014
 */
@Component("ProgramInvokeFactory")
@Slf4j(topic = "vm")
public class ProgramInvokeFactoryImpl implements ProgramInvokeFactory {

    // Invocation by the wire tx
    @Override
    public ProgramInvoke createProgramInvoke(InternalTransaction.TrxType trxType,
                                             ExecutorType executorType, Transaction tx, long tokenValue, long tokenId, Block block,
                                             Deposit deposit, long vmStartInUs,
                                             long vmShouldEndInUs, long cpuLimit) throws ContractValidateException {
        byte[] contractAddress;
        byte[] ownerAddress;
        long balance;
        byte[] data;
        byte[] lastHash = null;
        byte[] coinbase = null;
        long timestamp = 0L;
        long number = -1L;

        if (trxType == TRX_CONTRACT_CREATION_TYPE) {
            CreateSmartContract contract = ContractWrapper.getSmartContractFromTransaction(tx);
            contractAddress = Wallet.generateContractAddress(tx);
            ownerAddress = contract.getOwnerAddress().toByteArray();
            balance = deposit.getBalance(ownerAddress);
            data = ByteUtil.EMPTY_BYTE_ARRAY;
            long callValue = contract.getNewContract().getCallValue();

            switch (executorType) {
                case ET_NORMAL_TYPE:
                case ET_PRE_TYPE:
                    if (null != block) {
                        lastHash = block.getBlockHeader().getRawDataOrBuilder().getParentHash().toByteArray();
                        coinbase = block.getBlockHeader().getRawDataOrBuilder().getWitnessAddress()
                                .toByteArray();
                        timestamp = block.getBlockHeader().getRawDataOrBuilder().getTimestamp() / 1000;
                        number = block.getBlockHeader().getRawDataOrBuilder().getNumber();
                    }
                    break;
                default:
                    break;
            }

            return new ProgramInvokeImpl(contractAddress, ownerAddress, ownerAddress, balance, callValue,
                    tokenValue, tokenId, data, lastHash, coinbase, timestamp, number, deposit, vmStartInUs,
                    vmShouldEndInUs, cpuLimit);

        } else if (trxType == TRX_CONTRACT_CALL_TYPE) {
            Contract.TriggerSmartContract contract = ContractWrapper
                    .getTriggerContractFromTransaction(tx);
            /***         ADDRESS op       ***/
            // YP: Get address of currently executing account.
            byte[] address = contract.getContractAddress().toByteArray();

            /***         ORIGIN op       ***/
            // YP: This is the sender of original transaction; it is never a contract.
            byte[] origin = contract.getOwnerAddress().toByteArray();

            /***         CALLER op       ***/
            // YP: This is the address of the account that is directly responsible for this execution.
            byte[] caller = contract.getOwnerAddress().toByteArray();

            /***         BALANCE op       ***/
            balance = deposit.getBalance(caller);

            /***        CALLVALUE op      ***/
            long callValue = contract.getCallValue();

            /***     CALLDATALOAD  op   ***/
            /***     CALLDATACOPY  op   ***/
            /***     CALLDATASIZE  op   ***/
            data = contract.getData().toByteArray();

            switch (executorType) {
                case ET_CONSTANT_TYPE:
                    break;
                case ET_PRE_TYPE:
                case ET_NORMAL_TYPE:
                    if (null != block) {
                        /***    PREVHASH  op  ***/
                        lastHash = block.getBlockHeader().getRawDataOrBuilder().getParentHash().toByteArray();
                        /***   COINBASE  op ***/
                        coinbase = block.getBlockHeader().getRawDataOrBuilder().getWitnessAddress()
                                .toByteArray();
                        /*** TIMESTAMP  op  ***/
                        timestamp = block.getBlockHeader().getRawDataOrBuilder().getTimestamp() / 1000;
                        /*** NUMBER  op  ***/
                        number = block.getBlockHeader().getRawDataOrBuilder().getNumber();
                    }
                    break;
                default:
                    break;
            }

            return new ProgramInvokeImpl(address, origin, caller, balance, callValue, tokenValue, tokenId,
                    data,
                    lastHash, coinbase, timestamp, number, deposit, vmStartInUs, vmShouldEndInUs,
                    cpuLimit);
        }
        throw new ContractValidateException("Unknown contract type");
    }

    /**
     * This invocation created for contract call contract
     */
    @Override
    public ProgramInvoke createProgramInvoke(Program program, DataWord toAddress,
                                             DataWord callerAddress,
                                             DataWord inValue, DataWord tokenValue, DataWord tokenId, long balanceInt, byte[] dataIn,
                                             Deposit deposit, boolean isStaticCall, boolean byTestingSuite, long vmStartInUs,
                                             long vmShouldEndInUs, long cpuLimit) {

        DataWord address = toAddress;
        DataWord origin = program.getOriginAddress();
        DataWord caller = callerAddress;
        DataWord balance = new DataWord(balanceInt);
        DataWord callValue = inValue;

        byte[] data = Arrays.clone(dataIn);
        DataWord lastHash = program.getPrevHash();
        DataWord coinbase = program.getCoinbase();
        DataWord timestamp = program.getTimestamp();
        DataWord number = program.getNumber();
        DataWord difficulty = program.getDifficulty();

        return new ProgramInvokeImpl(address, origin, caller, balance, callValue, tokenValue, tokenId,
                data, lastHash, coinbase, timestamp, number, difficulty,
                deposit, program.getCallDeep() + 1, isStaticCall, byTestingSuite, vmStartInUs,
                vmShouldEndInUs, cpuLimit);
    }

}
