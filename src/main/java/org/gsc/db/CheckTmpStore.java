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

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Spliterator;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ItemNotFoundException;

@Component
public class CheckTmpStore extends GSCDatabase<byte[]> {

    @Autowired
    public CheckTmpStore(ApplicationContext ctx) {
        super("tmp");
    }

    @Override
    public void put(byte[] key, byte[] item) {
    }

    @Override
    public void delete(byte[] key) {

    }

    @Override
    public byte[] get(byte[] key)
            throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException {
        return null;
    }

    @Override
    public boolean has(byte[] key) {
        return false;
    }

    @Override
    public void forEach(Consumer action) {

    }

    @Override
    public Spliterator spliterator() {
        return null;
    }
}