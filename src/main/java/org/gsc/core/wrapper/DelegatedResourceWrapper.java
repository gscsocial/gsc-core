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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol.DelegatedResource;

@Slf4j(topic = "wrapper")
public class DelegatedResourceWrapper implements ProtoWrapper<DelegatedResource> {

    private DelegatedResource delegatedResource;

    public DelegatedResourceWrapper(final DelegatedResource delegatedResource) {
        this.delegatedResource = delegatedResource;
    }

    public DelegatedResourceWrapper(final byte[] data) {
        try {
            this.delegatedResource = DelegatedResource.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public DelegatedResourceWrapper(ByteString from, ByteString to) {
        this.delegatedResource = DelegatedResource.newBuilder()
                .setFrom(from)
                .setTo(to)
                .build();
    }


    public ByteString getFrom() {
        return this.delegatedResource.getFrom();
    }

    public ByteString getTo() {
        return this.delegatedResource.getTo();
    }

    public long getFrozenBalanceForCpu() {
        return this.delegatedResource.getFrozenBalanceForCpu();
    }

    public void setFrozenBalanceForCpu(long cpu, long expireTime) {
        this.delegatedResource = this.delegatedResource.toBuilder()
                .setFrozenBalanceForCpu(cpu)
                .setExpireTimeForCpu(expireTime)
                .build();
    }

    public void addFrozenBalanceForCpu(long cpu, long expireTime) {
        this.delegatedResource = this.delegatedResource.toBuilder()
                .setFrozenBalanceForCpu(this.delegatedResource.getFrozenBalanceForCpu() + cpu)
                .setExpireTimeForCpu(expireTime)
                .build();
    }

    public long getFrozenBalanceForNet() {
        return this.delegatedResource.getFrozenBalanceForNet();
    }

    public void setFrozenBalanceForNet(long net, long expireTime) {
        this.delegatedResource = this.delegatedResource.toBuilder()
                .setFrozenBalanceForNet(net)
                .setExpireTimeForNet(expireTime)
                .build();
    }

    public void addFrozenBalanceForNet(long net, long expireTime) {
        this.delegatedResource = this.delegatedResource.toBuilder()
                .setFrozenBalanceForNet(this.delegatedResource.getFrozenBalanceForNet()
                        + net)
                .setExpireTimeForNet(expireTime)
                .build();
    }

    public long getExpireTimeForNet() {
        return this.delegatedResource.getExpireTimeForNet();
    }

    public long getExpireTimeForCpu(Manager manager) {
        if (manager.getDynamicPropertiesStore().getAllowMultiSign() == 0) {
            return this.delegatedResource.getExpireTimeForNet();
        } else {
            return this.delegatedResource.getExpireTimeForCpu();
        }
    }

    public void setExpireTimeForNet(long ExpireTime) {
        this.delegatedResource = this.delegatedResource.toBuilder()
                .setExpireTimeForNet(ExpireTime)
                .build();
    }

    public void setExpireTimeForCpu(long ExpireTime) {
        this.delegatedResource = this.delegatedResource.toBuilder()
                .setExpireTimeForCpu(ExpireTime)
                .build();
    }

    public byte[] createDbKey() {
        return createDbKey(this.delegatedResource.getFrom().toByteArray(),
                this.delegatedResource.getTo().toByteArray());
    }

    public static byte[] createDbKey(byte[] from, byte[] to) {
        byte[] key = new byte[from.length + to.length];
        System.arraycopy(from, 0, key, 0, from.length);
        System.arraycopy(to, 0, key, from.length, to.length);
        return key;
    }

    @Override
    public byte[] getData() {
        return this.delegatedResource.toByteArray();
    }

    @Override
    public DelegatedResource getInstance() {
        return this.delegatedResource;
    }

}
