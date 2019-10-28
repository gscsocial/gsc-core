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

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.gsc.utils.ByteArray;
import org.gsc.protos.Protocol.DelegatedResourceAccountIndex;

@Slf4j(topic = "wrapper")
public class DelegatedResourceAccountIndexWrapper implements
        ProtoWrapper<DelegatedResourceAccountIndex> {

    private DelegatedResourceAccountIndex delegatedResourceAccountIndex;

    public DelegatedResourceAccountIndexWrapper(
            final DelegatedResourceAccountIndex delegatedResourceAccountIndex) {
        this.delegatedResourceAccountIndex = delegatedResourceAccountIndex;
    }

    public DelegatedResourceAccountIndexWrapper(final byte[] data) {
        try {
            this.delegatedResourceAccountIndex = DelegatedResourceAccountIndex.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public DelegatedResourceAccountIndexWrapper(ByteString address) {
        this.delegatedResourceAccountIndex = DelegatedResourceAccountIndex.newBuilder()
                .setAccount(address)
                .build();
    }

    public ByteString getAccount() {
        return this.delegatedResourceAccountIndex.getAccount();
    }

    public void setAccount(ByteString address) {
        this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
                .setAccount(address).build();
    }

    public List<ByteString> getFromAccountsList() {
        return this.delegatedResourceAccountIndex.getFromAccountsList();
    }

    public void setAllFromAccounts(List<ByteString> fromAccounts) {
        this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
                .clearFromAccounts()
                .addAllFromAccounts(fromAccounts)
                .build();
    }

    public void addFromAccount(ByteString fromAccount) {
        this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
                .addFromAccounts(fromAccount)
                .build();
    }

    public List<ByteString> getToAccountsList() {
        return this.delegatedResourceAccountIndex.getToAccountsList();
    }

    public void addToAccount(ByteString toAccount) {
        this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
                .addToAccounts(toAccount)
                .build();
    }

    public void setAllToAccounts(List<ByteString> toAccounts) {
        this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
                .clearToAccounts()
                .addAllToAccounts(toAccounts)
                .build();
    }

//  public void removeToAccount(ByteString toAccount) {
//    this.delegatedResourceAccountIndex = this.delegatedResourceAccountIndex.toBuilder()
//        . (toAccount)
//        .build();
//  }


    public byte[] createDbKey() {
        return getAccount().toByteArray();
    }

    public String createReadableString() {
        return ByteArray.toHexString(getAccount().toByteArray());
    }

    @Override
    public byte[] getData() {
        return this.delegatedResourceAccountIndex.toByteArray();
    }

    @Override
    public DelegatedResourceAccountIndex getInstance() {
        return this.delegatedResourceAccountIndex;
    }

}
