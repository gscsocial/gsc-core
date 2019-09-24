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
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.protos.Protocol.Witness;

@Slf4j(topic = "wrapper")
public class WitnessWrapper implements ProtoWrapper<Witness>, Comparable<WitnessWrapper> {

    private Witness witness;


    @Override
    public int compareTo(WitnessWrapper otherObject) {
        return Long.compare(otherObject.getVoteCount(), this.getVoteCount());
    }

    /**
     * WitnessWrapper constructor with pubKey and url.
     */
    public WitnessWrapper(final ByteString pubKey, final String url) {
        final Witness.Builder witnessBuilder = Witness.newBuilder();
        this.witness = witnessBuilder
                .setPubKey(pubKey)
                .setAddress(ByteString.copyFrom(ECKey.computeAddress(pubKey.toByteArray())))
                .setUrl(url).build();
    }

    public WitnessWrapper(final Witness witness) {
        this.witness = witness;
    }

    /**
     * WitnessWrapper constructor with address.
     */
    public WitnessWrapper(final ByteString address) {
        this.witness = Witness.newBuilder().setAddress(address).build();
    }

    /**
     * WitnessWrapper constructor with address and voteCount.
     */
    public WitnessWrapper(final ByteString address, final long voteCount, final String url) {
        final Witness.Builder witnessBuilder = Witness.newBuilder();
        this.witness = witnessBuilder
                .setAddress(address)
                .setVoteCount(voteCount).setUrl(url).build();
    }

    public WitnessWrapper(final byte[] data) {
        try {
            this.witness = Witness.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public ByteString getAddress() {
        return this.witness.getAddress();
    }

    public byte[] createDbKey() {
        return getAddress().toByteArray();
    }

    public String createReadableString() {
        return ByteArray.toHexString(getAddress().toByteArray());
    }

    @Override
    public byte[] getData() {
        return this.witness.toByteArray();
    }

    @Override
    public Witness getInstance() {
        return this.witness;
    }

    public void setPubKey(final ByteString pubKey) {
        this.witness = this.witness.toBuilder().setPubKey(pubKey).build();
    }

    public long getVoteCount() {
        return this.witness.getVoteCount();
    }

    public void setVoteCount(final long voteCount) {
        this.witness = this.witness.toBuilder().setVoteCount(voteCount).build();
    }

    public void setTotalProduced(final long totalProduced) {
        this.witness = this.witness.toBuilder().setTotalProduced(totalProduced).build();
    }

    public long getTotalProduced() {
        return this.witness.getTotalProduced();
    }

    public void setTotalMissed(final long totalMissed) {
        this.witness = this.witness.toBuilder().setTotalMissed(totalMissed).build();
    }

    public long getTotalMissed() {
        return this.witness.getTotalMissed();
    }

    public void setLatestBlockNum(final long latestBlockNum) {
        this.witness = this.witness.toBuilder().setLatestBlockNum(latestBlockNum).build();
    }

    public long getLatestBlockNum() {
        return this.witness.getLatestBlockNum();
    }

    public void setLatestSlotNum(final long latestSlotNum) {
        this.witness = this.witness.toBuilder().setLatestSlotNum(latestSlotNum).build();
    }

    public long getLatestSlotNum() {
        return this.witness.getLatestSlotNum();
    }

    public void setIsJobs(final boolean isJobs) {
        this.witness = this.witness.toBuilder().setIsJobs(isJobs).build();
    }

    public boolean getIsJobs() {
        return this.witness.getIsJobs();
    }

    public void setUrl(final String url) {
        this.witness = this.witness.toBuilder().setUrl(url).build();
    }

    public String getUrl() {
        return this.witness.getUrl();
    }
}
