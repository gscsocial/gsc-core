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

package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.args.Args;
import org.gsc.core.exception.BadItemException;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
public class BlockWrapperTest {

  private static BlockWrapper blockWrapper0 = new BlockWrapper(1,
      Sha256Hash.wrap(ByteString
          .copyFrom(ByteArray
              .fromHexString("9938a342238077182498b464ac0292229938a342238077182498b464ac029222"))),
      1234,ByteString.EMPTY,
      ByteString.copyFrom("1234567".getBytes()));
  private static String dbPath = "db_bloackwrapper_test";

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath},
        Constant.TEST_NET_CONF);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testCalcMerkleRoot() throws Exception {
    blockWrapper0.setMerkleRoot();
    Assert.assertEquals(
        Sha256Hash.wrap(Sha256Hash.ZERO_HASH.getByteString()).toString(),
        blockWrapper0.getMerkleRoot().toString());

    logger.info("Transaction[X] Merkle Root : {}", blockWrapper0.getMerkleRoot().toString());

    TransferContract transferContract1 = TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();

    TransferContract transferContract2 = TransferContract.newBuilder()
        .setAmount(2L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "ED738B3A0FE390EAA71B768B6D02CDBD18FB207B"))))
        .build();

    blockWrapper0
        .addTransaction(new TransactionWrapper(transferContract1, ContractType.TransferContract));
    blockWrapper0
        .addTransaction(new TransactionWrapper(transferContract2, ContractType.TransferContract));
    blockWrapper0.setMerkleRoot();

    if (Arrays.equals(Constant.ADD_PRE_FIX_BYTE, Wallet.getAddressPreFixByte())) {
      Assert.assertEquals(
          "398c2c57fa9b4a4a1b5a43fb4483b994485542af561f7d51935ec7ef4c1efcb7",
          blockWrapper0.getMerkleRoot().toString());
    } else {
      Assert.assertEquals(
          "398c2c57fa9b4a4a1b5a43fb4483b994485542af561f7d51935ec7ef4c1efcb7",
          blockWrapper0.getMerkleRoot().toString());
    }

    logger.info("Transaction[O] Merkle Root : {}", blockWrapper0.getMerkleRoot().toString());
  }

  @Test
  public void testGetData() {
    blockWrapper0.getData();
    byte[] b = blockWrapper0.getData();
    BlockWrapper blockWrapper1 = null;
    try {
      blockWrapper1 = new BlockWrapper(b);
      Assert.assertEquals(blockWrapper0.getBlockId(), blockWrapper1.getBlockId());
    } catch (BadItemException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testValidate() {

  }

  @Test
  public void testGetInsHash() {
    Assert.assertEquals(1,
        blockWrapper0.getInstance().getBlockHeader().getRawData().getNumber());
    Assert.assertEquals(blockWrapper0.getParentHash(),
        Sha256Hash.wrap(blockWrapper0.getParentHashStr()));
  }

  @Test
  public void testGetTimeStamp() {
    Assert.assertEquals(1234L, blockWrapper0.getTimeStamp());
  }

}