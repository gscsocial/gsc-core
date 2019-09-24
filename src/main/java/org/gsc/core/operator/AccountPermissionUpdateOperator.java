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

import static java.util.stream.Collectors.toList;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.db.AccountStore;
import org.gsc.db.Manager;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.AccountPermissionUpdateContract;
import org.gsc.protos.Protocol.Key;
import org.gsc.protos.Protocol.Permission;
import org.gsc.protos.Protocol.Permission.PermissionType;
import org.gsc.protos.Protocol.Transaction.Result.code;


@Slf4j(topic = "operator")
public class AccountPermissionUpdateOperator extends AbstractOperator {

    AccountPermissionUpdateOperator(Any contract, Manager dbManager) {
        super(contract, dbManager);
    }

    @Override
    public boolean execute(TransactionResultWrapper result) throws ContractExeException {
        long fee = calcFee();
        final AccountPermissionUpdateContract accountPermissionUpdateContract;
        try {
            accountPermissionUpdateContract = contract.unpack(AccountPermissionUpdateContract.class);

            byte[] ownerAddress = accountPermissionUpdateContract.getOwnerAddress().toByteArray();
            AccountStore accountStore = dbManager.getAccountStore();
            AccountWrapper account = accountStore.get(ownerAddress);
            account.updatePermissions(accountPermissionUpdateContract.getOwner(),
                    accountPermissionUpdateContract.getWitness(),
                    accountPermissionUpdateContract.getActivesList());
            accountStore.put(ownerAddress, account);

            dbManager.adjustBalance(ownerAddress, -fee);
            dbManager.adjustBalance(dbManager.getAccountStore().getBlackhole().createDbKey(), fee);

            result.setStatus(fee, code.SUCESS);
        } catch (BalanceInsufficientException e) {
            logger.debug(e.getMessage(), e);
            result.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            result.setStatus(fee, code.FAILED);
            throw new ContractExeException(e.getMessage());
        }

        return true;
    }

    private boolean checkPermission(Permission permission) throws ContractValidateException {
        if (permission.getKeysCount() > dbManager.getDynamicPropertiesStore().getTotalSignNum()) {
            throw new ContractValidateException("number of keys in permission should not be greater "
                    + "than " + dbManager.getDynamicPropertiesStore().getTotalSignNum());
        }
        if (permission.getKeysCount() == 0) {
            throw new ContractValidateException("key's count should be greater than 0");
        }
        if (permission.getType() == PermissionType.Witness && permission.getKeysCount() != 1) {
            throw new ContractValidateException("Witness permission's key count should be 1");
        }
        if (permission.getThreshold() <= 0) {
            throw new ContractValidateException("permission's threshold should be greater than 0");
        }
        String name = permission.getPermissionName();
        if (!StringUtils.isEmpty(name) && name.length() > 32) {
            throw new ContractValidateException("permission's name is too long");
        }
        //check owner name ?
        if (permission.getParentId() != 0) {
            throw new ContractValidateException("permission's parent should be owner");
        }

        long weightSum = 0;
        List<ByteString> addressList = permission.getKeysList()
                .stream()
                .map(x -> x.getAddress())
                .distinct()
                .collect(toList());
        if (addressList.size() != permission.getKeysList().size()) {
            throw new ContractValidateException(
                    "address should be distinct in permission " + permission.getType());
        }
        for (Key key : permission.getKeysList()) {
            if (!Wallet.addressValid(key.getAddress().toByteArray())) {
                throw new ContractValidateException("key is not a validate address");
            }
            if (key.getWeight() <= 0) {
                throw new ContractValidateException("key's weight should be greater than 0");
            }
            try {
                weightSum = Math.addExact(weightSum, key.getWeight());
            } catch (ArithmeticException e) {
                throw new ContractValidateException(e.getMessage());
            }
        }
        if (weightSum < permission.getThreshold()) {
            throw new ContractValidateException(
                    "sum of all key's weight should not be less than threshold in permission " + permission
                            .getType());
        }

        ByteString operations = permission.getOperations();
        if (permission.getType() != PermissionType.Active) {
            if (!operations.isEmpty()) {
                throw new ContractValidateException(
                        permission.getType() + " permission needn't operations");
            }
            return true;
        }
        //check operations
        if (operations.isEmpty() || operations.size() != 32) {
            throw new ContractValidateException("operations size must 32");
        }

        byte[] types1 = dbManager.getDynamicPropertiesStore().getAvailableContractType();
        for (int i = 0; i < 256; i++) {
            boolean b = (operations.byteAt(i / 8) & (1 << (i % 8))) != 0;
            boolean t = ((types1[(i / 8)] & 0xff) & (1 << (i % 8))) != 0;
            if (b && !t) {
                throw new ContractValidateException(i + " isn't a validate ContractType");
            }
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
        if (this.dbManager.getDynamicPropertiesStore().getAllowMultiSign() != 1) {
            throw new ContractValidateException("multi sign is not allowed, "
                    + "need to be opened by the committee");
        }
        if (!this.contract.is(AccountPermissionUpdateContract.class)) {
            throw new ContractValidateException(
                    "contract type error,expected type [AccountPermissionUpdateContract],real type["
                            + contract
                            .getClass() + "]");
        }
        final AccountPermissionUpdateContract accountPermissionUpdateContract;
        try {
            accountPermissionUpdateContract = contract.unpack(AccountPermissionUpdateContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        byte[] ownerAddress = accountPermissionUpdateContract.getOwnerAddress().toByteArray();
        if (!Wallet.addressValid(ownerAddress)) {
            throw new ContractValidateException("invalidate ownerAddress");
        }
        AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
        if (accountWrapper == null) {
            throw new ContractValidateException("ownerAddress account does not exist");
        }

        if (!accountPermissionUpdateContract.hasOwner()) {
            throw new ContractValidateException("owner permission is missed");
        }

        if (accountWrapper.getIsWitness()) {
            if (!accountPermissionUpdateContract.hasWitness()) {
                throw new ContractValidateException("witness permission is missed");
            }
        } else {
            if (accountPermissionUpdateContract.hasWitness()) {
                throw new ContractValidateException("account isn't witness can't set witness permission");
            }
        }

        if (accountPermissionUpdateContract.getActivesCount() == 0) {
            throw new ContractValidateException("active permission is missed");
        }
        if (accountPermissionUpdateContract.getActivesCount() > 8) {
            throw new ContractValidateException("active permission is too many");
        }

        Permission owner = accountPermissionUpdateContract.getOwner();
        Permission witness = accountPermissionUpdateContract.getWitness();
        List<Permission> actives = accountPermissionUpdateContract.getActivesList();

        if (owner.getType() != PermissionType.Owner) {
            throw new ContractValidateException("owner permission type is error");
        }
        if (!checkPermission(owner)) {
            return false;
        }
        if (accountWrapper.getIsWitness()) {
            if (witness.getType() != PermissionType.Witness) {
                throw new ContractValidateException("witness permission type is error");
            }
            if (!checkPermission(witness)) {
                return false;
            }
        }
        for (Permission permission : actives) {
            if (permission.getType() != PermissionType.Active) {
                throw new ContractValidateException("active permission type is error");
            }
            if (!checkPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return contract.unpack(AccountPermissionUpdateContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return dbManager.getDynamicPropertiesStore().getUpdateAccountPermissionFee();
    }
}
