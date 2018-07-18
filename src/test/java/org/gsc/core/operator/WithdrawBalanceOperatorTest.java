package org.gsc.core.operator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
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
import org.gsc.config.args.Witness;
import org.gsc.db.Manager;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class WithdrawBalanceOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_withdraw_balance_test";
  private static AnnotationConfigApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALIDATE = "aaaa";
  private static final String OWNER_ACCOUNT_INVALIDATE;
  private static final long initBalance = 10_000_000_000L;
  private static final long allowance = 32_000_000L;

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
    AccountWrapper ownerCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
  }

  private Any getContract(String ownerAddress) {
    return Any.pack(
        Contract.WithdrawBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .build());
  }

  @Test
  public void testWithdrawBalance() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);
    try {
      dbManager.adjustAllowance(address, allowance);
    } catch (BalanceInsufficientException e) {
      fail("BalanceInsufficientException");
    }
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    Assert.assertEquals(accountWrapper.getAllowance(), allowance);
    Assert.assertEquals(accountWrapper.getLatestWithdrawTime(), 0);

    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address),
        100, "http://baidu.com");
    dbManager.getWitnessStore().put(address, witnessWrapper);

    WithdrawBalanceOperator actuator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance + allowance);
      Assert.assertEquals(owner.getAllowance(), 0);
      Assert.assertNotEquals(owner.getLatestWithdrawTime(), 0);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void invalidOwnerAddress() {
    WithdrawBalanceOperator actuator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS_INVALIDATE), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

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
    WithdrawBalanceOperator actuator = new WithdrawBalanceOperator(
        getContract(OWNER_ACCOUNT_INVALIDATE), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

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
  public void notWitness() {
//    long now = System.currentTimeMillis();
//    AccountWrapper accountCapsule = dbManager.getAccountStore()
//        .get(ByteArray.fromHexString(OWNER_ADDRESS));
//    accountCapsule.setFrozen(1_000_000_000L, now);
//    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    WithdrawBalanceOperator actuator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS + "] is not a witnessAccount",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noAllowance() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);

    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    Assert.assertEquals(accountWrapper.getAllowance(), 0);

    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address),
        100, "http://baidu.com");
    dbManager.getWitnessStore().put(address, witnessWrapper);

    WithdrawBalanceOperator actuator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("witnessAccount does not have any allowance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void isGR() {
    Witness w = Args.getInstance().getGenesisBlock().getWitnesses().get(0);
    byte[] address = w.getAddress();
    AccountWrapper grCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8("gr"),
            ByteString.copyFrom(address),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(grCapsule.createDbKey(), grCapsule);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    try {
      dbManager.adjustAllowance(address, allowance);
    } catch (BalanceInsufficientException e) {
      fail("BalanceInsufficientException");
    }
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    Assert.assertEquals(accountWrapper.getAllowance(), allowance);

    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address),
        100, "http://google.com");

    dbManager.getAccountStore().put(address, accountWrapper);
    dbManager.getWitnessStore().put(address, witnessWrapper);

    WithdrawBalanceOperator actuator = new WithdrawBalanceOperator(
        getContract(ByteArray.toHexString(address)), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertTrue(dbManager.getWitnessStore().has(address));

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      String readableOwnerAddress = StringUtil.createReadableString(address);
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + readableOwnerAddress
          + "] is a guard representative and is not allowed to withdraw Balance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notTimeToWithdraw() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);
    try {
      dbManager.adjustAllowance(address, allowance);
    } catch (BalanceInsufficientException e) {
      fail("BalanceInsufficientException");
    }
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    accountWrapper.setLatestWithdrawTime(now);
    Assert.assertEquals(accountWrapper.getAllowance(), allowance);
    Assert.assertEquals(accountWrapper.getLatestWithdrawTime(), now);

    WitnessWrapper witnessWrapper = new WitnessWrapper(ByteString.copyFrom(address),
        100, "http://baidu.com");

    dbManager.getAccountStore().put(address, accountWrapper);
    dbManager.getWitnessStore().put(address, witnessWrapper);

    WithdrawBalanceOperator actuator = new WithdrawBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("The last withdraw time is "
          + now + ",less than 24 hours", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

}

