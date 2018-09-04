package org.gsc.core.capsule;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.gsc.core.wrapper.AccountWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.config.args.Args;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Vote;

public class AccountWrapperTest {

  private static String dbPath = "output_accountCapsule_test";
  static AccountWrapper accountWrapperTest;
  static AccountWrapper accountWrapper;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        Constant.TEST_CONF);
    ByteString accountName = ByteString.copyFrom(AccountWrapperTest.randomBytes(16));
    ByteString address = ByteString.copyFrom(AccountWrapperTest.randomBytes(32));
    AccountType accountType = AccountType.forNumber(1);
    accountWrapperTest = new AccountWrapper(accountName, address, accountType);
    byte[] accountByte = accountWrapperTest.getData();
    accountWrapper = new AccountWrapper(accountByte);
    accountWrapperTest.setBalance(1111L);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void getDataTest() {
    //test AccountWrapper onstructed function
    Assert.assertEquals(accountWrapper.getInstance().getAccountName(),
        accountWrapperTest.getInstance().getAccountName());
    Assert.assertEquals(accountWrapper.getInstance().getType(),
        accountWrapperTest.getInstance().getType());
    Assert.assertEquals(1111, accountWrapperTest.getBalance());
  }

  @Test
  public void addVotesTest() {
    //test addVote and getVotesList function
    ByteString voteAddress = ByteString.copyFrom(AccountWrapperTest.randomBytes(32));
    long voteAdd = 10L;
    accountWrapperTest.addVotes(voteAddress, voteAdd);
    List<Vote> votesList = accountWrapperTest.getVotesList();
    for (Vote vote :
        votesList) {
      Assert.assertEquals(voteAddress, vote.getVoteAddress());
      Assert.assertEquals(voteAdd, vote.getVoteCount());
    }
  }

  @Test
  public void AssetAmountTest() {
    //test AssetAmount ,addAsset and reduceAssetAmount function

    String nameAdd = "TokenX";
    long amountAdd = 222L;
    boolean addBoolean = accountWrapperTest
        .addAssetAmount(nameAdd.getBytes(), amountAdd);

    Assert.assertTrue(addBoolean);

    Map<String, Long> assetMap = accountWrapperTest.getAssetMap();
    for (Map.Entry<String, Long> entry : assetMap.entrySet()) {
      Assert.assertEquals(nameAdd, entry.getKey());
      Assert.assertEquals(amountAdd, entry.getValue().longValue());
    }
    long amountReduce = 22L;

    boolean reduceBoolean = accountWrapperTest
        .reduceAssetAmount(ByteArray.fromString("TokenX"), amountReduce);
    Assert.assertTrue(reduceBoolean);

    Map<String, Long> assetMapAfter = accountWrapperTest.getAssetMap();
    for (Map.Entry<String, Long> entry : assetMapAfter.entrySet()) {
      Assert.assertEquals(nameAdd, entry.getKey());
      Assert.assertEquals(amountAdd - amountReduce, entry.getValue().longValue());
    }
    String key = nameAdd;
    long value = 11L;
    boolean addAsssetBoolean = accountWrapperTest.addAsset(key.getBytes(), value);
    Assert.assertFalse(addAsssetBoolean);

    String keyName = "TokenTest";
    long amountValue = 33L;
    boolean addAsssetTrue = accountWrapperTest.addAsset(keyName.getBytes(), amountValue);
    Assert.assertTrue(addAsssetTrue);
  }


  public static byte[] randomBytes(int length) {
    //generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    return result;
  }
}