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

package org.gsc.net.services;

import com.google.protobuf.ByteString;
import org.gsc.core.wrapper.BlockWrapper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.gsc.utils.Sha256Hash;
import org.gsc.net.peer.message.BlockMessage;
import org.gsc.net.peer.Item;
import org.gsc.net.service.AdvService;
import org.gsc.protos.Protocol.Inventory.InventoryType;

@Ignore
public class AdvServiceTest {

  AdvService service = new AdvService();

  @Test
  public void testAddInv() {
    boolean flag;
    Item item = new Item(Sha256Hash.ZERO_HASH, InventoryType.BLOCK);
    flag = service.addInv(item);
    Assert.assertTrue(flag);
    flag = service.addInv(item);
    Assert.assertTrue(!flag);
  }

  @Test
  public void testBroadcast() {
    BlockWrapper blockWrapper = new BlockWrapper(1, Sha256Hash.ZERO_HASH,
        System.currentTimeMillis(), ByteString.EMPTY, Sha256Hash.ZERO_HASH.getByteString());
    BlockMessage msg = new BlockMessage(blockWrapper);
    service.broadcast(msg);
    Item item = new Item(blockWrapper.getBlockId(), InventoryType.BLOCK);
    Assert.assertTrue(service.getMessage(item) != null);
  }
}
