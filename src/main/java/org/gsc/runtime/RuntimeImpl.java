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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

import com.google.protobuf.ByteString;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.core.operator.OperatorFactory;
import org.gsc.core.wrapper.*;
import org.spongycastle.util.encoders.Hex;
import org.gsc.runtime.event.EventPluginLoader;
import org.gsc.runtime.event.trigger.ContractTrigger;
import org.gsc.runtime.config.VMConfig;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.CpuCost;
import org.gsc.runtime.vm.LogInfoTriggerParser;
import org.gsc.runtime.vm.VM;
import org.gsc.runtime.vm.VMConstant;
import org.gsc.runtime.vm.VMUtils;
import org.gsc.runtime.vm.program.InternalTransaction;
import org.gsc.runtime.vm.program.Program;
import org.gsc.runtime.vm.program.ProgramPrecompile;
import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.runtime.vm.program.invoke.ProgramInvoke;
import org.gsc.runtime.vm.program.invoke.ProgramInvokeFactory;
import org.gsc.db.dbsource.Deposit;
import org.gsc.db.dbsource.DepositImpl;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.operator.Operator;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.config.args.Args;
import org.gsc.db.CpuProcessor;
import org.gsc.db.TransactionTrace;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;
import org.gsc.runtime.utils.MUtil;

@Slf4j(topic = "VM")
public class RuntimeImpl implements Runtime {

    private VMConfig config = VMConfig.getInstance();

    private Transaction trx;
    private BlockWrapper blockCap;
    private Deposit deposit;
    private ProgramInvokeFactory programInvokeFactory;
    private String runtimeError;

    private CpuProcessor cpuProcessor;
    private ProgramResult result = new ProgramResult();

    private VM vm;
    private Program program;
    private InternalTransaction rootInternalTransaction;

    @Getter
    @Setter
    private InternalTransaction.TrxType trxType;
    private InternalTransaction.ExecutorType executorType;

    //tx trace
    private TransactionTrace trace;

    @Getter
    @Setter
    private boolean isStaticCall = false;

    @Setter
    private boolean enableEventListener;

    private LogInfoTriggerParser logInfoTriggerParser;

    /**
     * For blockCap's trx run
     */
    public RuntimeImpl(TransactionTrace trace, BlockWrapper block, Deposit deposit,
                       ProgramInvokeFactory programInvokeFactory) {
        this.trace = trace;
        this.trx = trace.getTrx().getInstance();

        if (Objects.nonNull(block)) {
            this.blockCap = block;
            this.executorType = InternalTransaction.ExecutorType.ET_NORMAL_TYPE;
        } else {
            this.blockCap = new BlockWrapper(Block.newBuilder().build());
            this.executorType = InternalTransaction.ExecutorType.ET_PRE_TYPE;
        }
        this.deposit = deposit;
        this.programInvokeFactory = programInvokeFactory;
        this.cpuProcessor = new CpuProcessor(deposit.getDbManager());

        ContractType contractType = this.trx.getRawData().getContract(0).getType();
        switch (contractType.getNumber()) {
            case ContractType.TriggerSmartContract_VALUE:
                trxType = InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
                break;
            case ContractType.CreateSmartContract_VALUE:
                trxType = InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
                break;
            default:
                trxType = InternalTransaction.TrxType.TRX_PRECOMPILED_TYPE;
        }
    }


    /**
     * For constant trx with latest blockCap.
     */
    public RuntimeImpl(Transaction tx, BlockWrapper block, DepositImpl deposit,
                       ProgramInvokeFactory programInvokeFactory, boolean isStaticCall) {
        this(tx, block, deposit, programInvokeFactory);
        this.isStaticCall = isStaticCall;
    }

    private RuntimeImpl(Transaction tx, BlockWrapper block, DepositImpl deposit,
                        ProgramInvokeFactory programInvokeFactory) {
        this.trx = tx;
        this.deposit = deposit;
        this.programInvokeFactory = programInvokeFactory;
        this.executorType = InternalTransaction.ExecutorType.ET_PRE_TYPE;
        this.blockCap = block;
        this.cpuProcessor = new CpuProcessor(deposit.getDbManager());
        ContractType contractType = tx.getRawData().getContract(0).getType();
        switch (contractType.getNumber()) {
            case ContractType.TriggerSmartContract_VALUE:
                trxType = InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
                break;
            case ContractType.CreateSmartContract_VALUE:
                trxType = InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
                break;
            default:
                trxType = InternalTransaction.TrxType.TRX_PRECOMPILED_TYPE;
        }
    }


    private void precompiled() throws ContractValidateException, ContractExeException {
        TransactionWrapper trxCap = new TransactionWrapper(trx);
        final List<Operator> operatorList = OperatorFactory
                .createOperator(trxCap, deposit.getDbManager());

        for (Operator act : operatorList) {
            act.validate();
            act.execute(result.getRet());
        }
    }

    public void execute()
            throws ContractValidateException, ContractExeException, VMIllegalException {
        switch (trxType) {
            case TRX_PRECOMPILED_TYPE:
                precompiled();
                break;
            case TRX_CONTRACT_CREATION_TYPE:
                create();
                break;
            case TRX_CONTRACT_CALL_TYPE:
                call();
                break;
            default:
                throw new ContractValidateException("Unknown contract type");
        }
    }

    public long getAccountCpuLimitWithFixRatio(AccountWrapper account, long feeLimit,
                                               long callValue) {

        long dotPerCpu = Constant.DOT_PER_CPU;
        if (deposit.getDbManager().getDynamicPropertiesStore().getCpuFee() > 0) {
            dotPerCpu = deposit.getDbManager().getDynamicPropertiesStore().getCpuFee();
        }

        long leftFrozenCpu = cpuProcessor.getAccountLeftCpuFromFreeze(account);

        long cpuFromBalance = max(account.getBalance() - callValue, 0) / dotPerCpu;
        long availableCpu = Math.addExact(leftFrozenCpu, cpuFromBalance);

        long cpuFromFeeLimit = feeLimit / dotPerCpu;
        return min(availableCpu, cpuFromFeeLimit);

    }

    private long getAccountCpuLimitWithFloatRatio(AccountWrapper account, long feeLimit,
                                                  long callValue) {

        long dotPerCpu = Constant.DOT_PER_CPU;
        if (deposit.getDbManager().getDynamicPropertiesStore().getCpuFee() > 0) {
            dotPerCpu = deposit.getDbManager().getDynamicPropertiesStore().getCpuFee();
        }
        // can change the calc way
        long leftCpuFromFreeze = cpuProcessor.getAccountLeftCpuFromFreeze(account);
        callValue = max(callValue, 0);
        long cpuFromBalance = Math
                .floorDiv(max(account.getBalance() - callValue, 0), dotPerCpu);

        long cpuFromFeeLimit;
        long totalBalanceForCpuFreeze = account.getAllFrozenBalanceForCpu();
        if (0 == totalBalanceForCpuFreeze) {
            cpuFromFeeLimit =
                    feeLimit / dotPerCpu;
        } else {
            long totalCpuFromFreeze = cpuProcessor
                    .calculateGlobalCpuLimit(account);
            long leftBalanceForCpuFreeze = getCpuFee(totalBalanceForCpuFreeze,
                    leftCpuFromFreeze,
                    totalCpuFromFreeze);

            if (leftBalanceForCpuFreeze >= feeLimit) {
                cpuFromFeeLimit = BigInteger.valueOf(totalCpuFromFreeze)
                        .multiply(BigInteger.valueOf(feeLimit))
                        .divide(BigInteger.valueOf(totalBalanceForCpuFreeze)).longValueExact();
            } else {
                cpuFromFeeLimit = Math
                        .addExact(leftCpuFromFreeze,
                                (feeLimit - leftBalanceForCpuFreeze) / dotPerCpu);
            }
        }

        return min(Math.addExact(leftCpuFromFreeze, cpuFromBalance), cpuFromFeeLimit);
    }

    private long getTotalCpuLimitWithFloatRatio(AccountWrapper creator, AccountWrapper caller,
                                                TriggerSmartContract contract, long feeLimit, long callValue) {

        long callerCpuLimit = getAccountCpuLimitWithFloatRatio(caller, feeLimit, callValue);
        if (Arrays.equals(creator.getAddress().toByteArray(), caller.getAddress().toByteArray())) {
            return callerCpuLimit;
        }

        // creatorCpuFromFreeze
        long creatorCpuLimit = cpuProcessor.getAccountLeftCpuFromFreeze(creator);

        ContractWrapper contractWrapper = this.deposit
                .getContract(contract.getContractAddress().toByteArray());
        long consumeUserResourcePercent = contractWrapper.getConsumeUserResourcePercent();

        if (creatorCpuLimit * consumeUserResourcePercent
                > (Constant.ONE_HUNDRED - consumeUserResourcePercent) * callerCpuLimit) {
            return Math.floorDiv(callerCpuLimit * Constant.ONE_HUNDRED, consumeUserResourcePercent);
        } else {
            return Math.addExact(callerCpuLimit, creatorCpuLimit);
        }
    }

    public long getTotalCpuLimitWithFixRatio(AccountWrapper creator, AccountWrapper caller,
                                             TriggerSmartContract contract, long feeLimit, long callValue)
            throws ContractValidateException {

        long callerCpuLimit = getAccountCpuLimitWithFixRatio(caller, feeLimit, callValue);
        if (Arrays.equals(creator.getAddress().toByteArray(), caller.getAddress().toByteArray())) {
            // when the creator calls his own contract, this logic will be used.
            // so, the creator must use a BIG feeLimit to call his own contract,
            // which will cost the feeLimit TRX when the creator's frozen cpu is 0.
            return callerCpuLimit;
        }

        long creatorCpuLimit = 0;
        ContractWrapper contractWrapper = this.deposit
                .getContract(contract.getContractAddress().toByteArray());
        long consumeUserResourcePercent = contractWrapper.getConsumeUserResourcePercent();

        long originCpuLimit = contractWrapper.getOriginCpuLimit();
        if (originCpuLimit < 0) {
            throw new ContractValidateException("originCpuLimit can't be < 0");
        }

        if (consumeUserResourcePercent <= 0) {
            creatorCpuLimit = min(cpuProcessor.getAccountLeftCpuFromFreeze(creator),
                    originCpuLimit);
        } else {
            if (consumeUserResourcePercent < Constant.ONE_HUNDRED) {
                // creatorCpuLimit =
                // min(callerCpuLimit * (100 - percent) / percent, creatorLeftFrozenCpu, originCpuLimit)

                creatorCpuLimit = min(
                        BigInteger.valueOf(callerCpuLimit)
                                .multiply(BigInteger.valueOf(Constant.ONE_HUNDRED - consumeUserResourcePercent))
                                .divide(BigInteger.valueOf(consumeUserResourcePercent)).longValueExact(),
                        min(cpuProcessor.getAccountLeftCpuFromFreeze(creator), originCpuLimit)
                );
            }
        }
        return Math.addExact(callerCpuLimit, creatorCpuLimit);
    }

    public long getTotalCpuLimit(AccountWrapper creator, AccountWrapper caller,
                                 TriggerSmartContract contract, long feeLimit, long callValue)
            throws ContractValidateException {
        if (Objects.isNull(creator) && VMConfig.allowGvmConstantinople()) {
            return getAccountCpuLimitWithFixRatio(caller, feeLimit, callValue);
        }
        //  according to version
//        if (VMConfig.getCpuLimitHardFork()) {
            return getTotalCpuLimitWithFixRatio(creator, caller, contract, feeLimit, callValue);
//        } else {
//            return getTotalCpuLimitWithFloatRatio(creator, caller, contract, feeLimit, callValue);
//        }
    }

    private boolean isCheckTransaction() {
        return this.blockCap != null && !this.blockCap.getInstance().getBlockHeader()
                .getWitnessSignature().isEmpty();
    }

    private double getCpuLimitInUsRatio() {

        double cpuLimitRatio;

        if (InternalTransaction.ExecutorType.ET_NORMAL_TYPE == executorType) {
            // self witness generates block
            if (this.blockCap != null && blockCap.generatedByMyself &&
                    this.blockCap.getInstance().getBlockHeader().getWitnessSignature().isEmpty()) {
                cpuLimitRatio = 1.0;
            } else {
                // self witness or other witness or fullnode verifies block
                if (trx.getRet(0).getContractRet() == contractResult.OUT_OF_TIME) {
                    cpuLimitRatio = Args.getInstance().getMinTimeRatio();
                } else {
                    cpuLimitRatio = Args.getInstance().getMaxTimeRatio();
                }
            }
        } else {
            // self witness or other witness or fullnode receives tx
            cpuLimitRatio = 1.0;
        }

        return cpuLimitRatio;
    }

    /*
     **/
    private void create()
            throws ContractValidateException, VMIllegalException {
        if (!deposit.getDbManager().getDynamicPropertiesStore().supportVM()) {
            throw new ContractValidateException("vm work is off, need to be opened by the committee");
        }

        CreateSmartContract contract = ContractWrapper.getSmartContractFromTransaction(trx);
        if (contract == null) {
            throw new ContractValidateException("Cannot get CreateSmartContract from transaction");
        }
        SmartContract newSmartContract = contract.getNewContract();
        if (!contract.getOwnerAddress().equals(newSmartContract.getOriginAddress())) {
            logger.info("OwnerAddress not equals OriginAddress");
            throw new VMIllegalException("OwnerAddress is not equals OriginAddress");
        }

        byte[] contractName = newSmartContract.getName().getBytes();

        if (contractName.length > VMConstant.CONTRACT_NAME_LENGTH) {
            throw new ContractValidateException("contractName's length cannot be greater than 32");
        }

        long percent = contract.getNewContract().getConsumeUserResourcePercent();
        if (percent < 0 || percent > Constant.ONE_HUNDRED) {
            throw new ContractValidateException("percent must be >= 0 and <= 100");
        }

        byte[] contractAddress = Wallet.generateContractAddress(trx);
        // insure the new contract address haven't exist
        if (deposit.getAccount(contractAddress) != null) {
            throw new ContractValidateException(
                    "Trying to create a contract with existing contract address: " + Wallet
                            .encode58Check(contractAddress));
        }

        newSmartContract = newSmartContract.toBuilder()
                .setContractAddress(ByteString.copyFrom(contractAddress)).build();
        long callValue = newSmartContract.getCallValue();
        long tokenValue = 0;
        long tokenId = 0;
        if (VMConfig.allowGvmTransferGrc10()) {
            tokenValue = contract.getCallTokenValue();
            tokenId = contract.getTokenId();
        }
        byte[] callerAddress = contract.getOwnerAddress().toByteArray();
        // create vm to constructor smart contract
        try {
            long feeLimit = trx.getRawData().getFeeLimit();
            if (feeLimit < 0 || feeLimit > VMConfig.MAX_FEE_LIMIT) {
                logger.info("invalid feeLimit {}", feeLimit);
                throw new ContractValidateException(
                        "feeLimit must be >= 0 and <= " + VMConfig.MAX_FEE_LIMIT);
            }
            AccountWrapper creator = this.deposit
                    .getAccount(newSmartContract.getOriginAddress().toByteArray());

            long cpuLimit;
            // according to version

//            if (VMConfig.getCpuLimitHardFork()) {
                if (callValue < 0) {
                    throw new ContractValidateException("callValue must >= 0");
                }
                if (tokenValue < 0) {
                    throw new ContractValidateException("tokenValue must >= 0");
                }
                if (newSmartContract.getOriginCpuLimit() <= 0) {
                    throw new ContractValidateException("The originCpuLimit must be > 0");
                }
                cpuLimit = getAccountCpuLimitWithFixRatio(creator, feeLimit, callValue);
//            } else {
//                cpuLimit = getAccountCpuLimitWithFloatRatio(creator, feeLimit, callValue);
//            }

            checkTokenValueAndId(tokenValue, tokenId);

            byte[] ops = newSmartContract.getBytecode().toByteArray();
            rootInternalTransaction = new InternalTransaction(trx, trxType);

            long maxCpuTimeOfOneTx = deposit.getDbManager().getDynamicPropertiesStore()
                    .getMaxCpuTimeOfOneTx() * Constant.ONE_THOUSAND;
            long thisTxCPULimitInUs = (long) (maxCpuTimeOfOneTx * getCpuLimitInUsRatio());
            long vmStartInUs = System.nanoTime() / Constant.ONE_THOUSAND;
            long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;
            ProgramInvoke programInvoke = programInvokeFactory
                    .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE, executorType, trx,
                            tokenValue, tokenId, blockCap.getInstance(), deposit, vmStartInUs,
                            vmShouldEndInUs, cpuLimit);
            this.vm = new VM(config);
            this.program = new Program(ops, programInvoke, rootInternalTransaction, config,
                    this.blockCap);
            byte[] txId = new TransactionWrapper(trx).getTransactionId().getBytes();
            this.program.setRootTransactionId(txId);
            if (enableEventListener &&
                    (EventPluginLoader.getInstance().isContractEventTriggerEnable()
                            || EventPluginLoader.getInstance().isContractLogTriggerEnable())
                    && isCheckTransaction()) {
                logInfoTriggerParser = new LogInfoTriggerParser(blockCap.getNum(), blockCap.getTimeStamp(),
                        txId, callerAddress);

            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            throw new ContractValidateException(e.getMessage());
        }
        program.getResult().setContractAddress(contractAddress);

        deposit.createAccount(contractAddress, newSmartContract.getName(),
                Protocol.AccountType.Contract);

        deposit.createContract(contractAddress, new ContractWrapper(newSmartContract));
        byte[] code = newSmartContract.getBytecode().toByteArray();
        if (!VMConfig.allowGvmConstantinople()) {
            deposit.saveCode(contractAddress, ProgramPrecompile.getCode(code));
        }
        // transfer from callerAddress to contractAddress according to callValue
        if (callValue > 0) {
            MUtil.transfer(this.deposit, callerAddress, contractAddress, callValue);
        }
        if (VMConfig.allowGvmTransferGrc10()) {
            if (tokenValue > 0) {
                MUtil.transferToken(this.deposit, callerAddress, contractAddress, String.valueOf(tokenId),
                        tokenValue);
            }
        }

    }

    /**
     * **
     */

    private void call()
            throws ContractValidateException {

        if (!deposit.getDbManager().getDynamicPropertiesStore().supportVM()) {
            logger.info("vm work is off, need to be opened by the committee");
            throw new ContractValidateException("VM work is off, need to be opened by the committee");
        }

        Contract.TriggerSmartContract contract = ContractWrapper.getTriggerContractFromTransaction(trx);
        if (contract == null) {
            return;
        }

        if (contract.getContractAddress() == null) {
            throw new ContractValidateException("Cannot get contract address from TriggerContract");
        }

        byte[] contractAddress = contract.getContractAddress().toByteArray();

        ContractWrapper deployedContract = this.deposit.getContract(contractAddress);
        if (null == deployedContract) {
            logger.info("No contract or not a smart contract");
            throw new ContractValidateException("No contract or not a smart contract");
        }

        long callValue = contract.getCallValue();
        long tokenValue = 0;
        long tokenId = 0;
        if (VMConfig.allowGvmTransferGrc10()) {
            tokenValue = contract.getCallTokenValue();
            tokenId = contract.getTokenId();
        }

//        if (VMConfig.getCpuLimitHardFork()) {
            if (callValue < 0) {
                throw new ContractValidateException("callValue must >= 0");
            }
            if (tokenValue < 0) {
                throw new ContractValidateException("tokenValue must >= 0");
            }
//        }

        byte[] callerAddress = contract.getOwnerAddress().toByteArray();
        checkTokenValueAndId(tokenValue, tokenId);

        byte[] code = this.deposit.getCode(contractAddress);
        if (isNotEmpty(code)) {

            long feeLimit = trx.getRawData().getFeeLimit();
            if (feeLimit < 0 || feeLimit > VMConfig.MAX_FEE_LIMIT) {
                logger.info("invalid feeLimit {}", feeLimit);
                throw new ContractValidateException(
                        "feeLimit must be >= 0 and <= " + VMConfig.MAX_FEE_LIMIT);
            }
            AccountWrapper caller = this.deposit.getAccount(callerAddress);
            long cpuLimit;
            if (isStaticCall) {
                cpuLimit = Constant.CPU_LIMIT_IN_CONSTANT_TX;
            } else {
                AccountWrapper creator = this.deposit
                        .getAccount(deployedContract.getInstance().getOriginAddress().toByteArray());
                cpuLimit = getTotalCpuLimit(creator, caller, contract, feeLimit, callValue);
            }

            long maxCpuTimeOfOneTx = deposit.getDbManager().getDynamicPropertiesStore()
                    .getMaxCpuTimeOfOneTx() * Constant.ONE_THOUSAND;
            long thisTxCPULimitInUs =
                    (long) (maxCpuTimeOfOneTx * getCpuLimitInUsRatio());
            long vmStartInUs = System.nanoTime() / Constant.ONE_THOUSAND;
            long vmShouldEndInUs = vmStartInUs + thisTxCPULimitInUs;
            ProgramInvoke programInvoke = programInvokeFactory
                    .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE, executorType, trx,
                            tokenValue, tokenId, blockCap.getInstance(), deposit, vmStartInUs,
                            vmShouldEndInUs, cpuLimit);
            if (isStaticCall) {
                programInvoke.setStaticCall();
            }
            this.vm = new VM(config);
            rootInternalTransaction = new InternalTransaction(trx, trxType);
            this.program = new Program(code, programInvoke, rootInternalTransaction, config,
                    this.blockCap);
            byte[] txId = new TransactionWrapper(trx).getTransactionId().getBytes();
            this.program.setRootTransactionId(txId);

            if (enableEventListener &&
                    (EventPluginLoader.getInstance().isContractEventTriggerEnable()
                            || EventPluginLoader.getInstance().isContractLogTriggerEnable())
                    && isCheckTransaction()) {
                logInfoTriggerParser = new LogInfoTriggerParser(blockCap.getNum(), blockCap.getTimeStamp(),
                        txId, callerAddress);
            }
        }

        program.getResult().setContractAddress(contractAddress);
        //transfer from callerAddress to targetAddress according to callValue

        if (callValue > 0) {
            MUtil.transfer(this.deposit, callerAddress, contractAddress, callValue);
        }
        if (VMConfig.allowGvmTransferGrc10()) {
            if (tokenValue > 0) {
                MUtil.transferToken(this.deposit, callerAddress, contractAddress, String.valueOf(tokenId),
                        tokenValue);
            }
        }

    }

    public void go() {
        try {
            if (vm != null) {
                TransactionWrapper trxCap = new TransactionWrapper(trx);
                if (null != blockCap && blockCap.generatedByMyself && null != trxCap.getContractRet()
                        && contractResult.OUT_OF_TIME == trxCap.getContractRet()) {
                    result = program.getResult();
                    program.spendAllCpu();

                    Program.OutOfTimeException e = Program.Exception.alreadyTimeOut();
                    runtimeError = e.getMessage();
                    result.setException(e);
                    throw e;
                }

                vm.play(program);
                result = program.getResult();

                if (isStaticCall) {
                    long callValue = TransactionWrapper.getCallValue(trx.getRawData().getContract(0));
                    long callTokenValue = TransactionWrapper
                            .getCallTokenValue(trx.getRawData().getContract(0));
                    if (callValue > 0 || callTokenValue > 0) {
                        runtimeError = "constant cannot set call value or call token value.";
                        result.rejectInternalTransactions();
                    }
                    return;
                }

                if (InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE == trxType && !result.isRevert()) {
                    byte[] code = program.getResult().getHReturn();
                    long saveCodeCpu = (long) getLength(code) * CpuCost.getInstance().getCREATE_DATA();
                    long afterSpend = program.getCpuLimitLeft().longValue() - saveCodeCpu;
                    if (afterSpend < 0) {
                        if (null == result.getException()) {
                            result.setException(Program.Exception
                                    .notEnoughSpendCpu("save just created contract code",
                                            saveCodeCpu, program.getCpuLimitLeft().longValue()));
                        }
                    } else {
                        result.spendCpu(saveCodeCpu);
                        if (VMConfig.allowGvmConstantinople()) {
                            deposit.saveCode(program.getContractAddress().getNoLeadZeroesData(), code);
                        }
                    }
                }

                if (result.getException() != null || result.isRevert()) {
                    result.getDeleteAccounts().clear();
                    result.getLogInfoList().clear();
                    result.resetFutureRefund();
                    result.rejectInternalTransactions();

                    if (result.getException() != null) {
                        if (!(result.getException() instanceof Program.TransferException)) {
                            program.spendAllCpu();
                        }
                        runtimeError = result.getException().getMessage();
                        throw result.getException();
                    } else {
                        runtimeError = "REVERT opcode executed";
                    }
                } else {
                    deposit.commit();

                    if (logInfoTriggerParser != null) {
                        List<ContractTrigger> triggers = logInfoTriggerParser
                                .parseLogInfos(program.getResult().getLogInfoList(), this.deposit);
                        program.getResult().setTriggerList(triggers);
                    }

                }
            } else {
                deposit.commit();
            }
        } catch (Program.JVMStackOverFlowException e) {
            program.spendAllCpu();
            result = program.getResult();
            result.setException(e);
            result.rejectInternalTransactions();
            runtimeError = result.getException().getMessage();
            logger.info("JVMStackOverFlowException: {}", result.getException().getMessage());
        } catch (Program.OutOfTimeException e) {
            program.spendAllCpu();
            result = program.getResult();
            result.setException(e);
            result.rejectInternalTransactions();
            runtimeError = result.getException().getMessage();
            logger.info("timeout: {}", result.getException().getMessage());
        } catch (Throwable e) {
            if (!(e instanceof Program.TransferException)) {
                program.spendAllCpu();
            }
            result = program.getResult();
            result.rejectInternalTransactions();
            if (Objects.isNull(result.getException())) {
                logger.error(e.getMessage(), e);
                result.setException(new RuntimeException("Unknown Throwable"));
            }
            if (StringUtils.isEmpty(runtimeError)) {
                runtimeError = result.getException().getMessage();
            }
            logger.info("runtime result is :{}", result.getException().getMessage());
        }
        if (!isStaticCall) {
            trace.setBill(result.getCpuUsed());
        }
    }

    private static long getCpuFee(long callerCpuUsage, long callerCpuFrozen,
                                  long callerCpuTotal) {
        if (callerCpuTotal <= 0) {
            return 0;
        }
        return BigInteger.valueOf(callerCpuFrozen).multiply(BigInteger.valueOf(callerCpuUsage))
                .divide(BigInteger.valueOf(callerCpuTotal)).longValueExact();
    }

    public void finalization() {
        if (StringUtils.isEmpty(runtimeError)) {
            for (DataWord contract : result.getDeleteAccounts()) {
                deposit.deleteContract(MUtil.convertToGSCAddress((contract.getLast20Bytes())));
            }
        }

        if (config.vmTrace() && program != null) {
            String traceContent = program.getTrace()
                    .result(result.getHReturn())
                    .error(result.getException())
                    .toString();

            if (config.vmTraceCompressed()) {
                traceContent = VMUtils.zipAndEncode(traceContent);
            }

            String txHash = Hex.toHexString(rootInternalTransaction.getHash());
            VMUtils.saveProgramTraceFile(config, txHash, traceContent);
        }

    }

    public void checkTokenValueAndId(long tokenValue, long tokenId) throws ContractValidateException {
        if (VMConfig.allowGvmTransferGrc10()) {
            if (VMConfig.allowMultiSign()) { //allowMultiSigns
                // tokenid can only be 0
                // or (MIN_TOKEN_ID, Long.Max]
                if (tokenId <= VMConstant.MIN_TOKEN_ID && tokenId != 0) {
                    throw new ContractValidateException("tokenId must > " + VMConstant.MIN_TOKEN_ID);
                }
                // tokenid can only be 0 when tokenvalue = 0,
                // or (MIN_TOKEN_ID, Long.Max]
                if (tokenValue > 0 && tokenId == 0) {
                    throw new ContractValidateException("invalid arguments with tokenValue = " + tokenValue +
                            ", tokenId = " + tokenId);
                }
            }
        }
    }

    public ProgramResult getResult() {
        return result;
    }

    public String getRuntimeError() {
        return runtimeError;
    }

}
