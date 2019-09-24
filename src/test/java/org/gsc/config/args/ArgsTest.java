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
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.gsc.utils.ByteArray;
import org.gsc.core.Constant;

@Slf4j
public class ArgsTest {

    @After
    public void destroy() {
        Args.clearParam();
    }

    @Test
    public void get() {
        Args.setParam(new String[]{"-w"}, Constant.TEST_NET_CONF);

        Args args = Args.getInstance();
        Assert.assertEquals("database", args.getStorage().getDbDirectory());

        Assert.assertEquals(3, args.getBootNodes().size());

        GenesisBlock genesisBlock = args.getGenesisBlock();

        Assert.assertEquals(4, genesisBlock.getAssets().size());

        Assert.assertEquals(3, genesisBlock.getWitnesses().size());

        Assert.assertEquals("1565913600000", genesisBlock.getTimestamp());

        Assert.assertEquals("0x0000000000000000000000000000000000000000000000000000000000000000",
                genesisBlock.getParentHash());

        System.out.println(args.getLocalWitnesses().getPrivateKeys());
        Assert.assertEquals(
                Lists.newArrayList("86721e81012ffc0ac54b0ffb6af4fe6a22ce5eebc89fefd1ca91d0d1f8ef85be"),
                args.getLocalWitnesses().getPrivateKeys());

        Assert.assertTrue(args.isNodeDiscoveryEnable());
        Assert.assertTrue(args.isNodeDiscoveryPersist());
        Assert.assertEquals("127.0.0.1", args.getNodeDiscoveryBindIp());
        // Assert.assertEquals("123.168.234.1", args.getNodeExternalIp());
        Assert.assertEquals(50505, args.getNodeListenPort());
        Assert.assertEquals(2000, args.getNodeConnectionTimeout());
        Assert.assertEquals(0, args.getActiveNodes().size());
        Assert.assertEquals(30, args.getNodeMaxActiveNodes());
        Assert.assertEquals(43, args.getNodeP2pVersion());
        //Assert.assertEquals(30, args.getSyncNodeCount());

        // gRPC network configs checking
        Assert.assertEquals(5021, args.getRpcPort());
        Assert.assertEquals(Integer.MAX_VALUE, args.getMaxConcurrentCallsPerConnection());
        Assert
                .assertEquals(NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW, args.getFlowControlWindow());
        Assert.assertEquals(60000L, args.getMaxConnectionIdleInMillis());
        Assert.assertEquals(Long.MAX_VALUE, args.getMaxConnectionAgeInMillis());
        Assert.assertEquals(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE, args.getMaxMessageSize());
        Assert.assertEquals(GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE, args.getMaxHeaderListSize());
        Assert.assertEquals(1L, args.getAllowCreationOfContracts());

        Assert.assertEquals("86721e81012ffc0ac54b0ffb6af4fe6a22ce5eebc89fefd1ca91d0d1f8ef85be",
                args.getLocalWitnesses().getPrivateKey());
        Assert.assertEquals("01f80c63ab67ead97e4f48de58f76e3d32a72d5b774251",
                ByteArray.toHexString(args.getLocalWitnesses().getWitnessAccountAddress()));


    }
}
