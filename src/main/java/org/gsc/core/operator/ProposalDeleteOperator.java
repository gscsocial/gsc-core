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

package org.gsc.core.operator;

import static org.gsc.core.operator.OperatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.gsc.core.operator.OperatorConstant.NOT_EXIST_STR;
import static org.gsc.core.operator.OperatorConstant.PROPOSAL_EXCEPTION_STR;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.ProposalWrapper;
import org.gsc.utils.ByteArray;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract.ProposalDeleteContract;
import org.gsc.protos.Protocol.Proposal.State;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class ProposalDeleteOperator extends AbstractOperator {

    ProposalDeleteOperator(final Any contract, final Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            final ProposalDeleteContract proposalDeleteContract = this.contract
                    .unpack(ProposalDeleteContract.class);
            ProposalWrapper proposalWrapper = (Objects.isNull(deposit)) ? dbManager.getProposalStore().
                    get(ByteArray.fromLong(proposalDeleteContract.getProposalId())) :
                    deposit.getProposalWrapper(ByteArray.fromLong(proposalDeleteContract.getProposalId()));
            proposalWrapper.setState(State.CANCELED);
            if (Objects.isNull(deposit)) {
                dbManager.getProposalStore().put(proposalWrapper.createDbKey(), proposalWrapper);
            } else {
                deposit.putProposalValue(proposalWrapper.createDbKey(), proposalWrapper);
            }

            ret.setStatus(fee, code.SUCESS);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        } catch (ItemNotFoundException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }
        return true;
    }

    @Override
    public boolean validate() throws ContractValidateException {
        if (this.contract == null) {
            throw new ContractValidateException("No contract!");
        }
        if (dbManager == null && (deposit == null || deposit.getDbManager() == null)) {
            throw new ContractValidateException("No dbManager!");
        }
        if (!this.contract.is(ProposalDeleteContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [ProposalDeleteContract],real type[" + contract
                            .getClass() + "]");
        }
        final ProposalDeleteContract contract;
        try {
            contract = this.contract.unpack(ProposalDeleteContract.class);
        } catch (InvalidProtocolBufferException e) {
            throw new ContractValidateException(e.getMessage());
        }

        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        if (!Objects.isNull(deposit)) {
            if (Objects.isNull(deposit.getAccount(ownerAddress))) {
                throw new ContractValidateException(
                        ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
            }
        } else if (!dbManager.getAccountStore().has(ownerAddress)) {
            throw new ContractValidateException(ACCOUNT_EXCEPTION_STR + readableOwnerAddress
                    + NOT_EXIST_STR);
        }

        long latestProposalNum = Objects.isNull(deposit) ? dbManager.getDynamicPropertiesStore()
                .getLatestProposalNum() : deposit.getLatestProposalNum();
        if (contract.getProposalId() > latestProposalNum) {
            throw new ContractValidateException(PROPOSAL_EXCEPTION_STR + contract.getProposalId()
                    + NOT_EXIST_STR);
        }

        ProposalWrapper proposalWrapper;
        try {
            proposalWrapper = Objects.isNull(getDeposit()) ? dbManager.getProposalStore().
                    get(ByteArray.fromLong(contract.getProposalId())) :
                    deposit.getProposalWrapper(ByteArray.fromLong(contract.getProposalId()));
        } catch (ItemNotFoundException ex) {
            throw new ContractValidateException(PROPOSAL_EXCEPTION_STR + contract.getProposalId()
                    + NOT_EXIST_STR);
        }

        long now = dbManager.getHeadBlockTimeStamp();
        if (!proposalWrapper.getProposalAddress().equals(contract.getOwnerAddress())) {
            throw new ContractValidateException(PROPOSAL_EXCEPTION_STR + contract.getProposalId() + "] "
                    + "is not proposed by " + readableOwnerAddress);
        }
        if (now >= proposalWrapper.getExpirationTime()) {
            throw new ContractValidateException(PROPOSAL_EXCEPTION_STR + contract.getProposalId()
                    + "] expired");
        }
        if (proposalWrapper.getState() == State.CANCELED) {
            throw new ContractValidateException(PROPOSAL_EXCEPTION_STR + contract.getProposalId()
                    + "] canceled");
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(ProposalDeleteContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }
}
