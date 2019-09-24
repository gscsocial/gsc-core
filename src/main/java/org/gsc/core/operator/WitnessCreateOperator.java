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
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.utils.StringUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.WitnessWrapper;
import org.gsc.core.wrapper.utils.TransactionUtil;
import org.gsc.db.Manager;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.WitnessCreateContract;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "operator")
public class WitnessCreateOperator extends AbstractOperator {

    WitnessCreateOperator(final Any contract, final Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
        long fee = calcFee();
        try {
            final WitnessCreateContract witnessCreateContract = this.contract
                    .unpack(WitnessCreateContract.class);
            this.createWitness(witnessCreateContract);
            ret.setStatus(fee, code.SUCESS);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            ret.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        } catch (BalanceInsufficientException e) {
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
        if (this.dbManager == null) {
            throw new ContractValidateException("No dbManager!");
        }
        if (!this.contract.is(WitnessCreateContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [WitnessCreateContract],real type[" + contract
                            .getClass() + "]");
        }
        final WitnessCreateContract contract;
        try {
            contract = this.contract.unpack(WitnessCreateContract.class);
        } catch (InvalidProtocolBufferException e) {
            throw new ContractValidateException(e.getMessage());
        }

        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("Invalid address");
        }

        if (!TransactionUtil.validUrl(contract.getUrl().toByteArray())) {
            throw new ContractValidateException("Invalid url");
        }

        AccountWrapper accountWrapper = this.dbManager.getAccountStore().get(ownerAddress);

        if (accountWrapper == null) {
            throw new ContractValidateException("account[" + readableOwnerAddress + "] not exists");
        }
    /* todo later
    if (ArrayUtils.isEmpty(accountWrapper.getAccountName().toByteArray())) {
      throw new ContractValidateException("account name not set");
    } */

        if (this.dbManager.getWitnessStore().has(ownerAddress)) {
            throw new ContractValidateException("Witness[" + readableOwnerAddress + "] has existed");
        }

        if (accountWrapper.getBalance() < dbManager.getDynamicPropertiesStore()
                .getAccountUpgradeCost()) {
            throw new ContractValidateException("balance < AccountUpgradeCost");
        }

        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(WitnessCreateContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return dbManager.getDynamicPropertiesStore().getAccountUpgradeCost();
    }

    private void createWitness(final WitnessCreateContract witnessCreateContract)
            throws BalanceInsufficientException {
        //Create Witness by witnessCreateContract
        final WitnessWrapper witnessWrapper = new WitnessWrapper(
                witnessCreateContract.getOwnerAddress(),
                0,
                witnessCreateContract.getUrl().toStringUtf8());

        logger.debug("createWitness,address[{}]", witnessWrapper.createReadableString());
        this.dbManager.getWitnessStore().put(witnessWrapper.createDbKey(), witnessWrapper);
        AccountWrapper accountWrapper = this.dbManager.getAccountStore()
                .get(witnessWrapper.createDbKey());
        accountWrapper.setIsWitness(true);
        if (dbManager.getDynamicPropertiesStore().getAllowMultiSign() == 1) {
            accountWrapper.setDefaultWitnessPermission(dbManager);
        }
        this.dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
        long cost = dbManager.getDynamicPropertiesStore().getAccountUpgradeCost();
        dbManager.adjustBalance(witnessCreateContract.getOwnerAddress().toByteArray(), -cost);

        dbManager.adjustBalance(this.dbManager.getAccountStore().getBlackhole().createDbKey(), +cost);

        dbManager.getDynamicPropertiesStore().addTotalCreateWitnessCost(cost);
    }
}
