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

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.*;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.DelegatedResourceAccountIndexWrapper;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.FreezeBalanceContract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class FreezeBalanceOperator extends AbstractOperator {

    FreezeBalanceOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        final FreezeBalanceContract freezeBalanceContract;
        try {
            freezeBalanceContract = contract.unpack(FreezeBalanceContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }
        AccountWrapper accountWrapper = dbManager.getAccountStore()
                .get(freezeBalanceContract.getOwnerAddress().toByteArray());

        long now = dbManager.getHeadBlockTimeStamp();
        long duration = freezeBalanceContract.getFrozenDuration() * 86_400_000;

        long newBalance = accountWrapper.getBalance() - freezeBalanceContract.getFrozenBalance();

        long frozenBalance = freezeBalanceContract.getFrozenBalance();
        long expireTime = now + duration;
        byte[] ownerAddress = freezeBalanceContract.getOwnerAddress().toByteArray();
        byte[] receiverAddress = freezeBalanceContract.getReceiverAddress().toByteArray();

        switch (freezeBalanceContract.getResource()) {
            case NET:
                if (!ArrayUtils.isEmpty(receiverAddress)
                        && dbManager.getDynamicPropertiesStore().supportDR()) {
                    delegateResource(ownerAddress, receiverAddress, true,
                            frozenBalance, expireTime);
                    accountWrapper.addDelegatedFrozenBalanceForNet(frozenBalance);
                } else {
                    long newFrozenBalanceForNet =
                            frozenBalance + accountWrapper.getFrozenBalance();
                    accountWrapper.setFrozenForNet(newFrozenBalanceForNet, expireTime);
                }
                dbManager.getDynamicPropertiesStore()
                        .addTotalNetWeight(frozenBalance / 1000_000L);
                break;
            case CPU:
                if (!ArrayUtils.isEmpty(receiverAddress)
                        && dbManager.getDynamicPropertiesStore().supportDR()) {
                    delegateResource(ownerAddress, receiverAddress, false,
                            frozenBalance, expireTime);
                    accountWrapper.addDelegatedFrozenBalanceForCpu(frozenBalance);
                } else {
                    long newFrozenBalanceForCpu =
                            frozenBalance + accountWrapper.getAccountResource()
                                    .getFrozenBalanceForCpu()
                                    .getFrozenBalance();
                    accountWrapper.setFrozenForCpu(newFrozenBalanceForCpu, expireTime);
                }
                dbManager.getDynamicPropertiesStore()
                        .addTotalCpuWeight(frozenBalance / 1000_000L);
                break;
        }

        accountWrapper.setBalance(newBalance);
        dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);

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
        if (!contract.is(FreezeBalanceContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [FreezeBalanceContract],real type[" + contract
                            .getClass() + "]");
        }

        final FreezeBalanceContract freezeBalanceContract;
        try {
            freezeBalanceContract = this.contract.unpack(FreezeBalanceContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        byte[] ownerAddress = freezeBalanceContract.getOwnerAddress().toByteArray();
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        if (accountWrapper == null) {
            String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
            throw new ContractValidateException(
                    "Account[" + readableOwnerAddress + "] not exists");
        }

        long frozenBalance = freezeBalanceContract.getFrozenBalance();
        if (frozenBalance <= 0) {
            throw new ContractValidateException("frozenBalance must be positive");
        }
        if (frozenBalance < 1_000_000L) {
            throw new ContractValidateException("frozenBalance must be more than 1GSC");
        }

        int frozenCount = accountWrapper.getFrozenCount();
        if (!(frozenCount == 0 || frozenCount == 1)) {
            throw new ContractValidateException("frozenCount must be 0 or 1");
        }
        if (frozenBalance > accountWrapper.getBalance()) {
            throw new ContractValidateException("frozenBalance must be less than accountBalance");
        }

        long frozenDuration = freezeBalanceContract.getFrozenDuration();
        long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
        long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();

        boolean needCheckFrozeTime = Args.getInstance().getCheckFrozenTime() == 1;//for test
        if (needCheckFrozeTime && !(frozenDuration >= minFrozenTime
                && frozenDuration <= maxFrozenTime)) {
            throw new ContractValidateException(
                    "frozenDuration must be less than " + maxFrozenTime + " days "
                            + "and more than " + minFrozenTime + " days");
        }

        switch (freezeBalanceContract.getResource()) {
            case NET:
                break;
            case CPU:
                break;
            default:
                throw new ContractValidateException(
                        "ResourceCode error,valid ResourceCode[NET、CPU]");
        }

        //todo：need version control and config for delegating resource
        byte[] receiverAddress = freezeBalanceContract.getReceiverAddress().toByteArray();
        //If the receiver is included in the contract, the receiver will receive the resource.
        if (!ArrayUtils.isEmpty(receiverAddress) && dbManager.getDynamicPropertiesStore().supportDR()) {
            if (Arrays.equals(receiverAddress, ownerAddress)) {
                throw new ContractValidateException(
                        "receiverAddress must not be the same as ownerAddress");
            }

            if (!Wallet.addressValid(receiverAddress)) {
                throw new ContractValidateException("Invalid receiverAddress");
            }

            AccountWrapper receiverWrapper = dbManager.getAccountStore().get(receiverAddress);
            if (receiverWrapper == null) {
                String readableOwnerAddress = StringUtil.createReadableString(receiverAddress);
                throw new ContractValidateException(
                        "Account[" + readableOwnerAddress + "] not exists");
            }

            if (dbManager.getDynamicPropertiesStore().getAllowGvmConstantinople() == 1
                    && receiverWrapper.getType() == AccountType.Contract) {
                throw new ContractValidateException(
                        "Do not allow delegate resources to contract addresses");
            }
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(FreezeBalanceContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

    private void delegateResource(byte[] ownerAddress, byte[] receiverAddress, boolean isNet,
                                  long balance, long expireTime) {
        byte[] key = DelegatedResourceWrapper.createDbKey(ownerAddress, receiverAddress);
        //modify DelegatedResourceStore
        DelegatedResourceWrapper delegatedResourceWrapper = dbManager.getDelegatedResourceStore()
                .get(key);
        if (delegatedResourceWrapper != null) {
            if (isNet) {
                delegatedResourceWrapper.addFrozenBalanceForNet(balance, expireTime);
            } else {
                delegatedResourceWrapper.addFrozenBalanceForCpu(balance, expireTime);
            }
        } else {
            delegatedResourceWrapper = new DelegatedResourceWrapper(
                    ByteString.copyFrom(ownerAddress),
                    ByteString.copyFrom(receiverAddress));
            if (isNet) {
                delegatedResourceWrapper.setFrozenBalanceForNet(balance, expireTime);
            } else {
                delegatedResourceWrapper.setFrozenBalanceForCpu(balance, expireTime);
            }

        }
        dbManager.getDelegatedResourceStore().put(key, delegatedResourceWrapper);

        //modify DelegatedResourceAccountIndexStore
        {
            DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexWrapper = dbManager
                    .getDelegatedResourceAccountIndexStore()
                    .get(ownerAddress);
            if (delegatedResourceAccountIndexWrapper == null) {
                delegatedResourceAccountIndexWrapper = new DelegatedResourceAccountIndexWrapper(
                        ByteString.copyFrom(ownerAddress));
            }
            List<ByteString> toAccountsList = delegatedResourceAccountIndexWrapper.getToAccountsList();
            if (!toAccountsList.contains(ByteString.copyFrom(receiverAddress))) {
                delegatedResourceAccountIndexWrapper.addToAccount(ByteString.copyFrom(receiverAddress));
            }
            dbManager.getDelegatedResourceAccountIndexStore()
                    .put(ownerAddress, delegatedResourceAccountIndexWrapper);
        }

        {
            DelegatedResourceAccountIndexWrapper delegatedResourceAccountIndexWrapper = dbManager
                    .getDelegatedResourceAccountIndexStore()
                    .get(receiverAddress);
            if (delegatedResourceAccountIndexWrapper == null) {
                delegatedResourceAccountIndexWrapper = new DelegatedResourceAccountIndexWrapper(
                        ByteString.copyFrom(receiverAddress));
            }
            List<ByteString> fromAccountsList = delegatedResourceAccountIndexWrapper
                    .getFromAccountsList();
            if (!fromAccountsList.contains(ByteString.copyFrom(ownerAddress))) {
                delegatedResourceAccountIndexWrapper.addFromAccount(ByteString.copyFrom(ownerAddress));
            }
            dbManager.getDelegatedResourceAccountIndexStore()
                    .put(receiverAddress, delegatedResourceAccountIndexWrapper);
        }

        //modify AccountStore
        AccountWrapper receiverWrapper = dbManager.getAccountStore().get(receiverAddress);
        if (isNet) {
            receiverWrapper.addAcquiredDelegatedFrozenBalanceForNet(balance);
        } else {
            receiverWrapper.addAcquiredDelegatedFrozenBalanceForCpu(balance);
        }

        dbManager.getAccountStore().put(receiverWrapper.createDbKey(), receiverWrapper);
    }

}
