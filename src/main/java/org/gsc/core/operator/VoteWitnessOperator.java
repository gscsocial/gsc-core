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
import static org.gsc.core.operator.OperatorConstant.WITNESS_EXCEPTION_STR;

import com.google.common.math.LongMath;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Iterator;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.gsc.db.dbsource.Deposit;
import org.gsc.utils.ByteArray;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.VotesWrapper;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.db.AccountStore;
import org.gsc.db.Manager;
import org.gsc.db.VotesStore;
import org.gsc.db.WitnessStore;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.VoteWitnessContract;
import org.gsc.protos.Contract.VoteWitnessContract.Vote;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class VoteWitnessOperator extends AbstractOperator {


    VoteWitnessOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            VoteWitnessContract voteContract = contract.unpack(VoteWitnessContract.class);
            countVoteAccount(voteContract, getDeposit());
            ret.setStatus(fee, code.SUCESS);
        } catch (InvalidProtocolBufferException e) {
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
        if (dbManager == null && (getDeposit() == null || getDeposit().getDbManager() == null)) {
            throw new ContractValidateException("No dbManager!");
        }
        if (!this.contract.is(VoteWitnessContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [VoteWitnessContract],real type[" + contract
                            .getClass() + "]");
        }
        final VoteWitnessContract contract;
        try {
            contract = this.contract.unpack(VoteWitnessContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        if (!Wallet.addressValid(contract.getOwnerAddress().toByteArray())) {
            throw new ContractValidateException("Invalid address");
        }
        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

        AccountStore accountStore = dbManager.getAccountStore();
        WitnessStore witnessStore = dbManager.getWitnessStore();

        if (contract.getVotesCount() == 0) {
            throw new ContractValidateException(
                    "VoteNumber must more than 0");
        }
        int maxVoteNumber = ChainConstant.MAX_VOTE_NUMBER;
        if (contract.getVotesCount() > maxVoteNumber) {
            throw new ContractValidateException(
                    "VoteNumber more than maxVoteNumber " + maxVoteNumber);
        }
        try {
            Iterator<Vote> iterator = contract.getVotesList().iterator();
            Long sum = 0L;
            while (iterator.hasNext()) {
                Vote vote = iterator.next();
                byte[] witnessCandidate = vote.getVoteAddress().toByteArray();
                if (!Wallet.addressValid(witnessCandidate)) {
                    throw new ContractValidateException("Invalid vote address!");
                }
                long voteCount = vote.getVoteCount();
                if (voteCount <= 0) {
                    throw new ContractValidateException("vote count must be greater than 0");
                }
                String readableWitnessAddress = StringUtil.createReadableString(vote.getVoteAddress());
                if (!Objects.isNull(getDeposit())) {
                    if (Objects.isNull(getDeposit().getAccount(witnessCandidate))) {
                        throw new ContractValidateException(
                                ACCOUNT_EXCEPTION_STR + readableWitnessAddress + NOT_EXIST_STR);
                    }
                } else if (!accountStore.has(witnessCandidate)) {
                    throw new ContractValidateException(
                            ACCOUNT_EXCEPTION_STR + readableWitnessAddress + NOT_EXIST_STR);
                }
                if (!Objects.isNull(getDeposit())) {
                    if (Objects.isNull(getDeposit().getWitness(witnessCandidate))) {
                        throw new ContractValidateException(
                                WITNESS_EXCEPTION_STR + readableWitnessAddress + NOT_EXIST_STR);
                    }
                } else if (!witnessStore.has(witnessCandidate)) {
                    throw new ContractValidateException(
                            WITNESS_EXCEPTION_STR + readableWitnessAddress + NOT_EXIST_STR);
                }
                sum = LongMath.checkedAdd(sum, vote.getVoteCount());
            }

            AccountWrapper accountWrapper =
                    (Objects.isNull(getDeposit())) ? accountStore.get(ownerAddress)
                            : getDeposit().getAccount(ownerAddress);
            if (accountWrapper == null) {
                throw new ContractValidateException(
                        ACCOUNT_EXCEPTION_STR + readableOwnerAddress + NOT_EXIST_STR);
            }

            long GSCPower = accountWrapper.getGSCPower();

            sum = LongMath.checkedMultiply(sum, 1000000L); //1Gsc -> drop. The vote count is based on GSC
            if (sum > GSCPower) {
                throw new ContractValidateException(
                        "The total number of votes[" + sum + "] is greater than the GscPower[" + GSCPower
                                + "]");
            }
        } catch (ArithmeticException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        return true;
    }

    private void countVoteAccount(VoteWitnessContract voteContract, Deposit deposit) {
        byte[] ownerAddress = voteContract.getOwnerAddress().toByteArray();

        VotesWrapper votesWrapper;
        VotesStore votesStore = dbManager.getVotesStore();
        AccountStore accountStore = dbManager.getAccountStore();

        AccountWrapper accountWrapper = (Objects.isNull(getDeposit())) ? accountStore.get(ownerAddress)
                : getDeposit().getAccount(ownerAddress);

        if (!Objects.isNull(getDeposit())) {
            VotesWrapper vWrapper = getDeposit().getVotesWrapper(ownerAddress);
            if (Objects.isNull(vWrapper)) {
                votesWrapper = new VotesWrapper(voteContract.getOwnerAddress(),
                        accountWrapper.getVotesList());
            } else {
                votesWrapper = vWrapper;
            }
        } else if (!votesStore.has(ownerAddress)) {
            votesWrapper = new VotesWrapper(voteContract.getOwnerAddress(),
                    accountWrapper.getVotesList());
        } else {
            votesWrapper = votesStore.get(ownerAddress);
        }

        accountWrapper.clearVotes();
        votesWrapper.clearNewVotes();

        voteContract.getVotesList().forEach(vote -> {
            logger.debug("countVoteAccount,address[{}]",
                    ByteArray.toHexString(vote.getVoteAddress().toByteArray()));

            votesWrapper.addNewVotes(vote.getVoteAddress(), vote.getVoteCount());
            accountWrapper.addVotes(vote.getVoteAddress(), vote.getVoteCount());
        });

        if (Objects.isNull(deposit)) {
            accountStore.put(accountWrapper.createDbKey(), accountWrapper);
            votesStore.put(ownerAddress, votesWrapper);
        } else {
            // cache
            deposit.putAccountValue(accountWrapper.createDbKey(), accountWrapper);
            deposit.putVoteValue(ownerAddress, votesWrapper);
        }

    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(VoteWitnessContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
