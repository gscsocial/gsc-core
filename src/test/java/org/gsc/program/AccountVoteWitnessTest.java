package org.gsc.program;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol.AccountType;

@Slf4j
public class AccountVoteWitnessTest {

  private static AnnotationConfigApplicationContext context;

  private static Manager dbManager;
  private static String dbPath = "output_witness_test";

  static {
    Args.setParam(new String[] {"-d", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
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
    final List<WitnessWrapper> witnessWrapperList = this.getWitnessList();
    accountWrapperList.forEach(
        accountCapsule -> {
          dbManager
              .getAccountStore()
              .put(accountCapsule.getAddress().toByteArray(), accountCapsule);
          this.printAccount(accountCapsule.getAddress());
        });
    witnessWrapperList.forEach(
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
    final WitnessWrapper witnessWrapper = dbManager.getWitnessStore().get(address.toByteArray());
    if (null == witnessWrapper) {
      logger.info("address is {}  , witness is null", address.toStringUtf8());
      return;
    }
    logger.info(
        "address is {}  ,countVote is {}",
        witnessWrapper.getAddress().toStringUtf8(),
        witnessWrapper.getVoteCount());
  }

  private List<AccountWrapper> getAccountList() {
    final List<AccountWrapper> accountWrapperList = Lists.newArrayList();
    final AccountWrapper accountGsc =
        new AccountWrapper(
            ByteString.copyFrom("00000000001".getBytes()),
            ByteString.copyFromUtf8("Gsc"),
            AccountType.Normal);
    final AccountWrapper accountMarcus =
        new AccountWrapper(
            ByteString.copyFrom("00000000002".getBytes()),
            ByteString.copyFromUtf8("Marcus"),
            AccountType.Normal);
    final AccountWrapper accountOlivier =
        new AccountWrapper(
            ByteString.copyFrom("00000000003".getBytes()),
            ByteString.copyFromUtf8("Olivier"),
            AccountType.Normal);
    final AccountWrapper accountSasaXie =
        new AccountWrapper(
            ByteString.copyFrom("00000000004".getBytes()),
            ByteString.copyFromUtf8("SasaXie"),
            AccountType.Normal);
    final AccountWrapper accountVivider =
        new AccountWrapper(
            ByteString.copyFrom("00000000005".getBytes()),
            ByteString.copyFromUtf8("Vivider"),
            AccountType.Normal);
    // accountGsc addVotes
    accountGsc.addVotes(accountMarcus.getAddress(), 100);
    accountGsc.addVotes(accountOlivier.getAddress(), 100);
    accountGsc.addVotes(accountSasaXie.getAddress(), 100);
    accountGsc.addVotes(accountVivider.getAddress(), 100);

    // accountMarcus addVotes
    accountMarcus.addVotes(accountGsc.getAddress(), 100);
    accountMarcus.addVotes(accountOlivier.getAddress(), 100);
    accountMarcus.addVotes(accountSasaXie.getAddress(), 100);
    accountMarcus.addVotes(ByteString.copyFrom("00000000006".getBytes()), 100);
    accountMarcus.addVotes(ByteString.copyFrom("00000000007".getBytes()), 100);
    // accountOlivier addVotes
    accountOlivier.addVotes(accountGsc.getAddress(), 100);
    accountOlivier.addVotes(accountMarcus.getAddress(), 100);
    accountOlivier.addVotes(accountSasaXie.getAddress(), 100);
    accountOlivier.addVotes(accountVivider.getAddress(), 100);
    // accountSasaXie addVotes
    // accountVivider addVotes
    accountWrapperList.add(accountGsc);
    accountWrapperList.add(accountMarcus);
    accountWrapperList.add(accountOlivier);
    accountWrapperList.add(accountSasaXie);
    accountWrapperList.add(accountVivider);
    return accountWrapperList;
  }

  private List<WitnessWrapper> getWitnessList() {
    final List<WitnessWrapper> witnessWrapperList = Lists.newArrayList();
    final WitnessWrapper witnessGsc =
        new WitnessWrapper(ByteString.copyFrom("00000000001".getBytes()), 0, "");
    final WitnessWrapper witnessOlivier =
        new WitnessWrapper(ByteString.copyFrom("00000000003".getBytes()), 100, "");
    final WitnessWrapper witnessVivider =
        new WitnessWrapper(ByteString.copyFrom("00000000005".getBytes()), 200, "");
    final WitnessWrapper witnessSenaLiu =
        new WitnessWrapper(ByteString.copyFrom("00000000006".getBytes()), 300, "");
    witnessWrapperList.add(witnessGsc);
    witnessWrapperList.add(witnessOlivier);
    witnessWrapperList.add(witnessVivider);
    witnessWrapperList.add(witnessSenaLiu);
    return witnessWrapperList;
  }
}
