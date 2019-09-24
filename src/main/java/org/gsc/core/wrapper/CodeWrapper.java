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



package org.gsc.core.wrapper;

import lombok.extern.slf4j.Slf4j;
import org.gsc.utils.Sha256Hash;

import java.util.Arrays;

@Slf4j(topic = "wrapper")
public class CodeWrapper implements ProtoWrapper<byte[]> {

    private byte[] code;

    public CodeWrapper(byte[] code) {
        this.code = code;
    }

    public Sha256Hash getCodeHash() {
        return Sha256Hash.of(this.code);
    }

    @Override
    public byte[] getData() {
        return this.code;
    }

    @Override
    public byte[] getInstance() {
        return this.code;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.code);
    }
}
