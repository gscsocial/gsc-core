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

import static org.gsc.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.gsc.runtime.vm.program.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;
import static org.gsc.runtime.vm.program.InternalTransaction.TrxType.TRX_PRECOMPILED_TYPE;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.*;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.gsc.runtime.Runtime;
import org.gsc.runtime.RuntimeImpl;
import org.gsc.runtime.config.VMConfig;
import org.gsc.runtime.vm.program.InternalTransaction;
import org.gsc.runtime.vm.program.InternalTransaction.TrxType;
import org.gsc.runtime.vm.program.Program.BadJumpDestinationException;
import org.gsc.runtime.vm.program.Program.IllegalOperationException;
import org.gsc.runtime.vm.program.Program.JVMStackOverFlowException;
import org.gsc.runtime.vm.program.Program.OutOfCpuException;
import org.gsc.runtime.vm.program.Program.OutOfMemoryException;
import org.gsc.runtime.vm.program.Program.OutOfTimeException;
import org.gsc.runtime.vm.program.Program.PrecompiledContractException;
import org.gsc.runtime.vm.program.Program.StackTooLargeException;
import org.gsc.runtime.vm.program.Program.StackTooSmallException;
import org.gsc.runtime.vm.program.Program.TransferException;
import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.gsc.db.dbsource.DepositImpl;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.config.args.Args;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol.SmartContract.ABI;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;

@Slf4j(topic = "TransactionTrace")
public class TransactionTrace {

    private TransactionWrapper trx;

    private ReceiptWrapper receipt;

    private Manager dbManager;

    private Runtime runtime;

    private CpuProcessor cpuProcessor;

    private InternalTransaction.TrxType trxType;

    private long txStartTimeInMs;

    public TransactionWrapper getTrx() {
        return trx;
    }

    public enum TimeResultType {
        NORMAL,
        LONG_RUNNING,
        OUT_OF_TIME
    }

    @Getter
    @Setter
    private TimeResultType timeResultType = TimeResultType.NORMAL;

    public TransactionTrace(TransactionWrapper trx, Manager dbManager) {
        this.trx = trx;
        Transaction.Contract.ContractType contractType = this.trx.getInstance().getRawData()
                .getContract(0).getType();
        switch (contractType.getNumber()) {
            case ContractType.TriggerSmartContract_VALUE:
                trxType = TRX_CONTRACT_CALL_TYPE;
                break;
            case ContractType.CreateSmartContract_VALUE:
                trxType = TRX_CONTRACT_CREATION_TYPE;
                break;
            default:
                trxType = TRX_PRECOMPILED_TYPE;
        }

        this.dbManager = dbManager;
        this.receipt = new ReceiptWrapper(Sha256Hash.ZERO_HASH);

        this.cpuProcessor = new CpuProcessor(this.dbManager);
    }

    private boolean needVM() {
        return this.trxType == TRX_CONTRACT_CALL_TYPE || this.trxType == TRX_CONTRACT_CREATION_TYPE;
    }

    public void init(BlockWrapper blockCap) {
        init(blockCap, false);
    }

    //pre transaction check
    public void init(BlockWrapper blockCap, boolean eventPluginLoaded) {
        txStartTimeInMs = System.currentTimeMillis();
        DepositImpl deposit = DepositImpl.createRoot(dbManager);
        runtime = new RuntimeImpl(this, blockCap, deposit, new ProgramInvokeFactoryImpl());
        runtime.setEnableEventListener(eventPluginLoaded);
    }

    public void checkIsConstant() throws ContractValidateException, VMIllegalException {
        if (VMConfig.allowGvmConstantinople()) {
            return;
        }

        TriggerSmartContract triggerContractFromTransaction = ContractWrapper
                .getTriggerContractFromTransaction(this.getTrx().getInstance());
        if (TrxType.TRX_CONTRACT_CALL_TYPE == this.trxType) {
            DepositImpl deposit = DepositImpl.createRoot(dbManager);
            ContractWrapper contract = deposit
                    .getContract(triggerContractFromTransaction.getContractAddress().toByteArray());
            if (contract == null) {
                logger.info("contract: {} is not in contract store", Wallet
                        .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray()));
                throw new ContractValidateException("contract: " + Wallet
                        .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray())
                        + " is not in contract store");
            }
            ABI abi = contract.getInstance().getAbi();
            if (Wallet.isConstant(abi, triggerContractFromTransaction)) {
                throw new VMIllegalException("cannot call constant method");
            }
        }
    }

    //set bill
    public void setBill(long cpuUsage) {
        if (cpuUsage < 0) {
            cpuUsage = 0L;
        }
        receipt.setCpuUsageTotal(cpuUsage);
    }

    //set net bill
    public void setNetBill(long netUsage, long netFee) {
        receipt.setNetUsage(netUsage);
        receipt.setNetFee(netFee);
    }

    public void addNetBill(long netFee) {
        receipt.addNetFee(netFee);
    }

    public void exec()
            throws ContractExeException, ContractValidateException, VMIllegalException {
        /*  VM execute  */
        runtime.execute();
        runtime.go();

        if (TRX_PRECOMPILED_TYPE != runtime.getTrxType()) {
            if (contractResult.OUT_OF_TIME
                    .equals(receipt.getResult())) {
                setTimeResultType(TimeResultType.OUT_OF_TIME);
            } else if (System.currentTimeMillis() - txStartTimeInMs
                    > Args.getInstance().getLongRunningTime()) {
                setTimeResultType(TimeResultType.LONG_RUNNING);
            }
        }
    }

    public void finalization() throws ContractExeException {
        try {
            pay();
        } catch (BalanceInsufficientException e) {
            throw new ContractExeException(e.getMessage());
        }
        runtime.finalization();
    }

    /**
     * pay actually bill(include CPU and storage).
     */
    public void pay() throws BalanceInsufficientException {
        byte[] originAccount;
        byte[] callerAccount;
        long percent = 0;
        long originCpuLimit = 0;
        switch (trxType) {
            case TRX_CONTRACT_CREATION_TYPE:
                callerAccount = TransactionWrapper.getOwner(trx.getInstance().getRawData().getContract(0));
                originAccount = callerAccount;
                break;
            case TRX_CONTRACT_CALL_TYPE:
                TriggerSmartContract callContract = ContractWrapper
                        .getTriggerContractFromTransaction(trx.getInstance());
                ContractWrapper contractWrapper =
                        dbManager.getContractStore().get(callContract.getContractAddress().toByteArray());

                callerAccount = callContract.getOwnerAddress().toByteArray();
                originAccount = contractWrapper.getOriginAddress();
                percent = Math
                        .max(Constant.ONE_HUNDRED - contractWrapper.getConsumeUserResourcePercent(), 0);
                percent = Math.min(percent, Constant.ONE_HUNDRED);
                originCpuLimit = contractWrapper.getOriginCpuLimit();
                break;
            default:
                return;
        }

        // originAccount Percent = 30%
        AccountWrapper origin = dbManager.getAccountStore().get(originAccount);
        AccountWrapper caller = dbManager.getAccountStore().get(callerAccount);
        receipt.payCpuBill(
                dbManager,
                origin,
                caller,
                percent, originCpuLimit,
                cpuProcessor,
                dbManager.getWitnessController().getHeadSlot());
    }

    public boolean checkNeedRetry() {
        if (!needVM()) {
            return false;
        }
        return trx.getContractRet() != contractResult.OUT_OF_TIME && receipt.getResult()
                == contractResult.OUT_OF_TIME;
    }

    public void check() throws ReceiptCheckErrException {
        if (!needVM()) {
            return;
        }
        if (Objects.isNull(trx.getContractRet())) {
            throw new ReceiptCheckErrException("null resultCode");
        }
        if (!trx.getContractRet().equals(receipt.getResult())) {
            logger.info(
                    "this tx id: {}, the resultCode in received block: {}, the resultCode in self: {}",
                    Hex.toHexString(trx.getTransactionId().getBytes()), trx.getContractRet(),
                    receipt.getResult());
            throw new ReceiptCheckErrException("Different resultCode");
        }
    }

    public ReceiptWrapper getReceipt() {
        return receipt;
    }

    public void setResult() {
        if (!needVM()) {
            return;
        }
        RuntimeException exception = runtime.getResult().getException();
        if (Objects.isNull(exception) && StringUtils
                .isEmpty(runtime.getRuntimeError()) && !runtime.getResult().isRevert()) {
            receipt.setResult(contractResult.SUCCESS);
            return;
        }
        if (runtime.getResult().isRevert()) {
            receipt.setResult(contractResult.REVERT);
            return;
        }
        if (exception instanceof IllegalOperationException) {
            receipt.setResult(contractResult.ILLEGAL_OPERATION);
            return;
        }
        if (exception instanceof OutOfCpuException) {
            receipt.setResult(contractResult.OUT_OF_CPU);
            return;
        }
        if (exception instanceof BadJumpDestinationException) {
            receipt.setResult(contractResult.BAD_JUMP_DESTINATION);
            return;
        }
        if (exception instanceof OutOfTimeException) {
            receipt.setResult(contractResult.OUT_OF_TIME);
            return;
        }
        if (exception instanceof OutOfMemoryException) {
            receipt.setResult(contractResult.OUT_OF_MEMORY);
            return;
        }
        if (exception instanceof PrecompiledContractException) {
            receipt.setResult(contractResult.PRECOMPILED_CONTRACT);
            return;
        }
        if (exception instanceof StackTooSmallException) {
            receipt.setResult(contractResult.STACK_TOO_SMALL);
            return;
        }
        if (exception instanceof StackTooLargeException) {
            receipt.setResult(contractResult.STACK_TOO_LARGE);
            return;
        }
        if (exception instanceof JVMStackOverFlowException) {
            receipt.setResult(contractResult.JVM_STACK_OVER_FLOW);
            return;
        }
        if (exception instanceof TransferException) {
            receipt.setResult(contractResult.TRANSFER_FAILED);
            return;
        }

        logger.info("uncaught exception", exception);
        receipt.setResult(contractResult.UNKNOWN);
    }

    public String getRuntimeError() {
        return runtime.getRuntimeError();
    }

    public ProgramResult getRuntimeResult() {
        return runtime.getResult();
    }

    public Runtime getRuntime() {
        return runtime;
    }
}
