package org.gsc.core.operator;

import static org.testng.Assert.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ExchangeWrapper;
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
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j

public class ExchangeCreateOperatorTest {

  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_ExchangeCreate_test";
  private static final String ACCOUNT_NAME_FIRST = "ownerF";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String ACCOUNT_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND;
  private static final String URL = "https://tron.network";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String OWNER_ADDRESS_BALANCENOTSUFFIENT;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
    OWNER_ADDRESS_BALANCENOTSUFFIENT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e06d4271a1ced";
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
  public void initTest() {
    AccountWrapper ownerAccountFirstCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            AccountType.Normal,
            300_000_000L);
    AccountWrapper ownerAccountSecondCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
    dbManager.getDynamicPropertiesStore().saveLatestExchangeNum(0);

  }

  private Any getContract(String address, String firstTokenId, long firstTokenBalance,
      String secondTokenId, long secondTokenBalance) {
    return Any.pack(
        Contract.ExchangeCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setFirstTokenId(ByteString.copyFrom(firstTokenId.getBytes()))
            .setFirstTokenBalance(firstTokenBalance)
            .setSecondTokenId(ByteString.copyFrom(secondTokenId.getBytes()))
            .setSecondTokenBalance(secondTokenBalance)
            .build());
  }

  /**
   * first createExchange,result is success.
   */
  @Test
  public void successExchangeCreate() {
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);

      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
      Assert.assertEquals(id, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
//      Assert.assertEquals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId());
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeWrapper.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetMap = accountWrapper.getAssetMap();
      Assert.assertEquals(10000_000000L - 1024_000000L, accountWrapper.getBalance());
      Assert.assertEquals(0L, assetMap.get(firstTokenId).longValue());
      Assert.assertEquals(0L, assetMap.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * second create Exchange, result is success.
   */
  @Test
  public void successExchangeCreate2() {
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(200_000_000_000000L);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      long id = 1;
      ExchangeWrapper exchangeWrapper = dbManager.getExchangeStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(exchangeWrapper);
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), id);

      Assert.assertEquals(ByteString.copyFrom(ownerAddress), exchangeWrapper.getCreatorAddress());
      Assert.assertEquals(id, exchangeWrapper.getID());
      Assert.assertEquals(1000000, exchangeWrapper.getCreateTime());
      Assert.assertTrue(Arrays.equals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId()));
//      Assert.assertEquals(firstTokenId.getBytes(), exchangeWrapper.getFirstTokenId());
      Assert.assertEquals(firstTokenId, ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
      Assert.assertEquals(firstTokenBalance, exchangeWrapper.getFirstTokenBalance());
      Assert.assertEquals(secondTokenId, ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      Assert.assertEquals(secondTokenBalance, exchangeWrapper.getSecondTokenBalance());

      accountWrapper = dbManager.getAccountStore().get(ownerAddress);
      Map<String, Long> assetMap = accountWrapper.getAssetMap();
      Assert.assertEquals(200_000_000_000000L - 1024_000000L - firstTokenBalance,
          accountWrapper.getBalance());
      Assert.assertEquals(100_000_000L, assetMap.get(secondTokenId).longValue());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  /**
   * use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void invalidAddress() {
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_INVALID, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use AccountStore not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void noAccount() {
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_NOACCOUNT, firstTokenId, firstTokenBalance, secondTokenId,
        secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * No enough balance
   */
  @Test
  public void noEnoughBalance() {
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(1000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("No enough balance for exchange create fee!",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * exchange same tokens
   */
  @Test
  public void sameTokens() {
    String firstTokenId = "abc";
    long firstTokenBalance = 100000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("cannot exchange same tokens",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * token balance less than zero
   */
  @Test
  public void lessToken() {
    String firstTokenId = "abc";
    long firstTokenBalance = 0L;
    String secondTokenId = "def";
    long secondTokenBalance = 0L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), 1000);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 1000);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance must greater than zero",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * token balance must less than balanceLimit
   */
  @Test
  public void moreThanBalanceLimit() {
    String firstTokenId = "abc";
    long firstTokenBalance = 1_000_000_000_000_001L;
    String secondTokenId = "def";
    long secondTokenBalance = 100000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), secondTokenBalance);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("token balance must less than 1000000000000000",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * balance is not enough
   */
  @Test
  public void balanceNotEnough() {
    String firstTokenId = "_";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "abc";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(firstTokenBalance + 1000L);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * first token balance is not enough
   */
  @Test
  public void firstTokenBalanceNotEnough() {
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance - 1000L);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 200_000_000L);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("first token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * balance is not enough
   */
  @Test
  public void balanceNotEnough2() {
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000L;
    String secondTokenId = "_";
    long secondTokenBalance = 100_000_000_000000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.setBalance(secondTokenBalance + 1000L);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), 200_000_000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * first token balance is not enough
   */
  @Test
  public void secondTokenBalanceNotEnough() {
    String firstTokenId = "abc";
    long firstTokenBalance = 100_000_000_000000L;
    String secondTokenId = "def";
    long secondTokenBalance = 100_000_000L;

    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_FIRST);
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(ownerAddress);
    accountWrapper.addAssetAmount(firstTokenId.getBytes(), firstTokenBalance);
    accountWrapper.addAssetAmount(secondTokenId.getBytes(), 90_000_000L);
    accountWrapper.setBalance(10000_000000L);
    dbManager.getAccountStore().put(ownerAddress, accountWrapper);

    ExchangeCreateOperator actuator = new ExchangeCreateOperator(getContract(
        OWNER_ADDRESS_FIRST, firstTokenId, firstTokenBalance, secondTokenId, secondTokenBalance),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestExchangeNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("second token balance is not enough",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }
}