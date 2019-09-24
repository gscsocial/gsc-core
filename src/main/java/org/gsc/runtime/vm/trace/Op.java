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

import java.math.BigInteger;

import org.gsc.runtime.vm.OpCode;

public class Op {

    private OpCode code;
    private int deep;
    private int pc;
    private BigInteger cpu;
    private OpActions actions;

    public OpCode getCode() {
        return code;
    }

    public void setCode(OpCode code) {
        this.code = code;
    }

    public int getDeep() {
        return deep;
    }

    public void setDeep(int deep) {
        this.deep = deep;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public BigInteger getCpu() {
        return cpu;
    }

    public void setCpu(BigInteger cpu) {
        this.cpu = cpu;
    }

    public OpActions getActions() {
        return actions;
    }

    public void setActions(OpActions actions) {
        this.actions = actions;
    }
}
