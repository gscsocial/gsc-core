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

package org.gsc.core;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TxInputWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.gsc.utils.ByteArray;

@Slf4j
public class TxInputWrapperTest {

  @Test
  public void testTxOutputWrapper() {
    byte[] txId = ByteArray
        .fromHexString("2c0937534dd1b3832d05d865e8e6f2bf23218300b33a992740d45ccab7d4f519");
    long vout = 12345L;
    byte[] signature = ByteArray
        .fromHexString("ded9c2181fd7ea468a7a7b1475defe90bb0fc0ca8d0f2096b0617465cea6568c");
    byte[] pubkey = ByteArray
        .fromHexString("a0c9d5524c055381fe8b1950e0c3b09d252add57a7aec061ae258aa03ee25822");

    TxInputWrapper txInputWrapper = new TxInputWrapper(txId, vout, signature, pubkey);

    Assert
        .assertArrayEquals(txId, txInputWrapper.getTxInput().getRawData().getTxID().toByteArray());
    Assert.assertEquals(vout, txInputWrapper.getTxInput().getRawData().getVout());
    Assert.assertArrayEquals(signature, txInputWrapper.getTxInput().getSignature().toByteArray());
    Assert.assertArrayEquals(pubkey,
        txInputWrapper.getTxInput().getRawData().getPubKey().toByteArray());
    Assert.assertTrue(txInputWrapper.validate());
  }
}

