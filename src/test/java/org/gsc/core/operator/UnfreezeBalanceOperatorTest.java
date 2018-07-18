package org.gsc.core.operator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.VotesWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;
import org.gsc.protos.Protocol.Vote;

@Slf4j
public class UnfreezeBalanceOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "output_unfreeze_balance_test";
  private static AnnotationConfigApplicationContext context;
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALIDATE = "aaaa";
  private static final String OWNER_ACCOUNT_INVALIDATE;
  private static final long initBalance = 10_000_000_000L;
  private static final long frozenBalance = 1_000_000_000L;

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
        Contract.UnfreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .build());
  }

  @Test
  public void testUnfreezeBalance() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(frozenBalance, now);
    Assert.assertEquals(accountWrapper.getFrozenBalance(), frozenBalance);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator actuator = new UnfreezeBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
//    try {
//      Thread.sleep(10);
//    } catch (InterruptedException e) {
//      fail("Interrupted exception in sleep.");
//    }

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCCESS);
      AccountWrapper owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance + frozenBalance);
      Assert.assertEquals(owner.getFrozenBalance(), 0);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void invalidOwnerAddress() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator actuator = new UnfreezeBalanceOperator(
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
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator actuator = new UnfreezeBalanceOperator(
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
  public void noFrozenBalance() {
//    long now = System.currentTimeMillis();
//    AccountWrapper accountCapsule = dbManager.getAccountStore()
//        .get(ByteArray.fromHexString(OWNER_ADDRESS));
//    accountCapsule.setFrozen(1_000_000_000L, now);
//    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceOperator actuator = new UnfreezeBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozenBalance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void notTimeToUnfreeze() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountWrapper.setFrozen(1_000_000_000L, now + 60000);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator actuator = new UnfreezeBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("It's not time to unfreeze.", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testClearVotes() {
    byte[] ownerAddressBytes = ByteArray.fromHexString(OWNER_ADDRESS);
    ByteString ownerAddress = ByteString.copyFrom(ownerAddressBytes);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddressBytes);
    accountWrapper.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    UnfreezeBalanceOperator actuator = new UnfreezeBalanceOperator(
        getContract(OWNER_ADDRESS), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    dbManager.getVotesStore().reset();
    Assert.assertNull(dbManager.getVotesStore().get(ownerAddressBytes));
    try {
      actuator.validate();
      actuator.execute(ret);
      VotesWrapper votesWrapper = dbManager.getVotesStore().get(ownerAddressBytes);
      Assert.assertNotNull(votesWrapper);
      Assert.assertEquals(0, votesWrapper.getNewVotes().size());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // if had votes
    List<Vote> oldVotes = new ArrayList<Vote>();
    VotesWrapper votesWrapper = new VotesWrapper(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        oldVotes);
    votesWrapper.addNewVotes(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
        100);
    dbManager.getVotesStore().put(ByteArray.fromHexString(OWNER_ADDRESS), votesWrapper);
    accountWrapper.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
    try {
      actuator.validate();
      actuator.execute(ret);
      votesWrapper = dbManager.getVotesStore().get(ownerAddressBytes);
      Assert.assertNotNull(votesWrapper);
      Assert.assertEquals(0, votesWrapper.getNewVotes().size());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }


  }

}

