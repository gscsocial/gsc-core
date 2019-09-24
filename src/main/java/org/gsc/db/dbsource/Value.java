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

package org.gsc.db.dbsource;

import static org.gsc.db.dbsource.Type.VALUE_TYPE_UNKNOWN;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.*;
import org.gsc.runtime.config.VMConfig;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.core.exception.BadItemException;

public class Value {

    private Type type;
    private byte[] any = null;

    /**
     * @param any
     */
    public Value(byte[] any, Type type) {
        if (any != null && any.length > 0) {
            this.any = new byte[any.length];
            System.arraycopy(any, 0, this.any, 0, any.length);
            this.type = type.clone();
        }
    }

    /**
     * @param any
     * @param type
     */
    public Value(byte[] any, int type) {
        if (any != null && any.length > 0) {
            this.any = new byte[any.length];
            System.arraycopy(any, 0, this.any, 0, any.length);
            this.type = new Type(type);
        } else {
            if (VMConfig.allowMultiSign()) {
                this.type = new Type(VALUE_TYPE_UNKNOWN);
            }
        }
    }

    /**
     * @param value
     */
    private Value(Value value) {
        if (value.getAny() != null && value.getAny().length > 0) {
            this.any = new byte[value.any.length];
            System.arraycopy(value.getAny(), 0, this.any, 0, value.getAny().length);
            this.type = value.getType().clone();
        } else {
            if (VMConfig.allowMultiSign()) {
                this.type = new Type(VALUE_TYPE_UNKNOWN);
            }
        }
    }

    /**
     * @return
     */
    public Value clone() {
        return new Value(this);
    }

    /**
     * @return
     */
    public byte[] getAny() {
        return any;
    }

    /**
     * @return
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @param type
     */
    public void addType(Type type) {
        this.type.addType(type);
    }

    /**
     * @param type
     */
    public void addType(int type) {
        this.type.addType(type);
    }

    /**
     * @return
     */
    public AccountWrapper getAccount() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new AccountWrapper(any);
    }

    /**
     * @return
     */
    public BytesWrapper getBytes() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new BytesWrapper(any);
    }

    /**
     * @return
     */
    public TransactionWrapper getTransaction() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        try {
            return new TransactionWrapper(any);
        } catch (BadItemException e) {
            return null;
        }
    }

    /**
     * @return
     */
    public BlockWrapper getBlock() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        try {
            return new BlockWrapper(any);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return
     */
    public WitnessWrapper getWitness() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new WitnessWrapper(any);

    }

    public VotesWrapper getVotes() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new VotesWrapper(any);
    }

    /**
     * @return
     */
    public BytesWrapper getBlockIndex() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new BytesWrapper(any);
    }

    /**
     * @return
     */
    public CodeWrapper getCode() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new CodeWrapper(any);
    }

    /**
     * @return
     */
    public ContractWrapper getContract() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new ContractWrapper(any);
    }


    public AssetIssueWrapper getAssetIssue() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new AssetIssueWrapper(any);
    }

    public ProposalWrapper getProposal() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new ProposalWrapper(any);
    }

    public BytesWrapper getDynamicProperties() {
        if (ArrayUtils.isEmpty(any)) {
            return null;
        }
        return new BytesWrapper(any);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        Value V = (Value) obj;
        if (Arrays.equals(this.any, V.getAny())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new Integer(type.hashCode() + Arrays.hashCode(any)).hashCode();
    }

    public static Value create(byte[] any, int type) {
        return new Value(any, type);
    }

//    public static Value create(byte[] any, Type type) {
//        return new Value(any, type);
//    }

    public static Value create(byte[] any) {
        return new Value(any, Type.VALUE_TYPE_NORMAL);
    }
}
