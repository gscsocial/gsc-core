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

package org.gsc.runtime.vm.program;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;

import java.util.*;

import lombok.Setter;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.runtime.event.trigger.ContractTrigger;
import org.gsc.runtime.vm.CallCreate;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.LogInfo;
import org.gsc.utils.ByteArraySet;
import org.gsc.utils.ByteUtil;

public class ProgramResult {

    private long cpuUsed = 0;
    private long futureRefund = 0;

    private byte[] hReturn = ByteUtil.EMPTY_BYTE_ARRAY;
    private byte[] contractAddress = ByteUtil.EMPTY_BYTE_ARRAY;
    private RuntimeException exception;
    private boolean revert;

    private Set<DataWord> deleteAccounts;
    private ByteArraySet touchedAccounts = new ByteArraySet();
    private List<InternalTransaction> internalTransactions;
    private List<LogInfo> logInfoList;

    private TransactionResultWrapper ret = new TransactionResultWrapper();

    @Setter
    private List<ContractTrigger> triggerList;

    /*
     * for testing runs ,
     * call/create is not executed
     * but dummy recorded
     */
    private List<CallCreate> callCreateList;

    public void spendCpu(long cpu) {
        cpuUsed += cpu;
    }

    public void setRevert() {
        this.revert = true;
    }

    public boolean isRevert() {
        return revert;
    }

    public void refundCpu(long cpu) {
        cpuUsed -= cpu;
    }

    public void setContractAddress(byte[] contractAddress) {
        this.contractAddress = Arrays.copyOf(contractAddress, contractAddress.length);
    }

    public byte[] getContractAddress() {
        return Arrays.copyOf(contractAddress, contractAddress.length);
    }

    public void setHReturn(byte[] hReturn) {
        this.hReturn = hReturn;

    }

    public byte[] getHReturn() {
        return hReturn;
    }

    public List<ContractTrigger> getTriggerList() {
        return triggerList != null ? triggerList : new LinkedList<>();
    }

    public TransactionResultWrapper getRet() {
        return ret;
    }

    public void setRet(TransactionResultWrapper ret) {
        this.ret = ret;
    }

    public RuntimeException getException() {
        return exception;
    }

    public long getCpuUsed() {
        return cpuUsed;
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    public Set<DataWord> getDeleteAccounts() {
        if (deleteAccounts == null) {
            deleteAccounts = new HashSet<>();
        }
        return deleteAccounts;
    }

    public void addDeleteAccount(DataWord address) {
        getDeleteAccounts().add(address);
    }

    public void addDeleteAccounts(Set<DataWord> accounts) {
        if (!isEmpty(accounts)) {
            getDeleteAccounts().addAll(accounts);
        }
    }

    public void addTouchAccount(byte[] addr) {
        touchedAccounts.add(addr);
    }

    public Set<byte[]> getTouchedAccounts() {
        return touchedAccounts;
    }

    public void addTouchAccounts(Set<byte[]> accounts) {
        if (!isEmpty(accounts)) {
            getTouchedAccounts().addAll(accounts);
        }
    }

    public List<LogInfo> getLogInfoList() {
        if (logInfoList == null) {
            logInfoList = new ArrayList<>();
        }
        return logInfoList;
    }

    public void addLogInfo(LogInfo logInfo) {
        getLogInfoList().add(logInfo);
    }

    public void addLogInfos(List<LogInfo> logInfos) {
        if (!isEmpty(logInfos)) {
            getLogInfoList().addAll(logInfos);
        }
    }

    public List<CallCreate> getCallCreateList() {
        if (callCreateList == null) {
            callCreateList = new ArrayList<>();
        }
        return callCreateList;
    }

    public void addCallCreate(byte[] data, byte[] destination, byte[] cpuLimit, byte[] value) {
        getCallCreateList().add(new CallCreate(data, destination, cpuLimit, value));
    }

    public List<InternalTransaction> getInternalTransactions() {
        if (internalTransactions == null) {
            internalTransactions = new ArrayList<>();
        }
        return internalTransactions;
    }

    public InternalTransaction addInternalTransaction(byte[] parentHash, int deep,
                                                      byte[] senderAddress, byte[] transferAddress, long value, byte[] data, String note,
                                                      long nonce, Map<String, Long> token) {
        InternalTransaction transaction = new InternalTransaction(parentHash, deep,
                size(internalTransactions), senderAddress, transferAddress, value, data, note, nonce,
                token);
        getInternalTransactions().add(transaction);
        return transaction;
    }

    public void addInternalTransaction(InternalTransaction internalTransaction) {
        getInternalTransactions().add(internalTransaction);
    }

    public void addInternalTransactions(List<InternalTransaction> internalTransactions) {
        getInternalTransactions().addAll(internalTransactions);
    }

    public void rejectInternalTransactions() {
        for (InternalTransaction internalTx : getInternalTransactions()) {
            internalTx.reject();
        }
    }

    public void addFutureRefund(long cpuValue) {
        futureRefund += cpuValue;
    }

    public long getFutureRefund() {
        return futureRefund;
    }

    public void resetFutureRefund() {
        futureRefund = 0;
    }

    public void reset() {
        getDeleteAccounts().clear();
        getLogInfoList().clear();
        resetFutureRefund();
    }

    public void merge(ProgramResult another) {
        addInternalTransactions(another.getInternalTransactions());
        if (another.getException() == null && !another.isRevert()) {
            addDeleteAccounts(another.getDeleteAccounts());
            addLogInfos(another.getLogInfoList());
            addFutureRefund(another.getFutureRefund());
            addTouchAccounts(another.getTouchedAccounts());
        }
    }

    public static ProgramResult createEmpty() {
        ProgramResult result = new ProgramResult();
        result.setHReturn(ByteUtil.EMPTY_BYTE_ARRAY);
        return result;
    }

}
