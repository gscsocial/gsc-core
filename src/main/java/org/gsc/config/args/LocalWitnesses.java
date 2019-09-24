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

package org.gsc.config.args;

import com.google.common.collect.Lists;

import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.config.Parameter.ChainConstant;

@Slf4j(topic = "app")
public class LocalWitnesses {

    @Getter
    private List<String> privateKeys = Lists.newArrayList();

    private byte[] witnessAccountAddress;

    public LocalWitnesses() {
    }

    public LocalWitnesses(String privateKey) {
        addPrivateKeys(privateKey);
    }

    public LocalWitnesses(List<String> privateKeys) {
        setPrivateKeys(privateKeys);
    }

    public void setWitnessAccountAddress(final byte[] localWitnessAccountAddress) {
        this.witnessAccountAddress = localWitnessAccountAddress;
    }

    public byte[] getWitnessAccountAddress() {
        if (witnessAccountAddress == null) {
            byte[] privateKey = ByteArray.fromHexString(getPrivateKey());
            final ECKey ecKey = ECKey.fromPrivate(privateKey);
            this.witnessAccountAddress = ecKey.getAddress();
        }
        return witnessAccountAddress;
    }

    public void initWitnessAccountAddress() {
        if (witnessAccountAddress == null) {
            byte[] privateKey = ByteArray.fromHexString(getPrivateKey());
            final ECKey ecKey = ECKey.fromPrivate(privateKey);
            this.witnessAccountAddress = ecKey.getAddress();
        }
    }

    /**
     * Private key of ECKey.
     */
    public void setPrivateKeys(final List<String> privateKeys) {
        if (CollectionUtils.isEmpty(privateKeys)) {
            return;
        }
        for (String privateKey : privateKeys) {
            validate(privateKey);
        }
        this.privateKeys = privateKeys;
    }

    private void validate(String privateKey) {
        if (StringUtils.startsWithIgnoreCase(privateKey, "0X")) {
            privateKey = privateKey.substring(2);
        }

        if (StringUtils.isNotBlank(privateKey)
                && privateKey.length() != ChainConstant.PRIVATE_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Private key(" + privateKey + ") must be " + ChainConstant.PRIVATE_KEY_LENGTH
                            + "-bits hex string.");
        }
    }

    public void addPrivateKeys(String privateKey) {
        validate(privateKey);
        this.privateKeys.add(privateKey);
    }

    //get the first one recently
    public String getPrivateKey() {
        if (CollectionUtils.isEmpty(privateKeys)) {
            logger.warn("privateKey is null");
            return null;
        }
        return privateKeys.get(0);
    }

}
