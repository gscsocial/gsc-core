package org.gsc.program;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol.AccountType;

@Slf4j
public class AccountVoteWitnessTest {

  private static GSCApplicationContext context;

  private static Manager dbManager;
  private static String dbPath = "output_witness_test";

  static {
    Args.setParam(new String[] {"-d", dbPath}, Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  /** init db. */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    // Args.setParam(new String[]{}, Constant.TEST_CONF);
    //  dbManager = new Manager();
    //  dbManager.init();
  }

  /** remo db when after test. */
  @AfterClass
  public static void removeDb() {
    Args.clearParam();

    File dbFolder = new File(dbPath);
    if (deleteFolder(dbFolder)) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }

  private static Boolean deleteFolder(File index) {
    if (!index.isDirectory() || index.listFiles().length <= 0) {
      return index.delete();
    }
    for (File file : index.listFiles()) {
      if (null != file && !deleteFolder(file)) {
        return false;
      }
    }
    return index.delete();
  }

  @Test
  public void testAccountVoteWitness() {
    final List<AccountWrapper> accountWrapperList = this.getAccountList();
    final List<WitnessWrapper> witnessCapsuleList = this.getWitnessList();
    accountWrapperList.forEach(
        accountCapsule -> {
          dbManager
              .getAccountStore()
              .put(accountCapsule.getAddress().toByteArray(), accountCapsule);
          this.printAccount(accountCapsule.getAddress());
        });
    witnessCapsuleList.forEach(
        witnessCapsule ->
            dbManager
                .getWitnessStore()
                .put(witnessCapsule.getAddress().toByteArray(), witnessCapsule));
    dbManager.getWitnessController().updateWitness();
    this.printWitness(ByteString.copyFrom("00000000001".getBytes()));
    this.printWitness(ByteString.copyFrom("00000000002".getBytes()));
    this.printWitness(ByteString.copyFrom("00000000003".getBytes()));
    this.printWitness(ByteString.copyFrom("00000000004".getBytes()));
    this.printWitness(ByteString.copyFrom("00000000005".getBytes()));
    this.printWitness(ByteString.copyFrom("00000000006".getBytes()));
    this.printWitness(ByteString.copyFrom("00000000007".getBytes()));
  }

  private void printAccount(final ByteString address) {
    final AccountWrapper accountWrapper = dbManager.getAccountStore().get(address.toByteArray());
    if (null == accountWrapper) {
      logger.info("address is {}  , account is null", address.toStringUtf8());
      return;
    }
    logger.info(
        "address is {}  ,countVoteSize is {}",
        accountWrapper.getAddress().toStringUtf8(),
        accountWrapper.getVotesList().size());
  }

  private void printWitness(final ByteString address) {
    final WitnessWrapper witnessCapsule = dbManager.getWitnessStore().get(address.toByteArray());
    if (null == witnessCapsule) {
      logger.info("address is {}  , witness is null", address.toStringUtf8());
      return;
    }
    logger.info(
        "address is {}  ,countVote is {}",
        witnessCapsule.getAddress().toStringUtf8(),
        witnessCapsule.getVoteCount());
  }

  private List<AccountWrapper> getAccountList() {
    final List<AccountWrapper> accountWrapperList = Lists.newArrayList();
    final AccountWrapper account =
        new AccountWrapper(
            ByteString.copyFrom("00000000001".getBytes()),
            ByteString.copyFromUtf8("1"),
            AccountType.Normal);
    final AccountWrapper account1 =
        new AccountWrapper(
            ByteString.copyFrom("00000000002".getBytes()),
            ByteString.copyFromUtf8("2"),
            AccountType.Normal);
    final AccountWrapper account2 =
        new AccountWrapper(
            ByteString.copyFrom("00000000003".getBytes()),
            ByteString.copyFromUtf8("3"),
            AccountType.Normal);
    final AccountWrapper account3 =
        new AccountWrapper(
            ByteString.copyFrom("00000000004".getBytes()),
            ByteString.copyFromUtf8("4"),
            AccountType.Normal);
    final AccountWrapper account4 =
        new AccountWrapper(
            ByteString.copyFrom("00000000005".getBytes()),
            ByteString.copyFromUtf8("5"),
            AccountType.Normal);
    // account addVotes
    account.addVotes(account1.getAddress(), 100);
    account.addVotes(account2.getAddress(), 100);
    account.addVotes(account3.getAddress(), 100);
    account.addVotes(account4.getAddress(), 100);

    // account1 addVotes
    account1.addVotes(account.getAddress(), 100);
    account1.addVotes(account2.getAddress(), 100);
    account1.addVotes(account3.getAddress(), 100);
    account1.addVotes(ByteString.copyFrom("00000000006".getBytes()), 100);
    account1.addVotes(ByteString.copyFrom("00000000007".getBytes()), 100);
    // account2 addVotes
    account2.addVotes(account.getAddress(), 100);
    account2.addVotes(account1.getAddress(), 100);
    account2.addVotes(account3.getAddress(), 100);
    account2.addVotes(account4.getAddress(), 100);
    // account3 addVotes
    // account4 addVotes
    accountWrapperList.add(account);
    accountWrapperList.add(account1);
    accountWrapperList.add(account2);
    accountWrapperList.add(account3);
    accountWrapperList.add(account4);
    return accountWrapperList;
  }

  private List<WitnessWrapper> getWitnessList() {
    final List<WitnessWrapper> witnessCapsuleList = Lists.newArrayList();
    final WitnessWrapper witness1 =
        new WitnessWrapper(ByteString.copyFrom("00000000001".getBytes()), 0, "");
    final WitnessWrapper witness2 =
        new WitnessWrapper(ByteString.copyFrom("00000000003".getBytes()), 100, "");
    final WitnessWrapper witness3 =
        new WitnessWrapper(ByteString.copyFrom("00000000005".getBytes()), 200, "");
    final WitnessWrapper witness4 =
        new WitnessWrapper(ByteString.copyFrom("00000000006".getBytes()), 300, "");
    witnessCapsuleList.add(witness1);
    witnessCapsuleList.add(witness2);
    witnessCapsuleList.add(witness3);
    witnessCapsuleList.add(witness4);
    return witnessCapsuleList;
  }
}
