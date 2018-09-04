package org.gsc.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Random;

import org.gsc.common.application.GSCApplicationContext;
import org.gsc.core.wrapper.AccountWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.protos.Protocol.AccountType;

public class AccountIdIndexStoreTest {

  private static String dbPath = "output_AccountIndexStore_test";
  private static GSCApplicationContext context;
  private static AccountIdIndexStore accountIdIndexStore;
  private static final byte[] ACCOUNT_ADDRESS_ONE = randomBytes(16);
  private static final byte[] ACCOUNT_ADDRESS_TWO = randomBytes(16);
  private static final byte[] ACCOUNT_ADDRESS_THREE = randomBytes(16);
  private static final byte[] ACCOUNT_ADDRESS_FOUR = randomBytes(16);
  private static final byte[] ACCOUNT_NAME_ONE = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_TWO = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_THREE = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_FOUR = randomBytes(6);
  private static final byte[] ACCOUNT_NAME_FIVE = randomBytes(6);
  private static AccountWrapper accountWrapper1;
  private static AccountWrapper accountWrapper2;
  private static AccountWrapper accountWrapper3;
  private static AccountWrapper accountWrapper4;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @BeforeClass
  public static void init() {
    accountIdIndexStore = context.getBean(AccountIdIndexStore.class);
    accountWrapper1 = new AccountWrapper(ByteString.copyFrom(ACCOUNT_ADDRESS_ONE),
        ByteString.copyFrom(ACCOUNT_NAME_ONE), AccountType.Normal);
    accountWrapper1.setAccountId(ByteString.copyFrom(ACCOUNT_NAME_ONE).toByteArray());
    accountWrapper2 = new AccountWrapper(ByteString.copyFrom(ACCOUNT_ADDRESS_TWO),
        ByteString.copyFrom(ACCOUNT_NAME_TWO), AccountType.Normal);
    accountWrapper2.setAccountId(ByteString.copyFrom(ACCOUNT_NAME_TWO).toByteArray());
    accountWrapper3 = new AccountWrapper(ByteString.copyFrom(ACCOUNT_ADDRESS_THREE),
        ByteString.copyFrom(ACCOUNT_NAME_THREE), AccountType.Normal);
    accountWrapper3.setAccountId(ByteString.copyFrom(ACCOUNT_NAME_THREE).toByteArray());
    accountWrapper4 = new AccountWrapper(ByteString.copyFrom(ACCOUNT_ADDRESS_FOUR),
        ByteString.copyFrom(ACCOUNT_NAME_FOUR), AccountType.Normal);
    accountWrapper4.setAccountId(ByteString.copyFrom(ACCOUNT_NAME_FOUR).toByteArray());
    accountIdIndexStore.put(accountWrapper1);
    accountIdIndexStore.put(accountWrapper2);
    accountIdIndexStore.put(accountWrapper3);
    accountIdIndexStore.put(accountWrapper4);
  }

  @Test
  public void putAndGet() {
    byte[] address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_ONE));
    Assert.assertArrayEquals("putAndGet1", address, ACCOUNT_ADDRESS_ONE);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_TWO));
    Assert.assertArrayEquals("putAndGet2", address, ACCOUNT_ADDRESS_TWO);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_THREE));
    Assert.assertArrayEquals("putAndGet3", address, ACCOUNT_ADDRESS_THREE);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_FOUR));
    Assert.assertArrayEquals("putAndGet4", address, ACCOUNT_ADDRESS_FOUR);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOUNT_NAME_FIVE));
    Assert.assertNull("putAndGet4", address);

  }

  @Test
  public void putAndHas() {
    Boolean result = accountIdIndexStore.has(ACCOUNT_NAME_ONE);
    Assert.assertTrue("putAndGet1", result);
    result = accountIdIndexStore.has(ACCOUNT_NAME_TWO);
    Assert.assertTrue("putAndGet2", result);
    result = accountIdIndexStore.has(ACCOUNT_NAME_THREE);
    Assert.assertTrue("putAndGet3", result);
    result = accountIdIndexStore.has(ACCOUNT_NAME_FOUR);
    Assert.assertTrue("putAndGet4", result);
    result = accountIdIndexStore.has(ACCOUNT_NAME_FIVE);
    Assert.assertFalse("putAndGet4", result);
  }


  @Test
  public void testCaseInsensitive() {
    byte[] ACCOUNT_NAME = "aABbCcDd_ssd1234".getBytes();
    byte[] ACCOUNT_ADDRESS = randomBytes(16);

    AccountWrapper accountWrapper = new AccountWrapper(ByteString.copyFrom(ACCOUNT_ADDRESS),
        ByteString.copyFrom(ACCOUNT_NAME), AccountType.Normal);
    accountWrapper.setAccountId(ByteString.copyFrom(ACCOUNT_NAME).toByteArray());
    accountIdIndexStore.put(accountWrapper);

    Boolean result = accountIdIndexStore.has(ACCOUNT_NAME);
    Assert.assertTrue("fail", result);

    byte[] lowerCase = ByteString
        .copyFromUtf8(ByteString.copyFrom(ACCOUNT_NAME).toStringUtf8().toLowerCase())
        .toByteArray();
    result = accountIdIndexStore.has(lowerCase);
    Assert.assertTrue("lowerCase fail", result);

    byte[] upperCase = ByteString
        .copyFromUtf8(ByteString.copyFrom(ACCOUNT_NAME).toStringUtf8().toUpperCase())
        .toByteArray();
    result = accountIdIndexStore.has(upperCase);
    Assert.assertTrue("upperCase fail", result);

    Assert.assertNotNull("getLowerCase fail", accountIdIndexStore.get(upperCase));

  }

  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Wallet.getAddressPreFixByte();
    return result;
  }
}