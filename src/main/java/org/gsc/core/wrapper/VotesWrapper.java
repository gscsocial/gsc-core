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
import org.gsc.protos.Protocol.Vote;
import org.gsc.protos.Protocol.Votes;

@Slf4j(topic = "wrapper")
public class VotesWrapper implements ProtoWrapper<Votes> {

    private Votes votes;

    public VotesWrapper(final Votes votes) {
        this.votes = votes;
    }

    public VotesWrapper(final byte[] data) {
        try {
            this.votes = Votes.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public VotesWrapper(ByteString address, List<Vote> oldVotes) {
        this.votes = Votes.newBuilder()
                .setAddress(address)
                .addAllOldVotes(oldVotes)
                .build();
    }

    public ByteString getAddress() {
        return this.votes.getAddress();
    }

    public void setAddress(ByteString address) {
        this.votes = this.votes.toBuilder().setAddress(address).build();
    }

    public List<Vote> getOldVotes() {
        return this.votes.getOldVotesList();
    }

    public void setOldVotes(List<Vote> oldVotes) {
        this.votes = this.votes.toBuilder()
                .addAllOldVotes(oldVotes)
                .build();
    }

    public List<Vote> getNewVotes() {
        return this.votes.getNewVotesList();
    }

    public void clearNewVotes() {
        this.votes = this.votes.toBuilder()
                .clearNewVotes()
                .build();
    }

    public void addNewVotes(ByteString voteAddress, long voteCount) {
        this.votes = this.votes.toBuilder()
                .addNewVotes(Vote.newBuilder().setVoteAddress(voteAddress).setVoteCount(voteCount).build())
                .build();
    }

    public byte[] createDbKey() {
        return getAddress().toByteArray();
    }

    public String createReadableString() {
        return ByteArray.toHexString(getAddress().toByteArray());
    }

    @Override
    public byte[] getData() {
        return this.votes.toByteArray();
    }

    @Override
    public Votes getInstance() {
        return this.votes;
    }

}
