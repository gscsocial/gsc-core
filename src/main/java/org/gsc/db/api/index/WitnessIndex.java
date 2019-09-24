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

package org.gsc.db.api.index;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.disk.DiskIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.WitnessWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.gsc.utils.ByteArray;
import org.gsc.db.common.WrappedByteArray;
import org.gsc.db.db2.core.IGSCChainBase;
import org.gsc.protos.Protocol.Witness;

import javax.annotation.PostConstruct;

import static com.googlecode.cqengine.query.QueryFactory.attribute;

@Component
@Slf4j(topic = "DB")
public class WitnessIndex extends AbstractIndex<WitnessWrapper, Witness> {

    public static SimpleAttribute<WrappedByteArray, String> Witness_ADDRESS;
    public static Attribute<WrappedByteArray, String> PUBLIC_KEY;
    public static Attribute<WrappedByteArray, String> Witness_URL;

    @Autowired
    public WitnessIndex(
            @Qualifier("witnessStore") final IGSCChainBase<WitnessWrapper> database) {
        super(database);
    }

    @PostConstruct
    public void init() {
        initIndex(DiskPersistence.onPrimaryKeyInFile(Witness_ADDRESS, indexPath));
//    index.addIndex(DiskIndex.onAttribute(Witness_ADDRESS));
        index.addIndex(DiskIndex.onAttribute(PUBLIC_KEY));
        index.addIndex(DiskIndex.onAttribute(Witness_URL));
    }

    @Override
    public void setAttribute() {
        Witness_ADDRESS =
                attribute("witness address",
                        bytes -> ByteArray.toHexString(getObject(bytes).getAddress().toByteArray()));
        PUBLIC_KEY =
                attribute("public key",
                        bytes -> ByteArray.toHexString(getObject(bytes).getPubKey().toByteArray()));
        Witness_URL =
                attribute("witness url", bytes -> getObject(bytes).getUrl());

    }

}
