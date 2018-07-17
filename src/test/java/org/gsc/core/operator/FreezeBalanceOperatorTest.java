package org.gsc.core.operator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountCapsule;
import org.gsc.core.wrapper.TransactionResultCapsule;
import org.gsc.config.DefaultConfig;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class FreezeBalanceOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_freeze_balance_test";
  private static AnnotationConfigApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALIDATE = "aaaa";
  private static final String OWNER_ACCOUNT_INVALIDATE;
  private static final long initBalance = 10_000_000_000L;

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
    //    Args.setParam(new String[]{"--output-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
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
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  private Any getContract(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        Contract.FreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .build());
  }

  @Test
  public void testFreezeBalance() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(owner.getFrozenBalance(), frozenBalance);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void freezeLessThanZero() {
    long frozenBalance = -1_000_000_000L;
    long duration = 3;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenBalance must be positive", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void freezeMoreThanBalance() {
    long frozenBalance = 11_000_000_000L;
    long duration = 3;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenBalance must be less than accountBalance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS_INVALIDATE, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);

      Assert.assertEquals("Invalid address", e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }

  }

  @Test
  public void invalidOwnerAccount() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ACCOUNT_INVALIDATE, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALIDATE + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void durationLessThanMin() {
    long frozenBalance = 1_000_000_000L;
    long duration = 2;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
      long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenDuration must be less than " + maxFrozenTime + " days "
              + "and more than " + minFrozenTime + " days"
          , e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void durationMoreThanMax() {
    long frozenBalance = 1_000_000_000L;
    long duration = 4;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
      long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenDuration must be less than " + maxFrozenTime + " days "
              + "and more than " + minFrozenTime + " days"
          , e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void lessThan1TrxTest() {
    long frozenBalance = 1;
    long duration = 3;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenBalance must be more than 1TRX", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void frozenNumTest() {
    AccountCapsule account = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    account.setFrozen(1_000L, 1_000_000_000L);
    account.setFrozen(1_000_000L, 1_000_000_000L);
    dbManager.getAccountStore().put(account.getAddress().toByteArray(), account);

    long frozenBalance = 20_000_000L;
    long duration = 3L;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenCount must be 0 or 1", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  //@Test
  public void moreThanFrozenNumber() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceOperator actuator = new FreezeBalanceOperator(
        getContract(OWNER_ADDRESS, frozenBalance, duration), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      long maxFrozenNumber = dbManager.getDynamicPropertiesStore().getMaxFrozenNumber();
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("max frozen number is: " + maxFrozenNumber, e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }
}
