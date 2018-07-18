package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Account.Frozen;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UnfreezeAssetOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_unfreeze_asset_test";
  private static AnnotationConfigApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALIDATE = "aaaa";
  private static final String OWNER_ACCOUNT_INVALIDATE;
  private static final long initBalance = 10_000_000_000L;
  private static final long frozenBalance = 1_000_000_000L;
  private static final String assetName = "testCoin";

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ACCOUNT_INVALIDATE =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * Release resources.
   */
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

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createAccountCapsule() {
    AccountWrapper ownerCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            initBalance);
    ownerCapsule.setAssetIssuedName(ByteString.copyFromUtf8(assetName));
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
  }

  private Any getContract(String ownerAddress) {
    return Any.pack(
        Contract.UnfreezeAssetContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(ownerAddress))
            .build());
  }

  @Test
  public void testUnfreezeAsset() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen0 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now)
        .build();
    Frozen newFrozen1 = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance+1)
        .setExpireTime(now+600000)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen0).addFrozenSupply(newFrozen1).build();
    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeAssetOperator actuator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCCESS);
      AccountWrapper owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getAssetMap().get(assetName).longValue(), frozenBalance);
      Assert.assertEquals(owner.getFrozenSupplyCount(), 1);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    UnfreezeAssetOperator actuator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS_INVALIDATE),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAccount() {
    UnfreezeAssetOperator actuator = new UnfreezeAssetOperator(getContract(OWNER_ACCOUNT_INVALIDATE),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALIDATE + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notIssueAsset() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen).setAssetIssuedName(ByteString.EMPTY)
        .build();
    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeAssetOperator actuator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("this account did not issue any asset", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noFrozenSupply() {
    UnfreezeAssetOperator actuator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozen supply balance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notTimeToUnfreeze() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    Account account = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS))
        .getInstance();
    Frozen newFrozen = Frozen.newBuilder()
        .setFrozenBalance(frozenBalance)
        .setExpireTime(now + 60000)
        .build();
    account = account.toBuilder().addFrozenSupply(newFrozen).build();
    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeAssetOperator actuator = new UnfreezeAssetOperator(getContract(OWNER_ADDRESS),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("It's not time to unfreeze asset supply", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }
}