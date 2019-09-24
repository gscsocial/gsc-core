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

package org.gsc.runtime.vm;

import static org.gsc.crypto.Hash.sha3;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.gsc.runtime.config.VMConfig;
import org.gsc.runtime.vm.program.Program;
import org.gsc.runtime.vm.program.Stack;
import org.gsc.crypto.Hash;
import org.gsc.runtime.utils.MUtil;
import org.gsc.utils.ByteUtil;

@Slf4j(topic = "VM")
public class VM {

    private static final BigInteger _32_ = BigInteger.valueOf(32);
    private static final String CPU_LOG_FORMATE = "{} Op:[{}]  Cpu:[{}] Deep:[{}] Hint:[{}]";

    // 3MB
    private static final BigInteger MEM_LIMIT = BigInteger.valueOf(3L * 1024 * 1024);
    public static final String ADDRESS_LOG = "address: ";

    private final VMConfig config;

    public VM() {
        config = VMConfig.getInstance();
    }

    public VM(VMConfig config) {
        this.config = config;
    }

    private void checkMemorySize(OpCode op, BigInteger newMemSize) {
        if (newMemSize.compareTo(MEM_LIMIT) > 0) {
            throw Program.Exception.memoryOverflow(op);
        }
    }

    private long calcMemCpu(CpuCost cpuCosts, long oldMemSize, BigInteger newMemSize,
                            long copySize, OpCode op) {
        long cpuCost = 0;

        checkMemorySize(op, newMemSize);

        // memory drop consume calc
        long memoryUsage = (newMemSize.longValueExact() + 31) / 32 * 32;
        if (memoryUsage > oldMemSize) {
            long memWords = (memoryUsage / 32);
            long memWordsOld = (oldMemSize / 32);
            //TODO #POC9 c_quadCoeffDiv = 512, this should be a constant, not magic number
            long memCpu = (cpuCosts.getMEMORY() * memWords + memWords * memWords / 512)
                    - (cpuCosts.getMEMORY() * memWordsOld + memWordsOld * memWordsOld / 512);
            cpuCost += memCpu;
        }

        if (copySize > 0) {
            long copyCpu = cpuCosts.getCOPY_CPU() * ((copySize + 31) / 32);
            cpuCost += copyCpu;
        }
        return cpuCost;
    }

    public void step(Program program) {
        if (config.vmTrace()) {
            program.saveOpTrace();
        }

        try {
            OpCode op = OpCode.code(program.getCurrentOp());
            if (op == null) {
                throw Program.Exception.invalidOpCode(program.getCurrentOp());
            }

            // hard fork for 3.2
            if (!VMConfig.allowGvmTransferGrc10()) {
                if (op == OpCode.CALLTOKEN || op == OpCode.TOKENBALANCE || op == OpCode.CALLTOKENVALUE || op == OpCode.CALLTOKENID) {
                    throw Program.Exception.invalidOpCode(program.getCurrentOp());
                }
            }

            if (!VMConfig.allowGvmConstantinople()) {
                if (op == OpCode.SHL || op == OpCode.SHR || op == OpCode.SAR || op == OpCode.CREATE2 || op == OpCode.EXTCODEHASH) {
                    throw Program.Exception.invalidOpCode(program.getCurrentOp());
                }
            }
            program.setLastOp(op.val());
            program.verifyStackSize(op.require());
            program.verifyStackOverflow(op.require(), op.ret()); //Check not exceeding stack limits

            long oldMemSize = program.getMemSize();
            Stack stack = program.getStack();

            String hint = "";
            long cpuCost = op.getTier().asInt();
            CpuCost cpuCosts = CpuCost.getInstance();
            DataWord adjustedCallCpu = null;

            // Calculate fees and spend cpu
            switch (op) {
                case STOP:
                    cpuCost = cpuCosts.getSTOP();
                    break;
                case SUICIDE:
                    cpuCost = cpuCosts.getSUICIDE();
                    DataWord suicideAddressWord = stack.get(stack.size() - 1);
                    if (isDeadAccount(program, suicideAddressWord)
                            && !program.getBalance(program.getContractAddress()).isZero()) {
                        cpuCost += cpuCosts.getNEW_ACCT_SUICIDE();
                    }
                    break;
                case SSTORE:
                    // todo: check the reset to 0, refund or not
                    DataWord newValue = stack.get(stack.size() - 2);
                    DataWord oldValue = program.storageLoad(stack.peek());
                    if (oldValue == null && !newValue.isZero()) {
                        // set a new not-zero value
                        cpuCost = cpuCosts.getSET_SSTORE();
                    } else if (oldValue != null && newValue.isZero()) {
                        // set zero to an old value
                        program.futureRefundCpu(cpuCosts.getREFUND_SSTORE());
                        cpuCost = cpuCosts.getCLEAR_SSTORE();
                    } else {
                        // include:
                        // [1] oldValue == null && newValue == 0
                        // [2] oldValue != null && newValue != 0
                        cpuCost = cpuCosts.getRESET_SSTORE();
                    }
                    break;
                case SLOAD:
                    cpuCost = cpuCosts.getSLOAD();
                    break;
                case TOKENBALANCE:
                case BALANCE:
                    cpuCost = cpuCosts.getBALANCE();
                    break;

                // These all operate on memory and therefore potentially expand it:
                case MSTORE:
                    cpuCost = calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.peek(), new DataWord(32)),
                            0, op);
                    break;
                case MSTORE8:
                    cpuCost = calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.peek(), new DataWord(1)),
                            0, op);
                    break;
                case MLOAD:
                    cpuCost = calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.peek(), new DataWord(32)),
                            0, op);
                    break;
                case RETURN:
                case REVERT:
                    cpuCost = cpuCosts.getSTOP() + calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, op);
                    break;
                case SHA3:
                    cpuCost = cpuCosts.getSHA3() + calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, op);
                    DataWord size = stack.get(stack.size() - 2);
                    long chunkUsed = (size.longValueSafe() + 31) / 32;
                    cpuCost += chunkUsed * cpuCosts.getSHA3_WORD();
                    break;
                case CALLDATACOPY:
                case RETURNDATACOPY:
                    cpuCost = calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.peek(), stack.get(stack.size() - 3)),
                            stack.get(stack.size() - 3).longValueSafe(), op);
                    break;
                case CODECOPY:
                    cpuCost = calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.peek(), stack.get(stack.size() - 3)),
                            stack.get(stack.size() - 3).longValueSafe(), op);
                    break;
                case EXTCODESIZE:
                    cpuCost = cpuCosts.getEXT_CODE_SIZE();
                    break;
                case EXTCODECOPY:
                    cpuCost = cpuCosts.getEXT_CODE_COPY() + calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 4)),
                            stack.get(stack.size() - 4).longValueSafe(), op);
                    break;
                case EXTCODEHASH:
                    cpuCost = cpuCosts.getEXT_CODE_HASH();
                    break;
                case CALL:
                case CALLCODE:
                case DELEGATECALL:
                case STATICCALL:
                case CALLTOKEN:
                    // here, contract call an other contract, or a library, and so on
                    cpuCost = cpuCosts.getCALL();
                    DataWord callCpuWord = stack.get(stack.size() - 1);
                    DataWord callAddressWord = stack.get(stack.size() - 2);
                    DataWord value = op.callHasValue() ? stack.get(stack.size() - 3) : DataWord.ZERO;

                    //check to see if account does not exist and is not a precompiled contract
                    if (op == OpCode.CALL || op == OpCode.CALLTOKEN) {
                        if (isDeadAccount(program, callAddressWord) && !value.isZero()) {
                            cpuCost += cpuCosts.getNEW_ACCT_CALL();
                        }
                    }

                    // TODO #POC9 Make sure this is converted to BigInteger (256num support)
                    if (!value.isZero()) {
                        cpuCost += cpuCosts.getVT_CALL();
                    }

                    int opOff = op.callHasValue() ? 4 : 3;
                    if (op == OpCode.CALLTOKEN) {
                        opOff++;
                    }
                    BigInteger in = memNeeded(stack.get(stack.size() - opOff),
                            stack.get(stack.size() - opOff - 1)); // in offset+size
                    BigInteger out = memNeeded(stack.get(stack.size() - opOff - 2),
                            stack.get(stack.size() - opOff - 3)); // out offset+size
                    cpuCost += calcMemCpu(cpuCosts, oldMemSize, in.max(out), 0, op);
                    checkMemorySize(op, in.max(out));

                    if (cpuCost > program.getCpuLimitLeft().longValueSafe()) {
                        throw new Program.OutOfCpuException(
                                "Not enough cpu for '%s' operation executing: opCpu[%d], programCpu[%d]",
                                op.name(),
                                cpuCost, program.getCpuLimitLeft().longValueSafe());
                    }
                    DataWord getCpuLimitLeft = program.getCpuLimitLeft().clone();
                    getCpuLimitLeft.sub(new DataWord(cpuCost));

                    adjustedCallCpu = program.getCallCpu(op, callCpuWord, getCpuLimitLeft);
                    cpuCost += adjustedCallCpu.longValueSafe();
                    break;
                case CREATE:
                    cpuCost = cpuCosts.getCREATE() + calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)), 0, op);
                    break;
                case CREATE2:
                    DataWord codeSize = stack.get(stack.size() - 3);
                    cpuCost = cpuCosts.getCREATE();
                    cpuCost += calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)), 0, op);
                    cpuCost += DataWord.sizeInWords(codeSize.intValueSafe()) * cpuCosts.getSHA3_WORD();

                    break;
                case LOG0:
                case LOG1:
                case LOG2:
                case LOG3:
                case LOG4:
                    int nTopics = op.val() - OpCode.LOG0.val();
                    BigInteger dataSize = stack.get(stack.size() - 2).value();
                    BigInteger dataCost = dataSize
                            .multiply(BigInteger.valueOf(cpuCosts.getLOG_DATA_CPU()));
                    if (program.getCpuLimitLeft().value().compareTo(dataCost) < 0) {
                        throw new Program.OutOfCpuException(
                                "Not enough cpu for '%s' operation executing: opCpu[%d], programCpu[%d]",
                                op.name(),
                                dataCost.longValueExact(), program.getCpuLimitLeft().longValueSafe());
                    }
                    cpuCost = cpuCosts.getLOG_CPU()
                            + cpuCosts.getLOG_TOPIC_CPU() * nTopics
                            + cpuCosts.getLOG_DATA_CPU() * stack.get(stack.size() - 2).longValue()
                            + calcMemCpu(cpuCosts, oldMemSize,
                            memNeeded(stack.peek(), stack.get(stack.size() - 2)), 0, op);

                    checkMemorySize(op, memNeeded(stack.peek(), stack.get(stack.size() - 2)));
                    break;
                case EXP:

                    DataWord exp = stack.get(stack.size() - 2);
                    int bytesOccupied = exp.bytesOccupied();
                    cpuCost =
                            (long) cpuCosts.getEXP_CPU() + cpuCosts.getEXP_BYTE_CPU() * bytesOccupied;
                    break;
                default:
                    break;
            }

            program.spendCpu(cpuCost, op.name());
            program.checkCPUTimeLimit(op.name());

            // Execute operation
            switch (op) {
                /**
                 * Stop and Arithmetic Operations
                 */
                case STOP: {
                    program.setHReturn(ByteUtil.EMPTY_BYTE_ARRAY);
                    program.stop();
                }
                break;
                case ADD: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " + " + word2.value();
                    }

                    word1.add(word2);
                    program.stackPush(word1);
                    program.step();

                }
                break;
                case MUL: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " * " + word2.value();
                    }

                    word1.mul(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case SUB: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " - " + word2.value();
                    }

                    word1.sub(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case DIV: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " / " + word2.value();
                    }

                    word1.div(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case SDIV: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.sValue() + " / " + word2.sValue();
                    }

                    word1.sDiv(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case MOD: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " % " + word2.value();
                    }

                    word1.mod(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case SMOD: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.sValue() + " #% " + word2.sValue();
                    }

                    word1.sMod(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case EXP: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " ** " + word2.value();
                    }

                    word1.exp(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case SIGNEXTEND: {
                    DataWord word1 = program.stackPop();
                    BigInteger k = word1.value();

                    if (k.compareTo(_32_) < 0) {
                        DataWord word2 = program.stackPop();
                        if (logger.isDebugEnabled()) {
                            hint = word1 + "  " + word2.value();
                        }
                        word2.signExtend(k.byteValue());
                        program.stackPush(word2);
                    }
                    program.step();
                }
                break;
                case NOT: {
                    DataWord word1 = program.stackPop();
                    word1.bnot();

                    if (logger.isDebugEnabled()) {
                        hint = "" + word1.value();
                    }

                    program.stackPush(word1);
                    program.step();
                }
                break;
                case LT: {
                    // TODO: can be improved by not using BigInteger
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " < " + word2.value();
                    }

                    if (word1.value().compareTo(word2.value()) < 0) {
                        word1.and(DataWord.ZERO);
                        word1.getData()[31] = 1;
                    } else {
                        word1.and(DataWord.ZERO);
                    }
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case SLT: {
                    // TODO: can be improved by not using BigInteger
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.sValue() + " < " + word2.sValue();
                    }

                    if (word1.sValue().compareTo(word2.sValue()) < 0) {
                        word1.and(DataWord.ZERO);
                        word1.getData()[31] = 1;
                    } else {
                        word1.and(DataWord.ZERO);
                    }
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case SGT: {
                    // TODO: can be improved by not using BigInteger
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.sValue() + " > " + word2.sValue();
                    }

                    if (word1.sValue().compareTo(word2.sValue()) > 0) {
                        word1.and(DataWord.ZERO);
                        word1.getData()[31] = 1;
                    } else {
                        word1.and(DataWord.ZERO);
                    }
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case GT: {
                    // TODO: can be improved by not using BigInteger
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " > " + word2.value();
                    }

                    if (word1.value().compareTo(word2.value()) > 0) {
                        word1.and(DataWord.ZERO);
                        word1.getData()[31] = 1;
                    } else {
                        word1.and(DataWord.ZERO);
                    }
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case EQ: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " == " + word2.value();
                    }

                    if (word1.xor(word2).isZero()) {
                        word1.and(DataWord.ZERO);
                        word1.getData()[31] = 1;
                    } else {
                        word1.and(DataWord.ZERO);
                    }
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case ISZERO: {
                    DataWord word1 = program.stackPop();
                    if (word1.isZero()) {
                        word1.getData()[31] = 1;
                    } else {
                        word1.and(DataWord.ZERO);
                    }

                    if (logger.isDebugEnabled()) {
                        hint = "" + word1.value();
                    }

                    program.stackPush(word1);
                    program.step();
                }
                break;

                /**
                 * Bitwise Logic Operations
                 */
                case AND: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " && " + word2.value();
                    }

                    word1.and(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case OR: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " || " + word2.value();
                    }

                    word1.or(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case XOR: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = word1.value() + " ^ " + word2.value();
                    }

                    word1.xor(word2);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case BYTE: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();
                    final DataWord result;
                    if (word1.value().compareTo(_32_) < 0) {
                        byte tmp = word2.getData()[word1.intValue()];
                        word2.and(DataWord.ZERO);
                        word2.getData()[31] = tmp;
                        result = word2;
                    } else {
                        result = new DataWord();
                    }

                    if (logger.isDebugEnabled()) {
                        hint = "" + result.value();
                    }

                    program.stackPush(result);
                    program.step();
                }
                break;
                case SHL: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();
                    final DataWord result = word2.shiftLeft(word1);

                    if (logger.isInfoEnabled()) {
                        hint = "" + result.value();
                    }

                    program.stackPush(result);
                    program.step();
                }
                break;
                case SHR: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();
                    final DataWord result = word2.shiftRight(word1);

                    if (logger.isInfoEnabled()) {
                        hint = "" + result.value();
                    }

                    program.stackPush(result);
                    program.step();
                }
                break;
                case SAR: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();
                    final DataWord result = word2.shiftRightSigned(word1);

                    if (logger.isInfoEnabled()) {
                        hint = "" + result.value();
                    }

                    program.stackPush(result);
                    program.step();
                }
                break;
                case ADDMOD: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();
                    DataWord word3 = program.stackPop();
                    word1.addmod(word2, word3);
                    program.stackPush(word1);
                    program.step();
                }
                break;
                case MULMOD: {
                    DataWord word1 = program.stackPop();
                    DataWord word2 = program.stackPop();
                    DataWord word3 = program.stackPop();
                    word1.mulmod(word2, word3);
                    program.stackPush(word1);
                    program.step();
                }
                break;

                /**
                 * SHA3
                 */
                case SHA3: {
                    DataWord memOffsetData = program.stackPop();
                    DataWord lengthData = program.stackPop();
                    byte[] buffer = program
                            .memoryChunk(memOffsetData.intValueSafe(), lengthData.intValueSafe());

                    byte[] encoded = Hash.sha3(buffer);
                    DataWord word = new DataWord(encoded);

                    if (logger.isDebugEnabled()) {
                        hint = word.toString();
                    }

                    program.stackPush(word);
                    program.step();
                }
                break;

                /**
                 * Environmental Information
                 */
                case ADDRESS: {
                    DataWord address = program.getContractAddress();
                    if (VMConfig.allowMultiSign()) { // allowMultiSigns proposal
                        address = new DataWord(address.getLast20Bytes());
                    }

                    if (logger.isDebugEnabled()) {
                        hint = ADDRESS_LOG + Hex.toHexString(address.getLast20Bytes());
                    }

                    program.stackPush(address);
                    program.step();
                }
                break;
                case BALANCE: {
                    DataWord address = program.stackPop();
                    DataWord balance = program.getBalance(address);

                    if (logger.isDebugEnabled()) {
                        hint = ADDRESS_LOG
                                + Hex.toHexString(address.getLast20Bytes())
                                + " balance: " + balance.toString();
                    }

                    program.stackPush(balance);
                    program.step();
                }
                break;
                case ORIGIN: {
                    DataWord originAddress = program.getOriginAddress();

                    if (VMConfig.allowMultiSign()) { //allowMultiSign proposal
                        originAddress = new DataWord(originAddress.getLast20Bytes());
                    }

                    if (logger.isDebugEnabled()) {
                        hint = ADDRESS_LOG + Hex.toHexString(originAddress.getLast20Bytes());
                    }

                    program.stackPush(originAddress);
                    program.step();
                }
                break;
                case CALLER: {
                    DataWord callerAddress = program.getCallerAddress();
                    /**
                     since we use 21 bytes address instead of 20 as etherum, we need to make sure
                     the address length in vm is matching with 20
                     */
                    callerAddress = new DataWord(callerAddress.getLast20Bytes());
                    if (logger.isDebugEnabled()) {
                        hint = ADDRESS_LOG + Hex.toHexString(callerAddress.getLast20Bytes());
                    }

                    program.stackPush(callerAddress);
                    program.step();
                }
                break;
                case CALLVALUE: {
                    DataWord callValue = program.getCallValue();

                    if (logger.isDebugEnabled()) {
                        hint = "value: " + callValue;
                    }

                    program.stackPush(callValue);
                    program.step();
                }
                break;
                case CALLTOKENVALUE:
                    DataWord tokenValue = program.getTokenValue();

                    if (logger.isDebugEnabled()) {
                        hint = "tokenValue: " + tokenValue;
                    }

                    program.stackPush(tokenValue);
                    program.step();
                    break;
                case CALLTOKENID:
                    DataWord _tokenId = program.getTokenId();

                    if (logger.isDebugEnabled()) {
                        hint = "tokenId: " + _tokenId;
                    }

                    program.stackPush(_tokenId);
                    program.step();
                    break;
                case CALLDATALOAD: {
                    DataWord dataOffs = program.stackPop();
                    DataWord value = program.getDataValue(dataOffs);

                    if (logger.isDebugEnabled()) {
                        hint = "data: " + value;
                    }

                    program.stackPush(value);
                    program.step();
                }
                break;
                case CALLDATASIZE: {
                    DataWord dataSize = program.getDataSize();

                    if (logger.isDebugEnabled()) {
                        hint = "size: " + dataSize.value();
                    }

                    program.stackPush(dataSize);
                    program.step();
                }
                break;
                case CALLDATACOPY: {
                    DataWord memOffsetData = program.stackPop();
                    DataWord dataOffsetData = program.stackPop();
                    DataWord lengthData = program.stackPop();

                    byte[] msgData = program.getDataCopy(dataOffsetData, lengthData);

                    if (logger.isDebugEnabled()) {
                        hint = "data: " + Hex.toHexString(msgData);
                    }

                    program.memorySave(memOffsetData.intValueSafe(), msgData);
                    program.step();
                }
                break;
                case RETURNDATASIZE: {
                    DataWord dataSize = program.getReturnDataBufferSize();

                    if (logger.isDebugEnabled()) {
                        hint = "size: " + dataSize.value();
                    }

                    program.stackPush(dataSize);
                    program.step();
                }
                break;
                case RETURNDATACOPY: {
                    DataWord memOffsetData = program.stackPop();
                    DataWord dataOffsetData = program.stackPop();
                    DataWord lengthData = program.stackPop();

                    byte[] msgData = program.getReturnDataBufferData(dataOffsetData, lengthData);

                    if (msgData == null) {
                        throw new Program.ReturnDataCopyIllegalBoundsException(dataOffsetData, lengthData,
                                program.getReturnDataBufferSize().longValueSafe());
                    }

                    if (logger.isDebugEnabled()) {
                        hint = "data: " + Hex.toHexString(msgData);
                    }

                    program.memorySave(memOffsetData.intValueSafe(), msgData);
                    program.step();
                }
                break;
                case CODESIZE:
                case EXTCODESIZE: {

                    int length;
                    if (op == OpCode.CODESIZE) {
                        length = program.getCode().length;
                    } else {
                        DataWord address = program.stackPop();
                        length = program.getCodeAt(address).length;
                    }
                    DataWord codeLength = new DataWord(length);

                    if (logger.isDebugEnabled()) {
                        hint = "size: " + length;
                    }

                    program.stackPush(codeLength);
                    program.step();
                    break;
                }
                case CODECOPY:
                case EXTCODECOPY: {

                    byte[] fullCode = ByteUtil.EMPTY_BYTE_ARRAY;
                    if (op == OpCode.CODECOPY) {
                        fullCode = program.getCode();
                    }

                    if (op == OpCode.EXTCODECOPY) {
                        DataWord address = program.stackPop();
                        fullCode = program.getCodeAt(address);
                    }

                    int memOffset = program.stackPop().intValueSafe();
                    int codeOffset = program.stackPop().intValueSafe();
                    int lengthData = program.stackPop().intValueSafe();

                    int sizeToBeCopied =
                            (long) codeOffset + lengthData > fullCode.length
                                    ? (fullCode.length < codeOffset ? 0 : fullCode.length - codeOffset)
                                    : lengthData;

                    byte[] codeCopy = new byte[lengthData];

                    if (codeOffset < fullCode.length) {
                        System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied);
                    }

                    if (logger.isDebugEnabled()) {
                        hint = "code: " + Hex.toHexString(codeCopy);
                    }

                    program.memorySave(memOffset, codeCopy);
                    program.step();
                    break;
                }
                case EXTCODEHASH: {
                    DataWord address = program.stackPop();
                    byte[] codeHash = program.getCodeHashAt(address);
                    program.stackPush(codeHash);
                    program.step();
                }
                break;
                case GASPRICE: {
                    DataWord cpuPrice = new DataWord(0);

                    if (logger.isDebugEnabled()) {
                        hint = "price: " + cpuPrice.toString();
                    }

                    program.stackPush(cpuPrice);
                    program.step();
                }
                break;

                /**
                 * Block Information
                 */
                case BLOCKHASH: {

                    int blockIndex = program.stackPop().intValueSafe();

                    DataWord blockHash = program.getBlockHash(blockIndex);

                    if (logger.isDebugEnabled()) {
                        hint = "blockHash: " + blockHash;
                    }

                    program.stackPush(blockHash);
                    program.step();
                }
                break;
                case COINBASE: {
                    DataWord coinbase = program.getCoinbase();

                    if (logger.isDebugEnabled()) {
                        hint = "coinbase: " + Hex.toHexString(coinbase.getLast20Bytes());
                    }

                    program.stackPush(coinbase);
                    program.step();
                }
                break;
                case TIMESTAMP: {
                    DataWord timestamp = program.getTimestamp();

                    if (logger.isDebugEnabled()) {
                        hint = "timestamp: " + timestamp.value();
                    }

                    program.stackPush(timestamp);
                    program.step();
                }
                break;
                case NUMBER: {
                    DataWord number = program.getNumber();

                    if (logger.isDebugEnabled()) {
                        hint = "number: " + number.value();
                    }

                    program.stackPush(number);
                    program.step();
                }
                break;
                case DIFFICULTY: {
                    DataWord difficulty = program.getDifficulty();

                    if (logger.isDebugEnabled()) {
                        hint = "difficulty: " + difficulty;
                    }

                    program.stackPush(difficulty);
                    program.step();
                }
                break;
                case GASLIMIT: {
                    // todo: this cpulimit is the block's cpu limit
                    DataWord cpuLimit = new DataWord(0);

                    if (logger.isDebugEnabled()) {
                        hint = "cpulimit: " + cpuLimit;
                    }

                    program.stackPush(cpuLimit);
                    program.step();
                }
                break;
                case POP: {
                    program.stackPop();
                    program.step();
                }
                break;
                case DUP1:
                case DUP2:
                case DUP3:
                case DUP4:
                case DUP5:
                case DUP6:
                case DUP7:
                case DUP8:
                case DUP9:
                case DUP10:
                case DUP11:
                case DUP12:
                case DUP13:
                case DUP14:
                case DUP15:
                case DUP16: {

                    int n = op.val() - OpCode.DUP1.val() + 1;
                    DataWord word_1 = stack.get(stack.size() - n);
                    program.stackPush(word_1.clone());
                    program.step();

                    break;
                }
                case SWAP1:
                case SWAP2:
                case SWAP3:
                case SWAP4:
                case SWAP5:
                case SWAP6:
                case SWAP7:
                case SWAP8:
                case SWAP9:
                case SWAP10:
                case SWAP11:
                case SWAP12:
                case SWAP13:
                case SWAP14:
                case SWAP15:
                case SWAP16: {

                    int n = op.val() - OpCode.SWAP1.val() + 2;
                    stack.swap(stack.size() - 1, stack.size() - n);
                    program.step();
                    break;
                }
                case LOG0:
                case LOG1:
                case LOG2:
                case LOG3:
                case LOG4: {

                    if (program.isStaticCall()) {
                        throw new Program.StaticCallModificationException();
                    }
                    DataWord address = program.getContractAddress();

                    DataWord memStart = stack.pop();
                    DataWord memOffset = stack.pop();

                    int nTopics = op.val() - OpCode.LOG0.val();

                    List<DataWord> topics = new ArrayList<>();
                    for (int i = 0; i < nTopics; ++i) {
                        DataWord topic = stack.pop();
                        topics.add(topic);
                    }

                    byte[] data = program.memoryChunk(memStart.intValueSafe(), memOffset.intValueSafe());

                    LogInfo logInfo =
                            new LogInfo(address.getLast20Bytes(), topics, data);

                    if (logger.isDebugEnabled()) {
                        hint = logInfo.toString();
                    }

                    program.getResult().addLogInfo(logInfo);
                    program.step();
                    break;
                }
                case MLOAD: {
                    DataWord addr = program.stackPop();
                    DataWord data = program.memoryLoad(addr);

                    if (logger.isDebugEnabled()) {
                        hint = "data: " + data;
                    }

                    program.stackPush(data);
                    program.step();
                }
                break;
                case MSTORE: {
                    DataWord addr = program.stackPop();
                    DataWord value = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = "addr: " + addr + " value: " + value;
                    }

                    program.memorySave(addr, value);
                    program.step();
                }
                break;
                case MSTORE8: {
                    DataWord addr = program.stackPop();
                    DataWord value = program.stackPop();
                    byte[] byteVal = {value.getData()[31]};
                    program.memorySave(addr.intValueSafe(), byteVal);
                    program.step();
                }
                break;
                case SLOAD: {
                    DataWord key = program.stackPop();
                    DataWord val = program.storageLoad(key);

                    if (logger.isDebugEnabled()) {
                        hint = "key: " + key + " value: " + val;
                    }

                    if (val == null) {
                        val = key.and(DataWord.ZERO);
                    }

                    program.stackPush(val);
                    program.step();
                }
                break;
                case SSTORE: {
                    if (program.isStaticCall()) {
                        throw new Program.StaticCallModificationException();
                    }

                    DataWord addr = program.stackPop();
                    DataWord value = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint =
                                "[" + program.getContractAddress().toPrefixString() + "] key: " + addr + " value: "
                                        + value;
                    }

                    program.storageSave(addr, value);
                    program.step();
                }
                break;
                case JUMP: {
                    DataWord pos = program.stackPop();
                    int nextPC = program.verifyJumpDest(pos);

                    if (logger.isDebugEnabled()) {
                        hint = "~> " + nextPC;
                    }

                    program.setPC(nextPC);

                }
                break;
                case JUMPI: {
                    DataWord pos = program.stackPop();
                    DataWord cond = program.stackPop();

                    if (!cond.isZero()) {
                        int nextPC = program.verifyJumpDest(pos);

                        if (logger.isDebugEnabled()) {
                            hint = "~> " + nextPC;
                        }

                        program.setPC(nextPC);
                    } else {
                        program.step();
                    }

                }
                break;
                case PC: {
                    int pc = program.getPC();
                    DataWord pcWord = new DataWord(pc);

                    if (logger.isDebugEnabled()) {
                        hint = pcWord.toString();
                    }

                    program.stackPush(pcWord);
                    program.step();
                }
                break;
                case MSIZE: {
                    int memSize = program.getMemSize();
                    DataWord wordMemSize = new DataWord(memSize);

                    if (logger.isDebugEnabled()) {
                        hint = "" + memSize;
                    }

                    program.stackPush(wordMemSize);
                    program.step();
                }
                break;
                case GAS: {
                    DataWord cpu = program.getCpuLimitLeft();
                    if (logger.isDebugEnabled()) {
                        hint = "" + cpu;
                    }

                    program.stackPush(cpu);
                    program.step();
                }
                break;

                case PUSH1:
                case PUSH2:
                case PUSH3:
                case PUSH4:
                case PUSH5:
                case PUSH6:
                case PUSH7:
                case PUSH8:
                case PUSH9:
                case PUSH10:
                case PUSH11:
                case PUSH12:
                case PUSH13:
                case PUSH14:
                case PUSH15:
                case PUSH16:
                case PUSH17:
                case PUSH18:
                case PUSH19:
                case PUSH20:
                case PUSH21:
                case PUSH22:
                case PUSH23:
                case PUSH24:
                case PUSH25:
                case PUSH26:
                case PUSH27:
                case PUSH28:
                case PUSH29:
                case PUSH30:
                case PUSH31:
                case PUSH32: {
                    program.step();
                    int nPush = op.val() - OpCode.PUSH1.val() + 1;

                    byte[] data = program.sweep(nPush);

                    if (logger.isDebugEnabled()) {
                        hint = "" + Hex.toHexString(data);
                    }

                    program.stackPush(data);
                    break;
                }
                case JUMPDEST: {
                    program.step();
                }
                break;
                case CREATE: {
                    if (program.isStaticCall()) {
                        throw new Program.StaticCallModificationException();
                    }
                    DataWord value = program.stackPop();
                    DataWord inOffset = program.stackPop();
                    DataWord inSize = program.stackPop();
                    program.createContract(value, inOffset, inSize);

                    program.step();
                }
                break;
                case CREATE2: {
                    if (program.isStaticCall()) {
                        throw new Program.StaticCallModificationException();
                    }
                    DataWord value = program.stackPop();
                    DataWord inOffset = program.stackPop();
                    DataWord inSize = program.stackPop();
                    DataWord salt = program.stackPop();
                    program.createContract2(value, inOffset, inSize, salt);
                    program.step();
                }
                break;
                case TOKENBALANCE: {
                    DataWord tokenId = program.stackPop();
                    DataWord address = program.stackPop();
                    DataWord tokenBalance = program.getTokenBalance(address, tokenId);
                    program.stackPush(tokenBalance);

                    program.step();
                }
                break;
                case CALL:
                case CALLCODE:
                case CALLTOKEN:
                case DELEGATECALL:
                case STATICCALL: {
                    program.stackPop(); // use adjustedCallCpu instead of requested
                    DataWord codeAddress = program.stackPop();

                    DataWord value;
                    if (op.callHasValue()) {
                        value = program.stackPop();
                    } else {
                        value = DataWord.ZERO;
                    }

                    if (program.isStaticCall() && (op == OpCode.CALL || op == OpCode.CALLTOKEN) && !value.isZero()) {
                        throw new Program.StaticCallModificationException();
                    }

                    if (!value.isZero()) {
                        adjustedCallCpu.add(new DataWord(cpuCosts.getSTIPEND_CALL()));
                    }

                    DataWord tokenId = new DataWord(0);
                    boolean isTokenTransferMsg = false;
                    if (op == OpCode.CALLTOKEN) {
                        tokenId = program.stackPop();
                        if (VMConfig.allowMultiSign()) { // allowMultiSign proposal
                            isTokenTransferMsg = true;
                        }
                    }

                    DataWord inDataOffs = program.stackPop();
                    DataWord inDataSize = program.stackPop();

                    DataWord outDataOffs = program.stackPop();
                    DataWord outDataSize = program.stackPop();

                    if (logger.isDebugEnabled()) {
                        hint = "addr: " + Hex.toHexString(codeAddress.getLast20Bytes())
                                + " cpu: " + adjustedCallCpu.shortHex()
                                + " inOff: " + inDataOffs.shortHex()
                                + " inSize: " + inDataSize.shortHex();
                        logger.debug(CPU_LOG_FORMATE, String.format("%5s", "[" + program.getPC() + "]"),
                                String.format("%-12s", op.name()),
                                program.getCpuLimitLeft().value(),
                                program.getCallDeep(), hint);
                    }

                    program.memoryExpand(outDataOffs, outDataSize);

                    MessageCall msg = new MessageCall(
                            op, adjustedCallCpu, codeAddress, value, inDataOffs, inDataSize,
                            outDataOffs, outDataSize, tokenId, isTokenTransferMsg);

                    PrecompiledContracts.PrecompiledContract contract =
                            PrecompiledContracts.getContractForAddress(codeAddress);

                    if (!op.callIsStateless()) {
                        program.getResult().addTouchAccount(codeAddress.getLast20Bytes());
                    }

                    if (contract != null) {
                        program.callToPrecompiledAddress(msg, contract);
                    } else {
                        program.callToAddress(msg);
                    }

                    program.step();
                    break;
                }
                case RETURN:
                case REVERT: {
                    DataWord offset = program.stackPop();
                    DataWord size = program.stackPop();

                    byte[] hReturn = program.memoryChunk(offset.intValueSafe(), size.intValueSafe());
                    program.setHReturn(hReturn);

                    if (logger.isDebugEnabled()) {
                        hint = "data: " + Hex.toHexString(hReturn)
                                + " offset: " + offset.value()
                                + " size: " + size.value();
                    }

                    program.step();
                    program.stop();

                    if (op == OpCode.REVERT) {
                        program.getResult().setRevert();
                    }
                    break;
                }
                case SUICIDE: {
                    if (program.isStaticCall()) {
                        throw new Program.StaticCallModificationException();
                    }

                    DataWord address = program.stackPop();
                    program.suicide(address);
                    program.getResult().addTouchAccount(address.getLast20Bytes());

                    if (logger.isDebugEnabled()) {
                        hint = ADDRESS_LOG + Hex.toHexString(program.getContractAddress().getLast20Bytes());
                    }

                    program.stop();
                }
                break;
                default:
                    break;
            }

            program.setPreviouslyExecutedOp(op.val());
        } catch (RuntimeException e) {
            logger.info("VM halted: [{}]", e.getMessage());
            if (!(e instanceof Program.TransferException)) {
                program.spendAllCpu();
            }
            program.resetFutureRefund();
            program.stop();
            throw e;
        } finally {
            program.fullTrace();
        }
    }

    public void play(Program program) {
        try {
            if (program.byTestingSuite()) {
                return;
            }

            while (!program.isStopped()) {
                this.step(program);
            }

        } catch (Program.JVMStackOverFlowException | Program.OutOfTimeException e) {
            throw e;
        } catch (RuntimeException e) {
            if (StringUtils.isEmpty(e.getMessage())) {
                logger.warn("Unknown Exception occurred, tx id: {}",
                        Hex.toHexString(program.getRootTransactionId()), e);
                program.setRuntimeFailure(new RuntimeException("Unknown Exception"));
            } else {
                program.setRuntimeFailure(e);
            }
        } catch (StackOverflowError soe) {
            logger
                    .info("\n !!! StackOverflowError: update your java run command with -Xss !!!\n", soe);
            throw new Program.JVMStackOverFlowException();
        }
    }

    private boolean isDeadAccount(Program program, DataWord address) {
        return program.getContractState().getAccount(MUtil.convertToGSCAddress(address.getLast20Bytes()))
                == null;
    }

    /**
     * Utility to calculate new total memory size needed for an operation. <br/> Basically just offset
     * + size, unless size is 0, in which case the result is also 0.
     *
     * @param offset starting position of the memory
     * @param size   number of bytes needed
     * @return offset + size, unless size is 0. In that case memNeeded is also 0.
     */
    private static BigInteger memNeeded(DataWord offset, DataWord size) {
        return size.isZero() ? BigInteger.ZERO : offset.value().add(size.value());
    }
}
