package org.gsc.core;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.core.wrapper.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.BandwidthProcessor;
import org.gsc.db.Manager;
import org.gsc.db.TransactionTrace;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.AccountType;

@Slf4j
public class BandwidthProcessorTest {

  private static Manager dbManager;
  private static final String dbPath = "bandwidth_test";
  private static GSCApplicationContext context;
  private static final String ASSET_NAME;
  private static final String OWNER_ADDRESS;
  private static final String ASSET_ADDRESS;
  private static final String TO_ADDRESS;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    ASSET_NAME = "test_token";
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    ASSET_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }

  @Before
  public void createCapsule() {
    AccountWrapper ownerCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            0L);
    ownerCapsule.addAsset(ASSET_NAME.getBytes(), 100L);

    AccountWrapper toAccountWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            0L);

    AccountWrapper assetCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8("asset"),
            ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)),
            AccountType.AssetIssue,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());

    dbManager.getAccountStore().reset();
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountWrapper.getAddress().toByteArray(), toAccountWrapper);
    dbManager.getAccountStore().put(assetCapsule.getAddress().toByteArray(), assetCapsule);

    dbManager
        .getAssetIssueStore()
        .put(
            ByteArray.fromString(ASSET_NAME),
            new AssetIssueWrapper(getAssetIssueContract()));

  }

  private TransferAssetContract getTransferAssetContract() {
    return Contract.TransferAssetContract.newBuilder()
        .setAssetName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        .setAmount(100L)
        .build();
  }

  private AssetIssueContract getAssetIssueContract() {
    return Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ASSET_ADDRESS)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME))
        .setFreeAssetNetLimit(1000L)
        .setPublicFreeAssetNetLimit(1000L)
        .build();
  }


  //@Test
  public void testCreateNewAccount() throws Exception {
    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    TransferAssetContract transferAssetContract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(transferAssetContract);

    String NOT_EXISTS_ADDRESS =
        Wallet.getAddressPreFixString() + "008794500882809695a8a687866e76d4271a1abc";
    transferAssetContract = transferAssetContract.toBuilder()
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(NOT_EXISTS_ADDRESS))).build();

    org.gsc.protos.Protocol.Transaction.Contract contract = org.gsc.protos.Protocol.Transaction.Contract
        .newBuilder()
        .setType(Protocol.Transaction.Contract.ContractType.TransferAssetContract).setParameter(
            Any.pack(transferAssetContract)).build();

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only owner has frozen balance

    AccountWrapper ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setFrozen(10_000_000L, 0L);

    Assert.assertEquals(true, processor.contractCreateNewAccount(contract));
    long bytes = trx.getSerializedSize();
    processor.consumeBandwidthForCreateNewAccount(ownerCapsule, bytes, 1526647838000L);

    AccountWrapper ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    Assert.assertEquals(122L, ownerCapsuleNew.getNetUsage());

  }

  @Test
  public void testFree() throws Exception {

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    TransferAssetContract contract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(contract);

    AccountWrapper ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);

    dbManager.consumeBandwidth(trx, ret, trace);

    AccountWrapper ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM()
            ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getFreeNetUsage());
    Assert.assertEquals(508882612L, ownerCapsuleNew.getLatestConsumeFreeTime());//slot
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        dbManager.getDynamicPropertiesStore().getPublicNetUsage());
    Assert.assertEquals(508882612L, dbManager.getDynamicPropertiesStore().getPublicNetTime());
    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, ret, trace);
    ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(61L + 122 + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeNetUsage());
    Assert.assertEquals(508897012L,
        ownerCapsuleNew.getLatestConsumeFreeTime()); // 508882612L + 28800L/2
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(61L + 122L + (dbManager.getDynamicPropertiesStore().supportVM() ?
            Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        dbManager.getDynamicPropertiesStore().getPublicNetUsage());
    Assert.assertEquals(508897012L, dbManager.getDynamicPropertiesStore().getPublicNetTime());
    Assert.assertEquals(0L, ret.getFee());
  }

  @Test
  public void testConsumeAssetAccount() throws Exception {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only assetAccount has frozen balance

    TransferAssetContract contract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(contract);

    AccountWrapper ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountWrapper assetCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));
    assetCapsule.setFrozen(10_000_000L, 0L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(assetCapsule.getAddress().toByteArray(), assetCapsule);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeBandwidth(trx, ret, trace);

    AccountWrapper ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountWrapper assetCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        assetCapsuleNew.getNetUsage());
    Assert.assertEquals(508882612L, assetCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508882612L, ownerCapsuleNew.getLatestAssetOperationTime(ASSET_NAME));
    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getFreeAssetNetUsage(ASSET_NAME));
    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, ret, trace);

    ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    assetCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(61L + 122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        assetCapsuleNew.getNetUsage());
    Assert.assertEquals(508897012L, assetCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508897012L, ownerCapsuleNew.getLatestAssetOperationTime(ASSET_NAME));
    Assert.assertEquals(61L + 122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getFreeAssetNetUsage(ASSET_NAME));
    Assert.assertEquals(0L, ret.getFee());
  }

  @Test
  public void testConsumeOwner() throws Exception {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore()
        .saveTotalNetWeight(10_000_000L);//only owner has frozen balance

    TransferAssetContract contract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(contract);

    AccountWrapper ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setFrozen(10_000_000L, 0L);

    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeBandwidth(trx, ret, trace);

    AccountWrapper ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    AccountWrapper assetCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(ASSET_ADDRESS));

    Assert.assertEquals(122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX : 0),
        ownerCapsuleNew.getNetUsage());
    Assert.assertEquals(1526647838000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508882612L, ownerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(0L, ret.getFee());

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526691038000L); // + 12h

    dbManager.consumeBandwidth(trx, ret, trace);

    ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    Assert.assertEquals(61L + 122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX / 2 * 3 : 0),
        ownerCapsuleNew.getNetUsage());
    Assert.assertEquals(1526691038000L, ownerCapsuleNew.getLatestOperationTime());
    Assert.assertEquals(508897012L, ownerCapsuleNew.getLatestConsumeTime());
    Assert.assertEquals(0L, ret.getFee());
  }

  @Test
  public void testUsingFee() throws Exception {

    Args.getInstance().getGenesisBlock().getAssets().forEach(account -> {
      AccountWrapper capsule =
          new AccountWrapper(
              ByteString.copyFromUtf8(""),
              ByteString.copyFrom(account.getAddress()),
              AccountType.AssetIssue,
              100L);
      dbManager.getAccountStore().put(account.getAddress(), capsule);
    });

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore().saveFreeNetLimit(0L);

    TransferAssetContract contract = getTransferAssetContract();
    TransactionWrapper trx = new TransactionWrapper(contract);

    AccountWrapper ownerCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    ownerCapsule.setBalance(10_000_000L);

    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    TransactionTrace trace = new TransactionTrace(trx, dbManager);
    dbManager.consumeBandwidth(trx, ret, trace);

    AccountWrapper ownerCapsuleNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long transactionFee =
        (122L + (dbManager.getDynamicPropertiesStore().supportVM() ? Constant.MAX_RESULT_SIZE_IN_TX : 0)) * dbManager
            .getDynamicPropertiesStore().getTransactionFee();
    Assert.assertEquals(transactionFee,
        dbManager.getDynamicPropertiesStore().getTotalTransactionCost());
    Assert.assertEquals(
        10_000_000L - transactionFee,
        ownerCapsuleNew.getBalance());
    Assert.assertEquals(transactionFee, ret.getFee());

    dbManager.getAccountStore().delete(ByteArray.fromHexString(TO_ADDRESS));
    dbManager.consumeBandwidth(trx, ret, trace);

//    long createAccountFee = dbManager.getDynamicPropertiesStore().getCreateAccountFee();
//    ownerCapsuleNew = dbManager.getAccountStore()
//        .get(ByteArray.fromHexString(OWNER_ADDRESS));
//    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getCreateAccountFee(),
//        dbManager.getDynamicPropertiesStore().getTotalCreateAccountCost());
//    Assert.assertEquals(
//        10_000_000L - transactionFee - createAccountFee, ownerCapsuleNew.getBalance());
//    Assert.assertEquals(101220L, ret.getFee());
  }
}
