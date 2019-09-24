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

package org.gsc.net.peer.handler;

import java.util.List;

import com.google.protobuf.ByteString;
import org.gsc.core.wrapper.BlockWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.testng.collections.Lists;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.exception.P2pException;
import org.gsc.net.peer.message.BlockMessage;
import org.gsc.net.peer.Item;
import org.gsc.net.peer.PeerConnection;
import org.gsc.protos.Protocol.Inventory.InventoryType;
import org.gsc.protos.Protocol.Transaction;

public class BlockMsgHandlerTest {

  private BlockMsgHandler handler = new BlockMsgHandler();
  private PeerConnection peer = new PeerConnection();
  private BlockWrapper blockWrapper;
  private BlockMessage msg;

  @Test
  public void testProcessMessage() {
    try {
      blockWrapper = new BlockWrapper(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), ByteString.EMPTY, Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockWrapper);
      handler.processMessage(peer, new BlockMessage(blockWrapper));
    } catch (P2pException e) {
      Assert.assertTrue(e.getMessage().equals("no request"));
    }

    try {
      List<Transaction> transactionList = Lists.newArrayList();
      for (int i = 0; i < 1100000; i++) {
        transactionList.add(Transaction.newBuilder().build());
      }
      blockWrapper = new BlockWrapper(1, Sha256Hash.ZERO_HASH.getByteString(),
          System.currentTimeMillis() + 10000,1, ByteString.EMPTY,  transactionList);
      msg = new BlockMessage(blockWrapper);
      System.out.println("len = " + blockWrapper.getInstance().getSerializedSize());
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      System.out.println(e);
      Assert.assertTrue(e.getMessage().equals("block size over limit"));
    }

    try {
      blockWrapper = new BlockWrapper(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis() + 10000, ByteString.EMPTY, Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockWrapper);
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      System.out.println(e);
      Assert.assertTrue(e.getMessage().equals("block time error"));
    }
  }

}
