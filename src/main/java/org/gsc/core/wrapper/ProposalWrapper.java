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

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.gsc.utils.ByteArray;
import org.gsc.utils.StringUtil;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.protos.Protocol.Proposal;
import org.gsc.protos.Protocol.Proposal.State;

@Slf4j(topic = "wrapper")
public class ProposalWrapper implements ProtoWrapper<Proposal> {

    private Proposal proposal;

    public ProposalWrapper(final Proposal proposal) {
        this.proposal = proposal;
    }

    public ProposalWrapper(final byte[] data) {
        try {
            this.proposal = Proposal.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public ProposalWrapper(ByteString address, final long id) {
        this.proposal = Proposal.newBuilder()
                .setProposerAddress(address)
                .setProposalId(id)
                .build();
    }

    public long getID() {
        return this.proposal.getProposalId();
    }

    public void setID(long id) {
        this.proposal = this.proposal.toBuilder()
                .setProposalId(id)
                .build();
    }

    public ByteString getProposalAddress() {
        return this.proposal.getProposerAddress();
    }

    public void setProposalAddress(ByteString address) {
        this.proposal = this.proposal.toBuilder()
                .setProposerAddress(address)
                .build();
    }

    public Map<Long, Long> getParameters() {
        return this.proposal.getParametersMap();
    }

    public void setParameters(Map<Long, Long> parameters) {
        this.proposal = this.proposal.toBuilder()
                .putAllParameters(parameters)
                .build();
    }

    public long getExpirationTime() {
        return this.proposal.getExpirationTime();
    }

    public void setExpirationTime(long time) {
        this.proposal = this.proposal.toBuilder()
                .setExpirationTime(time)
                .build();
    }

    public long getCreateTime() {
        return this.proposal.getCreateTime();
    }

    public void setCreateTime(long time) {
        this.proposal = this.proposal.toBuilder()
                .setCreateTime(time)
                .build();
    }

    public List<ByteString> getApprovals() {
        return this.proposal.getApprovalsList();
    }

    public void removeApproval(ByteString address) {
        List<ByteString> approvals = Lists.newArrayList();
        approvals.addAll(getApprovals());
        approvals.remove(address);
        this.proposal = this.proposal.toBuilder()
                .clearApprovals()
                .addAllApprovals(approvals)
                .build();
    }

    public void clearApproval() {
        this.proposal = this.proposal.toBuilder().clearApprovals().build();
    }

    public void addApproval(ByteString committeeAddress) {
        this.proposal = this.proposal.toBuilder()
                .addApprovals(committeeAddress)
                .build();
    }

    public State getState() {
        return this.proposal.getState();
    }

    public void setState(State state) {
        this.proposal = this.proposal.toBuilder()
                .setState(state)
                .build();
    }

    public boolean hasProcessed() {
        return this.proposal.getState().equals(State.DISAPPROVED) || this.proposal.getState()
                .equals(State.APPROVED);
    }

    public boolean hasCanceled() {
        return this.proposal.getState().equals(State.CANCELED);
    }

    public boolean hasExpired(long time) {
        return this.proposal.getExpirationTime() <= time;
    }


    public byte[] createDbKey() {
        return calculateDbKey(getID());
    }

    public static byte[] calculateDbKey(long number) {
        return ByteArray.fromLong(number);
    }

    @Override
    public byte[] getData() {
        return this.proposal.toByteArray();
    }

    @Override
    public Proposal getInstance() {
        return this.proposal;
    }

    public boolean hasMostApprovals(List<ByteString> activeWitnesses) {
        long count = this.proposal.getApprovalsList().stream()
                .filter(witness -> activeWitnesses.contains(witness)).count();
        if (count != this.proposal.getApprovalsCount()) {
            List<ByteString> InvalidApprovalList = this.proposal.getApprovalsList().stream()
                    .filter(witness -> !activeWitnesses.contains(witness)).collect(Collectors.toList());
            logger.info("InvalidApprovalList:" + StringUtil.getAddressStringList(InvalidApprovalList));
        }
        if (activeWitnesses.size() != ChainConstant.MAX_ACTIVE_WITNESS_NUM) {
            logger.info("activeWitnesses size = {}", activeWitnesses.size());
        }
        return count >= activeWitnesses.size() * 7 / 10;
    }
}
