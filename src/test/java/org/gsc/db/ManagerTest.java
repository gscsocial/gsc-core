package org.gsc.db;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.gsc.crypto.ECKey;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.utils.Utils;
import org.gsc.core.Constant;
import org.gsc.core.wrapper.TransactionWrapper;
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
import org.gsc.core.exception.ReceiptException;
import org.gsc.core.exception.TaposException;
import org.gsc.core.exception.TooBigTransactionException;
import org.gsc.core.exception.TooBigTransactionResultException;
import org.gsc.core.exception.TransactionExpirationException;
import org.gsc.core.exception.TransactionTraceException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.core.exception.UnsupportVMException;
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
  private static String dbPath = "output_manager_test";

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);

    blockWrapper2 =
        new BlockWrapper(
            1,
            Sha256Hash.wrap(ByteString.copyFrom(
                ByteArray.fromHexString(
                    "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
            0,
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
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @Test
  public void setBlockReference()
      throws ContractExeException, UnLinkedBlockException, ValidateScheduleException, BadBlockException,
      ContractValidateException, ValidateSignatureException, BadItemException, ItemNotFoundException, AccountResourceInsufficientException, TransactionExpirationException, TooBigTransactionException, DupTransactionException, TaposException, BadNumberBlockException, NonCommonBlockException, ReceiptException, TransactionTraceException, ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {

    BlockWrapper blockWrapper =
        new BlockWrapper(
            1,
            Sha256Hash.wrap(dbManager.getGenesisBlockId().getByteString()),
            1,
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

//    Assert.assertTrue(
//        "containBlock is error",
//        dbManager.containBlock(
//            Sha256Hash.wrap(ByteArray.fromHexString(blockWrapper2.getBlockId().toString()))));

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

  //    @Test
  public void updateWits() {
    int sizePrv = dbManager.getWitnesses().size();
    dbManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}", ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    logger.info("------------");
    WitnessWrapper witnessCapsulef =
        new WitnessWrapper(
            ByteString.copyFrom(ByteArray.fromHexString("0x0011")), "www.gsc.net/first");
    witnessCapsulef.setIsJobs(true);
    WitnessWrapper witnessCapsules =
        new WitnessWrapper(
            ByteString.copyFrom(ByteArray.fromHexString("0x0012")), "www.gsc.net/second");
    witnessCapsules.setIsJobs(true);
    WitnessWrapper witnessCapsulet =
        new WitnessWrapper(
            ByteString.copyFrom(ByteArray.fromHexString("0x0013")), "www.gsc.net/three");
    witnessCapsulet.setIsJobs(false);

    dbManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}", ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    logger.info("---------");
    dbManager.getWitnessStore().put(witnessCapsulef.getAddress().toByteArray(), witnessCapsulef);
    dbManager.getWitnessStore().put(witnessCapsules.getAddress().toByteArray(), witnessCapsules);
    dbManager.getWitnessStore().put(witnessCapsulet.getAddress().toByteArray(), witnessCapsulet);
    dbManager.getWitnessController().initWits();
    dbManager
        .getWitnesses()
        .forEach(
            witnessAddress -> {
              logger.info(
                  "witness address is {}", ByteArray.toHexString(witnessAddress.toByteArray()));
            });
    int sizeTis = dbManager.getWitnesses().size();
    Assert.assertEquals("update add witness size is ", 2, sizeTis - sizePrv);
  }

  @Test
  public void fork()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException, ReceiptException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      BadBlockException, TaposException, BadNumberBlockException, NonCommonBlockException,
      TransactionTraceException, ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessWrapper witnessCapsule = new WitnessWrapper(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    dbManager.generateBlock(witnessCapsule, 1533529947843L, privateKey);

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockWrapper blockWrapper0 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    BlockWrapper blockWrapper1 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    dbManager.pushBlock(blockWrapper0);
    dbManager.pushBlock(blockWrapper1);

    BlockWrapper blockWrapper2 =
        createTestBlockCapsule(
            1533529947843L + 6000,
            num + 2, blockWrapper1.getBlockId().getByteString(), addressToProvateKeys);

    dbManager.pushBlock(blockWrapper2);

    Assert.assertNotNull(dbManager.getBlockStore().get(blockWrapper1.getBlockId().getBytes()));
    Assert.assertNotNull(dbManager.getBlockStore().get(blockWrapper2.getBlockId().getBytes()));

    Assert.assertEquals(
        dbManager.getBlockStore().get(blockWrapper2.getBlockId().getBytes()).getParentHash(),
        blockWrapper1.getBlockId());

    Assert.assertEquals(dbManager.getBlockStore().size(), size + 3);

    Assert.assertEquals(
        dbManager.getBlockIdByNum(dbManager.getHead().getNum() - 1), blockWrapper1.getBlockId());
    Assert.assertEquals(
        dbManager.getBlockIdByNum(dbManager.getHead().getNum() - 2), blockWrapper1.getParentHash());

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
      UnLinkedBlockException, ValidateScheduleException, BadItemException, ReceiptException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException,
      DupTransactionException, BadBlockException,
      TaposException, BadNumberBlockException, NonCommonBlockException, TransactionTraceException,
      ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessWrapper witnessCapsule = new WitnessWrapper(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    dbManager.generateBlock(witnessCapsule, 1533529947843L, privateKey);

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockWrapper blockWrapper0 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    BlockWrapper blockWrapper1 =
        createTestBlockCapsule(
            1533529947843L + 3001,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    logger.info("******block0:" + blockWrapper0);
    logger.info("******block1:" + blockWrapper1);

    dbManager.pushBlock(blockWrapper0);
    dbManager.pushBlock(blockWrapper1);
    context.getBean(KhaosDatabase.class).removeBlk(dbManager.getBlockIdByNum(num));
    Exception exception = null;

    BlockWrapper blockWrapper2 =
        createTestBlockCapsule(
            1533529947843L + 6000,
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
        createTestBlockCapsule(1533529947843L + 9000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);
    logger.info("******block3:" + blockWrapper3);
    dbManager.pushBlock(blockWrapper3);

    Assert.assertEquals(blockWrapper3.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockWrapper3.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes())
            .getBlockId());

    BlockWrapper blockWrapper4 =
        createTestBlockCapsule(1533529947843L + 12000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockWrapper3.getBlockId().getByteString(), addressToProvateKeys);
    logger.info("******block4:" + blockWrapper4);
    dbManager.pushBlock(blockWrapper4);

    Assert.assertEquals(blockWrapper4.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockWrapper4.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes())
            .getBlockId());
  }

  @Test
  public void switchBack()
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, BadItemException, ReceiptException,
      ItemNotFoundException, HeaderNotFound, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      BadBlockException, TaposException, BadNumberBlockException, NonCommonBlockException,
      TransactionTraceException, ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {
    Args.setParam(new String[]{"--witness"}, Constant.TEST_CONF);
    long size = dbManager.getBlockStore().size();
    System.out.print("block store size:" + size + "\n");
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessWrapper witnessCapsule = new WitnessWrapper(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));
    dbManager.generateBlock(witnessCapsule, 1533529947843L, privateKey);

    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();

    long num = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    BlockWrapper blockWrapper0 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    BlockWrapper blockWrapper1 =
        createTestBlockCapsule(
            1533529947843L + 3000,
            num + 1,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            addressToProvateKeys);

    dbManager.pushBlock(blockWrapper0);
    dbManager.pushBlock(blockWrapper1);
    try {
      BlockWrapper blockWrapper2 =
          createTestBlockCapsuleError(
              1533529947843L + 6000,
              num + 2, blockWrapper1.getBlockId().getByteString(), addressToProvateKeys);

      dbManager.pushBlock(blockWrapper2);
    } catch (ValidateScheduleException e) {
      logger.info("the fork chain has error block");
    }

    Assert.assertNotNull(dbManager.getBlockStore().get(blockWrapper0.getBlockId().getBytes()));
    Assert.assertEquals(blockWrapper0.getBlockId(),
        dbManager.getBlockStore().get(blockWrapper0.getBlockId().getBytes()).getBlockId());

    BlockWrapper blockWrapper3 =
        createTestBlockCapsule(
            1533529947843L + 9000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockWrapper0.getBlockId().getByteString(), addressToProvateKeys);
    dbManager.pushBlock(blockWrapper3);

    Assert.assertEquals(blockWrapper3.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockWrapper3.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes())
            .getBlockId());

    BlockWrapper blockWrapper4 =
        createTestBlockCapsule(
            1533529947843L + 12000,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            blockWrapper3.getBlockId().getByteString(), addressToProvateKeys);
    dbManager.pushBlock(blockWrapper4);

    Assert.assertEquals(blockWrapper4.getBlockId(),
        dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash());
    Assert.assertEquals(blockWrapper4.getBlockId(),
        dbManager.getBlockStore()
            .get(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes())
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

              WitnessWrapper witnessCapsule = new WitnessWrapper(address);
              dbManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
              dbManager.getWitnessController().addWitness(address);

              AccountWrapper accountWrapper =
                  new AccountWrapper(Account.newBuilder().setAddress(address).build());
              dbManager.getAccountStore().put(address.toByteArray(), accountWrapper);

              return Maps.immutableEntry(address, privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private BlockWrapper createTestBlockCapsule(
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    long time = System.currentTimeMillis();
    return createTestBlockCapsule(time, number, hash, addressToProvateKeys);
  }

  private BlockWrapper createTestBlockCapsule(long time,
                                              long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    WitnessController witnessController = dbManager.getWitnessController();
    ByteString witnessAddress =
        witnessController.getScheduledWitness(witnessController.getSlotAtTime(time));
    BlockWrapper blockWrapper = new BlockWrapper(number, Sha256Hash.wrap(hash), time,
        witnessAddress);
    blockWrapper.generatedByMyself = true;
    blockWrapper.setMerkleRoot();
    blockWrapper.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockWrapper;
  }

  private BlockWrapper createTestBlockCapsuleError(
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    long time = System.currentTimeMillis();
    return createTestBlockCapsuleError(time, number, hash, addressToProvateKeys);
  }

  private BlockWrapper createTestBlockCapsuleError(long time,
                                                   long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    WitnessController witnessController = dbManager.getWitnessController();
    ByteString witnessAddress =
        witnessController.getScheduledWitness(witnessController.getSlotAtTime(time));
    BlockWrapper blockWrapper = new BlockWrapper(number, Sha256Hash.wrap(hash), time,
        ByteString.copyFromUtf8("onlyTest"));
    blockWrapper.generatedByMyself = true;
    blockWrapper.setMerkleRoot();
    blockWrapper.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockWrapper;
  }
}
