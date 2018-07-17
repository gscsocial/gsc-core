package org.gsc.core.operator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Date;
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
public class TransferOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_transfer_test";
  private static AnnotationConfigApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String TO_ADDRESS;
  private static final long AMOUNT = 100;
  private static final long OWNER_BALANCE = 9999999;
  private static final long TO_BALANCE = 100001;
  private static final String OWNER_ADDRESS_INVALIDATE = "aaaa";
  private static final String TO_ADDRESS_INVALIDATE = "bbb";
  private static final String OWNER_ACCOUNT_INVALIDATE;
  private static final String OWNER_NO_BALANCE;
  private static final String To_ACCOUNT_INVALIDATE;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ACCOUNT_INVALIDATE =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    OWNER_NO_BALANCE = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3433";
    To_ACCOUNT_INVALIDATE =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3422";
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
  public void createCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            OWNER_BALANCE);
    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            TO_BALANCE);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  private Any getContract(long count) {
    long nowTime = new Date().getTime();
    return Any.pack(
        Contract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
            .setAmount(count)
            .build());
  }

  private Any getContract(long count, String owneraddress, String toaddress) {
    long nowTime = new Date().getTime();
    return Any.pack(
        Contract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owneraddress)))
            .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toaddress)))
            .setAmount(count)
            .build());
  }

  @Test
  public void rightTransfer() {
    TransferOperator actuator = new TransferOperator(getContract(AMOUNT), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - AMOUNT - ChainConstant.TRANSFER_FEE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + AMOUNT);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void perfectTransfer() {
    TransferOperator actuator = new TransferOperator(
        getContract(OWNER_BALANCE - ChainConstant.TRANSFER_FEE), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount =
          dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));

      Assert.assertEquals(owner.getBalance(), 0);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE + OWNER_BALANCE);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void moreTransfer() {
    TransferOperator actuator = new TransferOperator(getContract(OWNER_BALANCE + 1), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("balance is not sufficient.".equals(e.getMessage()));
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void iniviateOwnerAddress() {
    TransferOperator actuator = new TransferOperator(
        getContract(10000L, OWNER_ADDRESS_INVALIDATE, TO_ADDRESS), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid ownerAddress");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid ownerAddress", e.getMessage());
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);

    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }

  }

  @Test
  public void iniviateToAddress() {
    TransferOperator actuator = new TransferOperator(
        getContract(10000L, OWNER_ADDRESS, TO_ADDRESS_INVALIDATE), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid toAddress");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid toAddress", e.getMessage());
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void iniviateTrx() {
    TransferOperator actuator = new TransferOperator(
        getContract(100L, OWNER_ADDRESS, OWNER_ADDRESS), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Cannot transfer trx to yourself.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Cannot transfer trx to yourself.", e.getMessage());
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void noExitOwnerAccount() {
    TransferOperator actuator = new TransferOperator(
        getContract(100L, OWNER_ACCOUNT_INVALIDATE, TO_ADDRESS), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Validate TransferContract error, no OwnerAccount.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Validate TransferContract error, no OwnerAccount.", e.getMessage());
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  /**
   * If to account not exit, create it.
   */
  public void noExitToAccount() {
    TransferOperator actuator = new TransferOperator(
        getContract(1_000_000L, OWNER_ADDRESS, To_ACCOUNT_INVALIDATE), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      AccountCapsule noExitAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(To_ACCOUNT_INVALIDATE));
      Assert.assertTrue(null == noExitAccount);
      actuator.validate();
      actuator.execute(ret);
      noExitAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(To_ACCOUNT_INVALIDATE));
      Assert.assertFalse(null == noExitAccount);    //Had created.
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE - 1_000_000L);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
      noExitAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(To_ACCOUNT_INVALIDATE));
      Assert.assertEquals(noExitAccount.getBalance(), 1_000_000L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(To_ACCOUNT_INVALIDATE));
    }
  }

  @Test
  public void zeroAmountTest() {
    TransferOperator actuator = new TransferOperator(getContract(0), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0.".equals(e.getMessage()));
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void negativeAmountTest() {
    TransferOperator actuator = new TransferOperator(getContract(-AMOUNT), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue("Amount must greater than 0.".equals(e.getMessage()));
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void addOverflowTest() {
    // First, increase the to balance. Else can't complete this test case.
    AccountCapsule toAccount = dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
    toAccount.setBalance(Long.MAX_VALUE);
    dbManager.getAccountStore().put(ByteArray.fromHexString(TO_ADDRESS), toAccount);
    TransferOperator actuator = new TransferOperator(getContract(1), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(("long overflow").equals(e.getMessage()));
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      toAccount = dbManager.getAccountStore().get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), Long.MAX_VALUE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void insufficientFee() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_NO_BALANCE)),
            AccountType.Normal,
            -10000L);
    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(To_ACCOUNT_INVALIDATE)),
            AccountType.Normal,
            100L);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);

    TransferOperator actuator = new TransferOperator(
        getContract(AMOUNT, OWNER_NO_BALANCE, To_ACCOUNT_INVALIDATE), dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Validate TransferContract error, insufficient fee.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Validate TransferContract error, insufficient fee.", e.getMessage());
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      AccountCapsule toAccount = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(TO_ADDRESS));
      Assert.assertEquals(owner.getBalance(), OWNER_BALANCE);
      Assert.assertEquals(toAccount.getBalance(), TO_BALANCE);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAccountStore().delete(ByteArray.fromHexString(To_ACCOUNT_INVALIDATE));
    }
  }

}
