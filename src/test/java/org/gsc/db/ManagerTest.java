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

package org.gsc.db;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.utils.Sha256Hash;
import org.gsc.utils.Utils;
import org.gsc.core.Constant;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.BadBlockException;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.BadNumberBlockException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.DupTransactionException;
import org.gsc.core.exception.HeaderNotFound;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.core.exception.NonCommonBlockException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.TaposException;
import org.gsc.core.exception.TooBigTransactionException;
import org.gsc.core.exception.TooBigTransactionResultException;
import org.gsc.core.exception.TransactionExpirationException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.core.exception.ValidateScheduleException;
import org.gsc.core.exception.ValidateSignatureException;
import org.gsc.core.witness.WitnessController;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
public class ManagerTest {

  private static Manager dbManager;
  private static GSCApplicationContext context;
  private static BlockWrapper blockWrapper2;
  private static String dbPath = "db_manager_test";
  private static AtomicInteger port = new AtomicInteger(0);

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, Constant.TEST_NET_CONF);
    Args.getInstance().setNodeListenPort(10000 + port.incrementAndGet());
    context = new GSCApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);

    blockWrapper2 =
        new BlockWrapper(
            1,
            Sha256Hash.wrap(ByteString.copyFrom(
                ByteArray.fromHexString(
                    "27216755ffd8726184b428971834df395030578461e7b5b3967b68697ca9925c"))),
            0,ByteString.EMPTY,
                ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(
                        Args.getInstance().getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockWrapper2.setMerkleRoot();
    blockWrapper2.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void setBlockReference()
      throws ContractExeException, UnLinkedBlockException, ValidateScheduleException, BadBlockException,
      ContractValidateException, ValidateSignatureException, BadItemException, ItemNotFoundException, AccountResourceInsufficientException, TransactionExpirationException, TooBigTransactionException, DupTransactionException, TaposException, BadNumberBlockException, NonCommonBlockException, ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException {

    BlockWrapper blockWrapper =
        new BlockWrapper(
            1,
            Sha256Hash.wrap(dbManager.getGenesisBlockId().getByteString()),
            1,ByteString.EMPTY,
                ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(
                        Args.getInstance().getLocalWitnesses().getPrivateKey()))
                    .getAddress()));
    blockWrapper.setMerkleRoot();
    blockWrapper.sign(
        ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));

    TransferContract tc =
        TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionWrapper trx = new TransactionWrapper(tc, ContractType.TransferContract);
    if (dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 0) {
      dbManager.pushBlock(blockWrapper);
      Assert.assertEquals(1, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
      dbManager.setBlockReference(trx);
      Assert.assertEquals(1,
          ByteArray.toInt(trx.getInstance().getRawData().getRefBlockBytes().toByteArray()));
    }

    while (dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() > 0) {
      dbManager.eraseBlock();
    }

    dbManager.pushBlock(blockWrapper);
    Assert.assertEquals(1, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    dbManager.setBlockReference(trx);
    Assert.assertEquals(1,
        ByteArray.toInt(trx.getInstance().getRawData().getRefBlockBytes().toByteArray()));
  }

  @Test
  public void pushBlock() {
    boolean isUnlinked = false;
    try {
      dbManager.pushBlock(blockWrapper2);
    } catch (UnLinkedBlockException e) {
      isUnlinked = true;
    } catch (Exception e) {
      Assert.assertTrue("pushBlock is error", false);
    }

    if (isUnlinked) {
      Assert.assertEquals("getBlockIdByNum is error", dbManager.getHeadBlockNum(), 0);
    } else {
      try {
        Assert.assertEquals(
            "getBlockIdByNum is error",
            blockWrapper2.getBlockId().toString(),
            dbManager.getBlockIdByNum(1).toString());
      } catch (ItemNotFoundException e) {
        e.printStackTrace();
      }
    }

    Assert.assertTrue("hasBlocks is error", dbManager.hasBlocks());
  }

  public void updateWits() {
    int sizePrv = dbManager.getWitnesses().size();
    dbManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}",
                  ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    logger.info("------------");
    WitnessWrapper witnessWrapperf =
        new WitnessWrapper(
            ByteString.copyFrom(ByteArray.fromHexString("0x0011")), "www.gsc.net/first");
    witnessWrapperf.setIsJobs(true);
    WitnessWrapper witnessWrappers =
        new WitnessWrapper(
            ByteString.copyFrom(ByteArray.fromHexString("0x0012")), "www.gsc.net/second");
    witnessWrappers.setIsJobs(true);
    WitnessWrapper witnessWrappert =
        new WitnessWrapper(
            ByteString.copyFrom(ByteArray.fromHexString("0x0013")), "www.gsc.net/three");
    witnessWrappert.setIsJobs(false);

    dbManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}",
                  ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    logger.info("---------");
    dbManager.getWitnessStore().put(witnessWrapperf.getAddress().toByteArray(), witnessWrapperf);
    dbManager.getWitnessStore().put(witnessWrappers.getAddress().toByteArray(), witnessWrappers);
    dbManager.getWitnessStore().put(witnessWrappert.getAddress().toByteArray(), witnessWrappert);
    dbManager.getWitnessController().initWits();
    dbManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}",
                  ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    int sizeTis = dbManager.getWitnesses().size();
    Assert.assertEquals("update add witness size is ", 2, sizeTis - sizePrv);
  }

  @Test
  public void fork()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      BadBlockException, TaposException, BadNumberBlockException, NonCommonBlockException, ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_NET_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "27216755ffd8726184b428971834df395030578461e7b5b3967b68697ca9925c";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    dbManager.generateBlock(witnessWrapper, 1565913600000L, privateKey, false, false);

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockWrapper blockWrapper0 =
        createTestBlockWrapper(
                1565913600000L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    BlockWrapper blockWrapper1 =
        createTestBlockWrapper(
                1565913600000L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    dbManager.pushBlock(blockWrapper0);
    dbManager.pushBlock(blockWrapper1);

    BlockWrapper blockWrapper2 =
        createTestBlockWrapper(
                1565913600000L + 6000,
            num + 2, blockWrapper1.getBlockId().getByteString(), addressToProvateKeys);

    dbManager.pushBlock(blockWrapper2);

    Assert.assertNotNull(dbManager.getBlockStore().get(blockWrapper1.getBlockId().getBytes()));
    Assert.assertNotNull(dbManager.getBlockStore().get(blockWrapper2.getBlockId().getBytes()));

    Assert.assertEquals(
        dbManager.getBlockStore().get(blockWrapper2.getBlockId().getBytes()).getParentHash(),
        blockWrapper1.getBlockId());

    Assert.assertEquals(dbManager.getBlockStore().size(), size + 3);

    Assert.assertEquals(
        dbManager.getBlockIdByNum(dbManager.getHead().getNum() - 1),
        blockWrapper1.getBlockId());
    Assert.assertEquals(
        dbManager.getBlockIdByNum(dbManager.getHead().getNum() - 2),
        blockWrapper1.getParentHash());

    Assert.assertEquals(
        blockWrapper2.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(
        dbManager.getHead().getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
  }

  @Test
  public void doNotSwitch()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException,
      DupTransactionException, BadBlockException,
      TaposException, BadNumberBlockException, NonCommonBlockException,
      ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_NET_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "27216755ffd8726184b428971834df395030578461e7b5b3967b68697ca9925c";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    dbManager.generateBlock(witnessWrapper, 1565913600000L, privateKey, false, false);

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockWrapper blockWrapper0 =
        createTestBlockWrapper(
                1565913600000L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    BlockWrapper blockWrapper1 =
        createTestBlockWrapper(
                1565913600000L + 3001,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    logger.info("******block0:" + blockWrapper0);
    logger.info("******block1:" + blockWrapper1);

    dbManager.pushBlock(blockWrapper0);
    dbManager.pushBlock(blockWrapper1);
    context.getBean(KhaosDatabase.class).removeBlk(dbManager.getBlockIdByNum(num));
    Exception exception = null;

    BlockWrapper blockWrapper2 =
        createTestBlockWrapper(
                1565913600000L + 6000,
            num + 2, blockWrapper1.getBlockId().getByteString(), addressToProvateKeys);
    logger.info("******block2:" + blockWrapper2);
    try {
      dbManager.pushBlock(blockWrapper2);
    } catch (NonCommonBlockException e) {
      logger.info("do not switch fork");
      Assert.assertNotNull(dbManager.getBlockStore().get(blockWrapper0.getBlockId().getBytes()));
      Assert.assertEquals(blockWrapper0.getBlockId(),
          dbManager.getBlockStore().get(blockWrapper0.getBlockId().getBytes()).getBlockId());
      Assert.assertEquals(blockWrapper0.getBlockId(),
          dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
      exception = e;
    }

    if (exception == null) {
      throw new IllegalStateException();
    }

    BlockWrapper blockWrapper3 =
        createTestBlockWrapper(1565913600000L + 9000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);
    logger.info("******block3:" + blockWrapper3);
    dbManager.pushBlock(blockWrapper3);

    Assert.assertEquals(blockWrapper3.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockWrapper3.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getBytes())
            .getBlockId());

    BlockWrapper blockWrapper4 =
        createTestBlockWrapper(1565913600000L + 12000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockWrapper3.getBlockId().getByteString(), addressToProvateKeys);
    logger.info("******block4:" + blockWrapper4);
    dbManager.pushBlock(blockWrapper4);

    Assert.assertEquals(blockWrapper4.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockWrapper4.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getBytes())
            .getBlockId());
  }

  @Test
  public void testLastHeadBlockIsMaintenance()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      BadBlockException, TaposException, BadNumberBlockException, NonCommonBlockException,
      ReceiptCheckErrException, VMIllegalException,
      TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_NET_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "27216755ffd8726184b428971834df395030578461e7b5b3967b68697ca9925c";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    BlockWrapper blockWrapper =
        dbManager.generateBlock(witnessWrapper, 1565933600000L, privateKey, true, false);

    //has processed the first block of the maintenance period before starting the block
    dbManager.getWitnessStore().reset();
    dbManager.getDynamicPropertiesStore().saveStateFlag(0);
    blockWrapper = dbManager.generateBlock(witnessWrapper, 1565913600000L, privateKey, true, false);
    Assert.assertTrue(blockWrapper == null);
  }

  @Test
  public void switchBack()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      BadBlockException, TaposException, BadNumberBlockException, NonCommonBlockException, ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_NET_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "27216755ffd8726184b428971834df395030578461e7b5b3967b68697ca9925c";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    dbManager.generateBlock(witnessWrapper, 1565913600000L, privateKey, false, false);

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockWrapper blockWrapper0 =
        createTestBlockWrapper(
                1565913600000L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    BlockWrapper blockWrapper1 =
        createTestBlockWrapper(
                1565913600000L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getByteString(),
            addressToProvateKeys);

    dbManager.pushBlock(blockWrapper0);
    dbManager.pushBlock(blockWrapper1);
    try {
      BlockWrapper blockWrapper2 =
          createTestBlockWrapperError(
                  1565913600000L + 6000,
              num + 2, blockWrapper1.getBlockId().getByteString(), addressToProvateKeys);

      dbManager.pushBlock(blockWrapper2);
    } catch (ValidateScheduleException e) {
      logger.info("the fork chain has error block");
    }

    Assert.assertNotNull(dbManager.getBlockStore().get(blockWrapper0.getBlockId().getBytes()));
    Assert.assertEquals(blockWrapper0.getBlockId(),
        dbManager.getBlockStore().get(blockWrapper0.getBlockId().getBytes()).getBlockId());

    BlockWrapper blockWrapper3 =
        createTestBlockWrapper(
                1565913600000L + 9000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockWrapper0.getBlockId().getByteString(), addressToProvateKeys);
    dbManager.pushBlock(blockWrapper3);

    Assert.assertEquals(blockWrapper3.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockWrapper3.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getBytes())
            .getBlockId());

    BlockWrapper blockWrapper4 =
        createTestBlockWrapper(
                1565913600000L + 12000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockWrapper3.getBlockId().getByteString(), addressToProvateKeys);
    dbManager.pushBlock(blockWrapper4);

    Assert.assertEquals(blockWrapper4.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockWrapper4.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                .getBytes())
            .getBlockId());
  }

  private Map<ByteString, String> addTestWitnessAndAccount() {
    dbManager.getWitnesses().clear();
    return IntStream.range(0, 2)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessWrapper witnessWrapper = new WitnessWrapper(address);
              dbManager.getWitnessStore().put(address.toByteArray(), witnessWrapper);
              dbManager.getWitnessController().addWitness(address);

              AccountWrapper accountWrapper =
                  new AccountWrapper(Account.newBuilder().setAddress(address).build());
              dbManager.getAccountStore().put(address.toByteArray(), accountWrapper);

              return Maps.immutableEntry(address, privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private BlockWrapper createTestBlockWrapper(
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    long time = System.currentTimeMillis();
    return createTestBlockWrapper(time, number, hash, addressToProvateKeys);
  }

  private BlockWrapper createTestBlockWrapper(long time,
                                              long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    WitnessController witnessController = dbManager.getWitnessController();
    ByteString witnessAddress =
        witnessController.getScheduledWitness(witnessController.getSlotAtTime(time));
    BlockWrapper blockWrapper = new BlockWrapper(number, Sha256Hash.wrap(hash), time,ByteString.EMPTY,
            witnessAddress);
    blockWrapper.generatedByMyself = true;
    blockWrapper.setMerkleRoot();
    blockWrapper.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockWrapper;
  }

  private BlockWrapper createTestBlockWrapperError(
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    long time = System.currentTimeMillis();
    return createTestBlockWrapperError(time, number, hash, addressToProvateKeys);
  }

  private BlockWrapper createTestBlockWrapperError(long time,
                                                   long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    WitnessController witnessController = dbManager.getWitnessController();
    ByteString witnessAddress =
        witnessController.getScheduledWitness(witnessController.getSlotAtTime(time));
    BlockWrapper blockWrapper = new BlockWrapper(number, Sha256Hash.wrap(hash), time,ByteString.EMPTY,
            ByteString.copyFromUtf8("onlyTest"));
    blockWrapper.generatedByMyself = true;
    blockWrapper.setMerkleRoot();
    blockWrapper.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockWrapper;
  }
}
