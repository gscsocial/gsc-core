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
import org.gsc.core.wrapper.TxOutputWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.gsc.utils.ByteArray;

@Slf4j
public class TxOutputWrapperTest {

  @Test
  public void testTxOutputWrapper() {
    long value = 123456L;
    String address = "3450dde5007c67a50ec2e09489fa53ec1ff59c61e7ddea9638645e6e5f62e5f5";
    TxOutputWrapper txOutputWrapper = new TxOutputWrapper(value, address);

    Assert.assertEquals(value, txOutputWrapper.getTxOutput().getValue());
    Assert.assertEquals(address,
        ByteArray.toHexString(txOutputWrapper.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputWrapper.validate());

    long value3 = 9852448L;
    String address3 = "0xfd1a5decba973b0d31e84e7d8f4a5b10d33ab37ce6533f1ff5a9db2d9db8ef";
    String address4 = "fd1a5decba973b0d31e84e7d8f4a5b10d33ab37ce6533f1ff5a9db2d9db8ef";
    TxOutputWrapper txOutputWrapper2 = new TxOutputWrapper(value3, address3);

    Assert.assertEquals(value3, txOutputWrapper2.getTxOutput().getValue());
    Assert.assertEquals(address4,
        ByteArray.toHexString(txOutputWrapper2.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputWrapper2.validate());

    long value5 = 67549L;
    String address5 = null;
    TxOutputWrapper txOutputWrapper3 = new TxOutputWrapper(value5, address5);

    Assert.assertEquals(value5, txOutputWrapper3.getTxOutput().getValue());
    Assert.assertEquals("",
        ByteArray.toHexString(txOutputWrapper3.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputWrapper3.validate());

  }

}

