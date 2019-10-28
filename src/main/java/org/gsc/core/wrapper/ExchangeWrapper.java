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

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.gsc.utils.ByteArray;
import org.gsc.core.wrapper.utils.ExchangeProcessor;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol.Exchange;

@Slf4j(topic = "wrapper")
public class ExchangeWrapper implements ProtoWrapper<Exchange> {

    private Exchange exchange;

    public ExchangeWrapper(final Exchange exchange) {
        this.exchange = exchange;
    }

    public ExchangeWrapper(final byte[] data) {
        try {
            this.exchange = Exchange.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public ExchangeWrapper(ByteString address, final long id, long createTime,
                           byte[] firstTokenID, byte[] secondTokenID) {
        this.exchange = Exchange.newBuilder()
                .setExchangeId(id)
                .setCreatorAddress(address)
                .setCreateTime(createTime)
                .setFirstTokenId(ByteString.copyFrom(firstTokenID))
                .setSecondTokenId(ByteString.copyFrom(secondTokenID))
                .build();
    }

    public void setID(long id) {
        this.exchange = this.exchange.toBuilder()
                .setExchangeId(id)
                .build();
    }

    public long getID() {
        return this.exchange.getExchangeId();
    }

    public ByteString getCreatorAddress() {
        return this.exchange.getCreatorAddress();
    }

    public void setExchangeAddress(ByteString address) {
        this.exchange = this.exchange.toBuilder()
                .setCreatorAddress(address)
                .build();
    }

    public void setBalance(long firstTokenBalance, long secondTokenBalance) {
        this.exchange = this.exchange.toBuilder()
                .setFirstTokenBalance(firstTokenBalance)
                .setSecondTokenBalance(secondTokenBalance)
                .build();
    }

    public long getCreateTime() {
        return this.exchange.getCreateTime();
    }

    public void setCreateTime(long time) {
        this.exchange = this.exchange.toBuilder()
                .setCreateTime(time)
                .build();
    }

    public byte[] getFirstTokenId() {
        return this.exchange.getFirstTokenId().toByteArray();
    }

    public byte[] getSecondTokenId() {
        return this.exchange.getSecondTokenId().toByteArray();
    }

    public void setFirstTokenId(byte[] id) {
        this.exchange = this.exchange.toBuilder()
                .setFirstTokenId(ByteString.copyFrom(id))
                .build();
    }

    public void setSecondTokenId(byte[] id) {
        this.exchange = this.exchange.toBuilder()
                .setSecondTokenId(ByteString.copyFrom(id))
                .build();
    }

    public long getFirstTokenBalance() {
        return this.exchange.getFirstTokenBalance();
    }

    public long getSecondTokenBalance() {
        return this.exchange.getSecondTokenBalance();
    }


    public byte[] createDbKey() {
        return calculateDbKey(getID());
    }

    public static byte[] calculateDbKey(long number) {
        return ByteArray.fromLong(number);
    }

    public long transaction(byte[] sellTokenID, long sellTokenQuant) {
        long supply = 1_000_000_000_000_000_000L;
        ExchangeProcessor processor = new ExchangeProcessor(supply);

        long buyTokenQuant = 0;
        long firstTokenBalance = this.exchange.getFirstTokenBalance();
        long secondTokenBalance = this.exchange.getSecondTokenBalance();

        if (this.exchange.getFirstTokenId().equals(ByteString.copyFrom(sellTokenID))) {
            buyTokenQuant = processor.exchange(firstTokenBalance,
                    secondTokenBalance,
                    sellTokenQuant);
            this.exchange = this.exchange.toBuilder()
                    .setFirstTokenBalance(firstTokenBalance + sellTokenQuant)
                    .setSecondTokenBalance(secondTokenBalance - buyTokenQuant)
                    .build();
        } else {
            buyTokenQuant = processor.exchange(secondTokenBalance,
                    firstTokenBalance,
                    sellTokenQuant);
            this.exchange = this.exchange.toBuilder()
                    .setFirstTokenBalance(firstTokenBalance - buyTokenQuant)
                    .setSecondTokenBalance(secondTokenBalance + sellTokenQuant)
                    .build();
        }

        return buyTokenQuant;
    }

    //be carefully, this function should be used only before AllowSameTokenName proposal is not active
    public void resetTokenWithID(Manager manager) {
        if (manager.getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            byte[] firstTokenName = this.exchange.getFirstTokenId().toByteArray();
            byte[] secondTokenName = this.exchange.getSecondTokenId().toByteArray();
            byte[] firstTokenID = firstTokenName;
            byte[] secondTokenID = secondTokenName;
            if (!Arrays.equals(firstTokenName, "_".getBytes())) {
                firstTokenID = manager.getAssetIssueStore().get(firstTokenName).getId().getBytes();
            }
            if (!Arrays.equals(secondTokenName, "_".getBytes())) {
                secondTokenID = manager.getAssetIssueStore().get(secondTokenName).getId().getBytes();
            }
            this.exchange = this.exchange.toBuilder()
                    .setFirstTokenId(ByteString.copyFrom(firstTokenID))
                    .setSecondTokenId(ByteString.copyFrom(secondTokenID))
                    .build();
        }
    }

    @Override
    public byte[] getData() {
        return this.exchange.toByteArray();
    }

    @Override
    public Exchange getInstance() {
        return this.exchange;
    }

}
