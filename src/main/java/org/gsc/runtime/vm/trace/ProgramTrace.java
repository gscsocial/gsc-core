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

package org.gsc.runtime.vm.trace;

import static java.lang.String.format;
import static org.gsc.runtime.vm.trace.Serializers.serializeFieldsOnly;

import java.util.ArrayList;
import java.util.List;

import org.spongycastle.util.encoders.Hex;
import org.gsc.runtime.config.VMConfig;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.OpCode;
import org.gsc.runtime.vm.program.invoke.ProgramInvoke;
import org.gsc.runtime.utils.MUtil;
import org.gsc.utils.ByteUtil;

public class ProgramTrace {

    private List<Op> ops = new ArrayList<>();
    private String result;
    private String error;
    private String contractAddress;

    public ProgramTrace() {
        this(null, null);
    }

    public ProgramTrace(VMConfig config, ProgramInvoke programInvoke) {
        if (programInvoke != null && config.vmTrace()) {
            contractAddress = Hex
                    .toHexString(MUtil.convertToGSCAddress(programInvoke.getContractAddress().getLast20Bytes()));
        }
    }

    public List<Op> getOps() {
        return ops;
    }

    public void setOps(List<Op> ops) {
        this.ops = ops;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public ProgramTrace result(byte[] result) {
        setResult(ByteUtil.toHexString(result));
        return this;
    }

    public ProgramTrace error(Exception error) {
        setError(error == null ? "" : format("%s: %s", error.getClass(), error.getMessage()));
        return this;
    }

    public Op addOp(byte code, int pc, int deep, DataWord cpu, OpActions actions) {
        Op op = new Op();
        op.setActions(actions);
        op.setCode(OpCode.code(code));
        op.setDeep(deep);
        op.setCpu(cpu.value());
        op.setPc(pc);

        ops.add(op);

        return op;
    }

    /**
     * Used for merging sub calls execution.
     */
    public void merge(ProgramTrace programTrace) {
        this.ops.addAll(programTrace.ops);
    }

    public String asJsonString(boolean formatted) {
        return serializeFieldsOnly(this, formatted);
    }

    @Override
    public String toString() {
        return asJsonString(true);
    }
}
