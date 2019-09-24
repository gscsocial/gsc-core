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

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.*;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.DelegatedResourceAccountIndexWrapper;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.UnfreezeBalanceContract;
import org.gsc.protos.Protocol.Account.AccountResource;
import org.gsc.protos.Protocol.Account.Frozen;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class UnfreezeBalanceOperator extends AbstractOperator {

    UnfreezeBalanceOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        final UnfreezeBalanceContract unfreezeBalanceContract;
        try {
            unfreezeBalanceContract = contract.unpack(UnfreezeBalanceContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }
        byte[] ownerAddress = unfreezeBalanceContract.getOwnerAddress().toByteArray();

        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        long oldBalance = accountWrapper.getBalance();

        long unfreezeBalance = 0L;
        ;

        byte[] receiverAddress = unfreezeBalanceContract.getReceiverAddress().toByteArray();
        //If the receiver is not included in the contract, unfreeze frozen balance for this account.
        //otherwise,unfreeze delegated frozen balance provided this account.
        if (!ArrayUtils.isEmpty(receiverAddress) && dbManager.getDynamicPropertiesStore().supportDR()) {
            byte[] key = DelegatedResourceWrapper
                    .createDbKey(unfreezeBalanceContract.getOwnerAddress().toByteArray(),
                            unfreezeBalanceContract.getReceiverAddress().toByteArray());
            DelegatedResourceWrapper delegatedResourceWrapper = dbManager.getDelegatedResourceStore()
                    .get(key);

            switch (unfreezeBalanceContract.getResource()) {
                case NET:
                    unfreezeBalance = delegatedResourceWrapper.getFrozenBalanceForNet();
                    delegatedResourceWrapper.setFrozenBalanceForNet(0, 0);
                    accountWrapper.addDelegatedFrozenBalanceForNet(-unfreezeBalance);
                    break;
                case CPU:
                    unfreezeBalance = delegatedResourceWrapper.getFrozenBalanceForCpu();
                    delegatedResourceWrapper.setFrozenBalanceForCpu(0, 0);
                    accountWrapper.addDelegatedFrozenBalanceForCpu(-unfreezeBalance);
                    break;
                default:
                    //this should never happen
                    break;
            }

            AccountWrapper receiverWrapper = dbManager.getAccountStore().get(receiverAddress);
            if (dbManager.getDynamicPropertiesStore().getAllowGvmConstantinople() == 0 ||
                    (receiverWrapper != null && receiverWrapper.getType() != AccountType.Contract)) {
                switch (unfreezeBalanceContract.getResource()) {
                    case NET:
                        receiverWrapper.addAcquiredDelegatedFrozenBalanceForNet(-unfreezeBalance);
                        break;
                    case CPU:
                        receiverWrapper.addAcquiredDelegatedFrozenBalanceForCpu(-unfreezeBalance);
                        break;
                    default:
                        //this should never happen
                        break;
                }
                dbManager.getAccountStore().put(receiverWrapper.createDbKey(), receiverWrapper);
            }

            accountWrapper.setBalance(oldBalance + unfreezeBalance);

            if (delegatedResourceWrapper.getFrozenBalanceForNet() == 0
                    && delegatedResourceWrapper.getFrozenBalanceForCpu() == 0) {
                dbManager.getDelegatedResourceStore().delete(key);

                //modify DelegatedResourceAccountIndexStore
                {
                    DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexWrapper = dbManager
                            .getDelegatedResourceAccountIndexStore()
                            .get(ownerAddress);
                    if (delegatedResourceAccountIndexWrapper != null) {
                        List<ByteString> toAccountsList = new ArrayList<>(delegatedResourceAccountIndexWrapper
                                .getToAccountsList());
                        toAccountsList.remove(ByteString.copyFrom(receiverAddress));
                        delegatedResourceAccountIndexWrapper.setAllToAccounts(toAccountsList);
                        dbManager.getDelegatedResourceAccountIndexStore()
                                .put(ownerAddress, delegatedResourceAccountIndexWrapper);
                    }
                }

                {
                    DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexWrapper = dbManager
                            .getDelegatedResourceAccountIndexStore()
                            .get(receiverAddress);
                    if (delegatedResourceAccountIndexWrapper != null) {
                        List<ByteString> fromAccountsList = new ArrayList<>(delegatedResourceAccountIndexWrapper
                                .getFromAccountsList());
                        fromAccountsList.remove(ByteString.copyFrom(ownerAddress));
                        delegatedResourceAccountIndexWrapper.setAllFromAccounts(fromAccountsList);
                        dbManager.getDelegatedResourceAccountIndexStore()
                                .put(receiverAddress, delegatedResourceAccountIndexWrapper);
                    }
                }

            } else {
                dbManager.getDelegatedResourceStore().put(key, delegatedResourceWrapper);
            }
        } else {
            switch (unfreezeBalanceContract.getResource()) {
                case NET:

                    List<Frozen> frozenList = Lists.newArrayList();
                    frozenList.addAll(accountWrapper.getFrozenList());
                    Iterator<Frozen> iterator = frozenList.iterator();
                    long now = dbManager.getHeadBlockTimeStamp();
                    while (iterator.hasNext()) {
                        Frozen next = iterator.next();
                        if (next.getExpireTime() <= now) {
                            unfreezeBalance += next.getFrozenBalance();
                            iterator.remove();
                        }
                    }

                    accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
                            .setBalance(oldBalance + unfreezeBalance)
                            .clearFrozen().addAllFrozen(frozenList).build());

                    break;
                case CPU:
                    unfreezeBalance = accountWrapper.getAccountResource().getFrozenBalanceForCpu()
                            .getFrozenBalance();

                    AccountResource newAccountResource = accountWrapper.getAccountResource().toBuilder()
                            .clearFrozenBalanceForCpu().build();
                    accountWrapper.setInstance(accountWrapper.getInstance().toBuilder()
                            .setBalance(oldBalance + unfreezeBalance)
                            .setAccountResource(newAccountResource).build());

                    break;
                default:
                    //this should never happen
                    break;
            }

        }

        switch (unfreezeBalanceContract.getResource()) {
            case NET:
                dbManager.getDynamicPropertiesStore()
                        .addTotalNetWeight(-unfreezeBalance / 1000_000L);
                break;
            case CPU:
                dbManager.getDynamicPropertiesStore()
                        .addTotalCpuWeight(-unfreezeBalance / 1000_000L);
                break;
            default:
                //this should never happen
                break;
        }

        VotesWrapper votesWrapper;
        if (!dbManager.getVotesStore().has(ownerAddress)) {
            votesWrapper = new VotesWrapper(unfreezeBalanceContract.getOwnerAddress(),
                    accountWrapper.getVotesList());
        } else {
            votesWrapper = dbManager.getVotesStore().get(ownerAddress);
        }
        accountWrapper.clearVotes();
        votesWrapper.clearNewVotes();

        dbManager.getAccountStore().put(ownerAddress, accountWrapper);

        dbManager.getVotesStore().put(ownerAddress, votesWrapper);

        ret.setUnfreezeAmount(unfreezeBalance);
        ret.setStatus(fee, code.SUCESS);

        return true;
    }

    @Override
    public boolean validate() throws ContractValidateException {
        if (this.contract == null) {
            throw new ContractValidateException("No contract!");
        }
        if (this.dbManager == null) {
            throw new ContractValidateException("No dbManager!");
        }
        if (!this.contract.is(UnfreezeBalanceContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [UnfreezeBalanceContract],real type[" + contract
                            .getClass() + "]");
        }
        final UnfreezeBalanceContract unfreezeBalanceContract;
        try {
            unfreezeBalanceContract = this.contract.unpack(UnfreezeBalanceContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        byte[] ownerAddress = unfreezeBalanceContract.getOwnerAddress().toByteArray();
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        if (accountWrapper == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }
        long now = dbManager.getHeadBlockTimeStamp();
        byte[] receiverAddress = unfreezeBalanceContract.getReceiverAddress().toByteArray();
        //If the receiver is not included in the contract, unfreeze frozen balance for this account.
        //otherwise,unfreeze delegated frozen balance provided this account.
        if (!ArrayUtils.isEmpty(receiverAddress) && dbManager.getDynamicPropertiesStore().supportDR()) {
            if (Arrays.equals(receiverAddress, ownerAddress)) {
                throw new ContractValidateException(
                        "receiverAddress must not be the same as ownerAddress");
            }

            if (!Wallet.addressValid(receiverAddress)) {
                throw new ContractValidateException("Invalid receiverAddress");
            }

            AccountWrapper receiverWrapper = dbManager.getAccountStore().get(receiverAddress);
            if (dbManager.getDynamicPropertiesStore().getAllowGvmConstantinople() == 0
                    && receiverWrapper == null) {
                String readableReceiverAddress = StringUtil.createReadableString(receiverAddress);
                throw new ContractValidateException(
                        "Receiver Account[" + readableReceiverAddress + "] not exists");
            }

            byte[] key = DelegatedResourceWrapper
                    .createDbKey(unfreezeBalanceContract.getOwnerAddress().toByteArray(),
                            unfreezeBalanceContract.getReceiverAddress().toByteArray());
            DelegatedResourceWrapper delegatedResourceWrapper = dbManager.getDelegatedResourceStore()
                    .get(key);
            if (delegatedResourceWrapper == null) {
                throw new ContractValidateException(
                        "delegated Resource not exists");
            }

            switch (unfreezeBalanceContract.getResource()) {
                case NET:
                    if (delegatedResourceWrapper.getFrozenBalanceForNet() <= 0) {
                        throw new ContractValidateException("no delegatedFrozenBalance(NET)");
                    }

                    if (dbManager.getDynamicPropertiesStore().getAllowGvmConstantinople() == 0) {
                        if (receiverWrapper.getAcquiredDelegatedFrozenBalanceForNet()
                                < delegatedResourceWrapper.getFrozenBalanceForNet()) {
                            throw new ContractValidateException(
                                    "AcquiredDelegatedFrozenBalanceForNet[" + receiverWrapper
                                            .getAcquiredDelegatedFrozenBalanceForNet() + "] < delegatedNet["
                                            + delegatedResourceWrapper.getFrozenBalanceForNet()
                                            + "]");
                        }
                    } else {
                        if (receiverWrapper != null && receiverWrapper.getType() != AccountType.Contract
                                && receiverWrapper.getAcquiredDelegatedFrozenBalanceForNet()
                                < delegatedResourceWrapper.getFrozenBalanceForNet()) {
                            throw new ContractValidateException(
                                    "AcquiredDelegatedFrozenBalanceForNet[" + receiverWrapper
                                            .getAcquiredDelegatedFrozenBalanceForNet() + "] < delegatedNet["
                                            + delegatedResourceWrapper.getFrozenBalanceForNet()
                                            + "]");
                        }
                    }

                    if (delegatedResourceWrapper.getExpireTimeForNet() > now) {
                        throw new ContractValidateException("It's not time to unfreeze.");
                    }
                    break;
                case CPU:
                    if (delegatedResourceWrapper.getFrozenBalanceForCpu() <= 0) {
                        throw new ContractValidateException("no delegateFrozenBalance(Cpu)");
                    }
                    if (dbManager.getDynamicPropertiesStore().getAllowGvmConstantinople() == 0) {
                        if (receiverWrapper.getAcquiredDelegatedFrozenBalanceForCpu()
                                < delegatedResourceWrapper.getFrozenBalanceForCpu()) {
                            throw new ContractValidateException(
                                    "AcquiredDelegatedFrozenBalanceForCpu[" + receiverWrapper
                                            .getAcquiredDelegatedFrozenBalanceForCpu() + "] < delegatedCpuy["
                                            + delegatedResourceWrapper.getFrozenBalanceForCpu() +
                                            "]");
                        }
                    } else {
                        if (receiverWrapper != null && receiverWrapper.getType() != AccountType.Contract
                                && receiverWrapper.getAcquiredDelegatedFrozenBalanceForCpu()
                                < delegatedResourceWrapper.getFrozenBalanceForCpu()) {
                            throw new ContractValidateException(
                                    "AcquiredDelegatedFrozenBalanceForCpu[" + receiverWrapper
                                            .getAcquiredDelegatedFrozenBalanceForCpu() + "] < delegatedCpu["
                                            + delegatedResourceWrapper.getFrozenBalanceForCpu() +
                                            "]");
                        }
                    }

                    if (delegatedResourceWrapper.getExpireTimeForCpu(dbManager) > now) {
                        throw new ContractValidateException("It's not time to unfreeze.");
                    }
                    break;
                default:
                    throw new ContractValidateException(
                            "ResourceCode error.valid ResourceCode[NET、Cpu]");
            }

        } else {
            switch (unfreezeBalanceContract.getResource()) {
                case NET:
                    if (accountWrapper.getFrozenCount() <= 0) {
                        throw new ContractValidateException("no frozenBalance(NET)");
                    }

                    long allowedUnfreezeCount = accountWrapper.getFrozenList().stream()
                            .filter(frozen -> frozen.getExpireTime() <= now).count();
                    if (allowedUnfreezeCount <= 0) {
                        throw new ContractValidateException("It's not time to unfreeze(NET).");
                    }
                    break;
                case CPU:
                    Frozen frozenBalanceForCpu = accountWrapper.getAccountResource()
                            .getFrozenBalanceForCpu();
                    if (frozenBalanceForCpu.getFrozenBalance() <= 0) {
                        throw new ContractValidateException("no frozenBalance(Cpu)");
                    }
                    if (frozenBalanceForCpu.getExpireTime() > now) {
                        throw new ContractValidateException("It's not time to unfreeze(Cpu).");
                    }

                    break;
                default:
                    throw new ContractValidateException(
                            "ResourceCode error.valid ResourceCode[NET、Cpu]");
            }

        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(UnfreezeBalanceContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
