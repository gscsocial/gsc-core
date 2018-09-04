package org.gsc.core.operator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j

public class WitnessCreateOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_WitnessCreate_test";
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
    context = new GSCApplicationContext(DefaultConfig.class);
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
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    WitnessWrapper ownerCapsule =
        new WitnessWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            10_000_000L,
            URL);
    AccountWrapper ownerAccountSecondCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            300_000_000L);
    AccountWrapper ownerAccountFirstCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME_FIRST),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);

    dbManager.getWitnessStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getWitnessStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
  }

  private Any getContract(String address, String url) {
    return Any.pack(
        Contract.WitnessCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setUrl(ByteString.copyFrom(ByteArray.fromString(url)))
            .build());
  }

  private Any getContract(String address, ByteString url) {
    return Any.pack(
        Contract.WitnessCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setUrl(url)
            .build());
  }

  /**
   * first createWitness,result is success.
   */
  @Test
  public void firstCreateWitness() {
    WitnessCreateOperator actuator =
        new WitnessCreateOperator(getContract(OWNER_ADDRESS_FIRST, URL), dbManager);
    AccountWrapper accountWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      WitnessWrapper witnessCapsule =
          dbManager.getWitnessStore().get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
      Assert.assertNotNull(witnessCapsule);
      Assert.assertEquals(
          witnessCapsule.getInstance().getUrl(),
          URL);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * second createWitness,result is failed,exception is "Witness has existed".
   */
  @Test
  public void secondCreateAccount() {
    WitnessCreateOperator actuator =
        new WitnessCreateOperator(getContract(OWNER_ADDRESS_SECOND, URL), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertFalse(true);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness[" + OWNER_ADDRESS_SECOND + "] has existed", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Invalid Address createWitness,result is failed,exception is "Invalid address".
   */
  @Test
  public void InvalidAddress() {
    WitnessCreateOperator actuator =
        new WitnessCreateOperator(getContract(OWNER_ADDRESS_INVALID, URL), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
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
   * use Invalid url createWitness,result is failed,exception is "Invalid url".
   */
  @Test
  public void InvalidUrlTest() {
    TransactionResultWrapper ret = new TransactionResultWrapper();
    //Url cannot empty
    try {
      WitnessCreateOperator actuator = new WitnessCreateOperator(
          getContract(OWNER_ADDRESS_FIRST, ByteString.EMPTY), dbManager);
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid url");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //256 bytes
    String url256Bytes = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    //Url length can not greater than 256
    try {
      WitnessCreateOperator actuator = new WitnessCreateOperator(
          getContract(OWNER_ADDRESS_FIRST, ByteString.copyFromUtf8(url256Bytes + "0")), dbManager);
      actuator.validate();
      actuator.execute(ret);
      fail("Invalid url");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // 1 byte url is ok.
    try {
      WitnessCreateOperator actuator = new WitnessCreateOperator(
          getContract(OWNER_ADDRESS_FIRST, "0"), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      WitnessWrapper witnessCapsule =
          dbManager.getWitnessStore().get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
      Assert.assertNotNull(witnessCapsule);
      Assert.assertEquals(witnessCapsule.getInstance().getUrl(), "0");
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getWitnessStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
    // 256 bytes url is ok.
    try {
      WitnessCreateOperator actuator = new WitnessCreateOperator(
          getContract(OWNER_ADDRESS_FIRST, url256Bytes), dbManager);
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      WitnessWrapper witnessCapsule =
          dbManager.getWitnessStore().get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
      Assert.assertNotNull(witnessCapsule);
      Assert.assertEquals(witnessCapsule.getInstance().getUrl(), url256Bytes);
      Assert.assertTrue(true);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use AccountStore not exists Address createWitness,result is failed,exception is "account not
   * exists".
   */
  @Test
  public void noAccount() {
    WitnessCreateOperator actuator =
        new WitnessCreateOperator(getContract(OWNER_ADDRESS_NOACCOUNT, URL), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("account[+OWNER_ADDRESS_NOACCOUNT+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("account[" + OWNER_ADDRESS_NOACCOUNT + "] not exists", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  /**
   * use Account  ,result is failed,exception is "account not exists".
   */
  @Test
  public void balanceNotSufficient() {
    AccountWrapper balanceNotSufficientCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8("balanceNotSufficient"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_BALANCENOTSUFFIENT)),
            AccountType.Normal,
            50L);

    dbManager.getAccountStore()
        .put(balanceNotSufficientCapsule.getAddress().toByteArray(), balanceNotSufficientCapsule);
    WitnessCreateOperator actuator =
        new WitnessCreateOperator(getContract(OWNER_ADDRESS_BALANCENOTSUFFIENT, URL), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("witnessAccount  has balance[" + balanceNotSufficientCapsule.getBalance()
          + "] < MIN_BALANCE[100]");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("balance < AccountUpgradeCost", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
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
}