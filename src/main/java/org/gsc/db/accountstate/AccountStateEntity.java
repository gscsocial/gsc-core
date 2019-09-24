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

package org.gsc.db.accountstate;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;

@Slf4j(topic = "AccountState")
public class AccountStateEntity {

    private Account account;

    public AccountStateEntity() {
    }

    public AccountStateEntity(Account account) {
        Account.Builder builder = Account.newBuilder();
        builder.setAddress(account.getAddress());
        builder.setBalance(account.getBalance());
        //builder.putAllAssetV2(account.getAssetV2Map());
        builder.setAllowance(account.getAllowance());
        this.account = builder.build();
    }

    public Account getAccount() {
        return account;
    }

    public AccountStateEntity setAccount(Account account) {
        this.account = account;
        return this;
    }

    public byte[] toByteArrays() {
        return account.toByteArray();
    }

    public static AccountStateEntity parse(byte[] data) {
        try {
            return new AccountStateEntity().setAccount(Account.parseFrom(data));
        } catch (Exception e) {
            logger.error("parse to AccountStateEntity error! reason: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String toString() {
        return "address:" + Wallet.encode58Check(account.getAddress().toByteArray()) + "; " + account
                .toString();
    }
}
