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

package org.gsc.db;

import com.typesafe.config.ConfigObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.core.wrapper.AccountWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.gsc.core.Wallet;
import org.gsc.db.accountstate.callback.AccountStateCallBack;
import org.gsc.db.accountstate.storetrie.AccountStateStoreTrie;

@Slf4j(topic = "DB")
@Component
public class AccountStore extends GSCStoreWithRevoking<AccountWrapper> {

    private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

    @Autowired
    private AccountStateCallBack accountStateCallBack;

    @Autowired
    private AccountStateStoreTrie accountStateStoreTrie;

    @Autowired
    private AccountStore(@Value("account") String dbName) {
        super(dbName);
    }

    @Override
    public AccountWrapper get(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        return ArrayUtils.isEmpty(value) ? null : new AccountWrapper(value);
    }


    @Override
    public void put(byte[] key, AccountWrapper item) {
        super.put(key, item);
        accountStateCallBack.accountCallBack(key, item);
    }

    /**
     * Max Dot account.
     */
    public AccountWrapper getDot() {
        return getUnchecked(assertsAddress.get("Dot"));
    }

    /**
     * Min Dot account.
     */
    public AccountWrapper getBlackhole() {
        return getUnchecked(assertsAddress.get("Blackhole"));
    }

    public static void setAccount(com.typesafe.config.Config config) {
        List list = config.getObjectList("genesis.block.assets");
        for (int i = 0; i < list.size(); i++) {
            ConfigObject obj = (ConfigObject) list.get(i);
            String accountName = obj.get("accountName").unwrapped().toString();
            byte[] address = Wallet.decodeFromBase58Check(obj.get("address").unwrapped().toString());
            assertsAddress.put(accountName, address);
        }
    }

    @Override
    public void close() {
        super.close();
        accountStateStoreTrie.close();
    }
}
