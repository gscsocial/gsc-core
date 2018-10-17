package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.common.utils.Sha256Hash;
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
      1234,
      ByteString.copyFrom("1234567".getBytes()));
  private static String dbPath = "output_bloackcapsule_test";

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath},
        Constant.TEST_CONF);
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

    if (Constant.ADD_PRE_FIX_BYTE_TESTNET == Wallet.getAddressPreFixByte()) {
      Assert.assertEquals(
          "53421c1f1bcbbba67a4184cc3dbc1a59f90af7e2b0644dcfc8dc738fe30deffc",
          blockWrapper0.getMerkleRoot().toString());
    } else {
      Assert.assertEquals(
          "5bc862243292e6aa1d5e21a60bb6a673e4c2544709f6363d4a2f85ec29bcfe00",
          blockWrapper0.getMerkleRoot().toString());
    }

    logger.info("Transaction[O] Merkle Root : {}", blockWrapper0.getMerkleRoot().toString());
  }

  /* @Test
  public void testAddTransaction() {
    TransactionWrapper transactionCapsule = new TransactionWrapper("123", 1L);
    blockWrapper0.addTransaction(transactionCapsule);
    Assert.assertArrayEquals(blockWrapper0.getTransactions().get(0).getHash().getBytes(),
        transactionCapsule.getHash().getBytes());
    Assert.assertEquals(transactionCapsule.getInstance().getRawData().getVout(0).getValue(),
        blockWrapper0.getTransactions().get(0).getInstance().getRawData().getVout(0).getValue());
  } */

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