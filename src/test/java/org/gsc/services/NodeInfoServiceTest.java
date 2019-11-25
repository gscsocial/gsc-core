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

package org.gsc.services;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BlockWrapper;
import org.junit.Assert;
import org.gsc.api.GrpcAPI.EmptyMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletGrpc.WalletBlockingStub;
import org.gsc.application.GSCApplicationContext;
import org.gsc.net.node.NodeInfo;
import org.gsc.utils.Sha256Hash;
import org.gsc.Version;
import org.gsc.wallet.common.client.Configuration;

@Slf4j
public class NodeInfoServiceTest {

  private NodeInfoService nodeInfoService;
  private WitnessProductBlockService witnessProductBlockService;

  public NodeInfoServiceTest(GSCApplicationContext context) {
    nodeInfoService = context.getBean("nodeInfoService", NodeInfoService.class);
    witnessProductBlockService = context.getBean(WitnessProductBlockService.class);
  }

  public void test() {
    BlockWrapper blockWrapper1 = new BlockWrapper(1, Sha256Hash.ZERO_HASH,
        100, ByteString.EMPTY, ByteString.EMPTY);
    BlockWrapper blockWrapper2 = new BlockWrapper(1, Sha256Hash.ZERO_HASH,
        200, ByteString.EMPTY, ByteString.EMPTY);
    witnessProductBlockService.validWitnessProductTwoBlock(blockWrapper2);
    witnessProductBlockService.validWitnessProductTwoBlock(blockWrapper1);
    
    NodeInfo nodeInfo = nodeInfoService.getNodeInfo();
    Assert.assertEquals(nodeInfo.getConfigNodeInfo().getCodeVersion(), Version.getVersion());
    Assert.assertEquals(nodeInfo.getCheatWitnessInfoMap().size(), 1);
    logger.info("node info: {}", JSON.toJSONString(nodeInfo));
  }

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  public void testGrpc() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
            .usePlaintext(true)
            .build());
    logger.info("getNodeInfo: {}", walletStub.getNodeInfo(EmptyMessage.getDefaultInstance()));
  }

}
