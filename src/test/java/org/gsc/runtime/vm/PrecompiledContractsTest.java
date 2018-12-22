package org.gsc.runtime.vm;


import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.operator.FreezeBalanceOperator;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.ProposalWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.gsc.runtime.vm.PrecompiledContracts.PrecompiledContract;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.ByteUtil;
import org.gsc.common.utils.FileUtil;
import org.gsc.common.utils.StringUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.db.Manager;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Proposal.State;

@Slf4j
public class PrecompiledContractsTest {

  // common
  private static final DataWord voteContractAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010001");
  private static final DataWord freezeBalanceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010002");
  private static final DataWord unFreezeBalanceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010003");
  private static final DataWord withdrawBalanceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010004");
  private static final DataWord proposalApproveAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010005");
  private static final DataWord proposalCreateAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010006");
  private static final DataWord proposalDeleteAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010007");
  private static final DataWord convertFromgscBytesAddressAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010008");
  private static final DataWord convertFromgscBase58AddressAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010009");

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "output_PrecompiledContracts_test";
  private static final String ACCOUNT_NAME = "account";
  private static final String OWNER_ADDRESS;
  private static final String WITNESS_NAME = "witness";
  private static final String WITNESS_ADDRESS;
  private static final String WITNESS_ADDRESS_BASE = "548794500882809695a8a687866e76d4271a1abc" ;
  private static final String URL = "https://gsc.network";

  // withdraw
  private static final long initBalance = 10_000_000_000L;
  private static final long allowance = 32_000_000L;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    WITNESS_ADDRESS = Wallet.getAddressPreFixString() + WITNESS_ADDRESS_BASE;

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
    // witness: witnessCapsule
    WitnessWrapper witnessCapsule =
        new WitnessWrapper(
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            10L,
            URL);
    // witness: AccountWrapper
    AccountWrapper witnessAccountWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8(WITNESS_NAME),
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            AccountType.Normal,
            initBalance);
    // some normal account
    AccountWrapper ownerAccountFirstCapsule =
        new AccountWrapper(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            10_000_000_000_000L);

    dbManager.getAccountStore()
        .put(witnessAccountWrapper.getAddress().toByteArray(), witnessAccountWrapper);
    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getWitnessStore().put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
  }


  private Any getFreezeContract(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        Contract.FreezeBalanceContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(ownerAddress))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .build());
  }


  private PrecompiledContract createPrecompiledContract(DataWord addr, String ownerAddress) {
    PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
    /*
    contract.setCallerAddress(convertToGscAddress(Hex.decode(ownerAddress)));
    contract.setDeposit(DepositImpl.createRoot(dbManager));
    ProgramResult programResult = new ProgramResult();
    contract.setResult(programResult);
    */
    return contract;
  }

  @Test
  public void voteWitnessNativeTest()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ContractValidateException, ContractExeException {
    PrecompiledContract contract = createPrecompiledContract(voteContractAddr, OWNER_ADDRESS);
    byte[] witnessAddressBytes = new byte[32];
    byte[] witnessAddressBytes21 = Hex.decode(WITNESS_ADDRESS);
    System.arraycopy(witnessAddressBytes21, 0, witnessAddressBytes,
        witnessAddressBytes.length - witnessAddressBytes21.length, witnessAddressBytes21.length);

    DataWord voteCount = new DataWord(
        "0000000000000000000000000000000000000000000000000000000000000001");
    byte[] voteCountBytes = voteCount.getData();
    byte[] data = new byte[witnessAddressBytes.length + voteCountBytes.length];
    System.arraycopy(witnessAddressBytes, 0, data, 0, witnessAddressBytes.length);
    System.arraycopy(voteCountBytes, 0, data, witnessAddressBytes.length, voteCountBytes.length);

    long frozenBalance = 1_000_000_000_000L;
    long duration = 3;
    Any freezeContract = getFreezeContract(OWNER_ADDRESS, frozenBalance, duration);
    Constructor<FreezeBalanceOperator> constructor =
        FreezeBalanceOperator.class
            .getDeclaredConstructor(Any.class, dbManager.getClass());
    constructor.setAccessible(true);
    FreezeBalanceOperator freezeBalanceActuator = constructor
        .newInstance(freezeContract, dbManager);

    TransactionResultWrapper ret = new TransactionResultWrapper();
    freezeBalanceActuator.validate();
    freezeBalanceActuator.execute(ret);

    Boolean result = contract.execute(data).getLeft();
    Assert.assertEquals(1,
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
            .get(0).getVoteCount());
    Assert.assertArrayEquals(ByteArray.fromHexString(WITNESS_ADDRESS),
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
            .get(0).getVoteAddress().toByteArray());
    Assert.assertEquals(true, result);
  }

  @Test
  public void withdrawBalanceNativeTest() {
    PrecompiledContract contract = createPrecompiledContract(withdrawBalanceAddr, WITNESS_ADDRESS);

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    byte[] address = ByteArray.fromHexString(WITNESS_ADDRESS);
    try {
      dbManager.adjustAllowance(address, allowance);
    } catch (BalanceInsufficientException e) {
      fail("BalanceInsufficientException");
    }
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    Assert.assertEquals(allowance, accountWrapper.getAllowance());
    Assert.assertEquals(0, accountWrapper.getLatestWithdrawTime());

    WitnessWrapper witnessCapsule = new WitnessWrapper(ByteString.copyFrom(address),
        100, "http://baidu.com");
    dbManager.getWitnessStore().put(address, witnessCapsule);

    contract.execute(new byte[0]);
    AccountWrapper witnessAccount =
        dbManager.getAccountStore().get(ByteArray.fromHexString(WITNESS_ADDRESS));
    Assert.assertEquals(initBalance + allowance, witnessAccount.getBalance());
    Assert.assertEquals(0, witnessAccount.getAllowance());
    Assert.assertNotEquals(0, witnessAccount.getLatestWithdrawTime());
  }


  @Test
  public void proposalTest() {

    try {
      /*
       *  create proposal Test
       */
      DataWord key = new DataWord(
          "0000000000000000000000000000000000000000000000000000000000000000");
      // 1000000 == 0xF4240
      DataWord value = new DataWord(
          "00000000000000000000000000000000000000000000000000000000000F4240");
      byte[] data4Create =  new byte[64];
      System.arraycopy(key.getData(),0,data4Create,0,key.getData().length);
      System.arraycopy(value.getData(),0,data4Create,key.getData().length,value.getData().length);

      PrecompiledContract createContract = createPrecompiledContract(proposalCreateAddr,WITNESS_ADDRESS);

      Assert.assertEquals(0, dbManager.getDynamicPropertiesStore().getLatestProposalNum());
      ProposalWrapper proposalWrapper;
      byte[] idBytes = createContract.execute(data4Create).getRight();
      long id = ByteUtil.byteArrayToLong(idBytes);
      proposalWrapper = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(proposalWrapper);
      Assert.assertEquals(1, dbManager.getDynamicPropertiesStore().getLatestProposalNum());
      Assert.assertEquals(0, proposalWrapper.getApprovals().size());
      Assert.assertEquals(1000000, proposalWrapper.getCreateTime());
      Assert.assertEquals(261200000, proposalWrapper.getExpirationTime()
          ); // 2000000 + 3 * 4 * 21600000



      /*
       *  approve proposal Test
       */

      byte[] data4Approve = new byte[64];
      DataWord isApprove = new DataWord("0000000000000000000000000000000000000000000000000000000000000001");
      System.arraycopy(idBytes,0,data4Approve,0,idBytes.length);
      System.arraycopy(isApprove.getData(),0,data4Approve,idBytes.length,isApprove.getData().length);
      PrecompiledContract approveContract = createPrecompiledContract(proposalApproveAddr,WITNESS_ADDRESS);
      approveContract.execute(data4Approve);
      proposalWrapper = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertEquals(1, proposalWrapper.getApprovals().size());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS)),
          proposalWrapper.getApprovals().get(0));

      /*
       *  delete proposal Test
       */
      PrecompiledContract deleteContract = createPrecompiledContract(proposalDeleteAddr,WITNESS_ADDRESS);
      deleteContract.execute(idBytes);
      proposalWrapper = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertEquals(State.CANCELED, proposalWrapper.getState());

    } catch (ItemNotFoundException e) {
      Assert.fail();
    }
  }


  @Test
  public void convertFromgscBytesAddressNativeTest() {
    PrecompiledContract contract = createPrecompiledContract(convertFromgscBytesAddressAddr, WITNESS_ADDRESS);
    byte[] solidityAddress = contract.execute(new DataWord(WITNESS_ADDRESS).getData()).getRight();
    Assert.assertArrayEquals(solidityAddress,new DataWord(Hex.decode(WITNESS_ADDRESS_BASE)).getData());
  }

  @Test
  public void convertFromgscBase58AddressNative() {
    // 27WnTihwXsqCqpiNedWvtKCZHsLjDt4Hfmf  TestNet address
    DataWord word1 = new DataWord("3237576e54696877587371437170694e65645776744b435a48734c6a44743448");
    DataWord word2 = new DataWord("666d660000000000000000000000000000000000000000000000000000000000");

    byte[] data = new byte[35];
    System.arraycopy(word1.getData(),0,data,0, word1.getData().length);
    System.arraycopy(Arrays.copyOfRange(word2.getData(), 0, 3),0,data,word1.getData().length,3);
    PrecompiledContract contract = createPrecompiledContract(convertFromgscBase58AddressAddr, WITNESS_ADDRESS);

    byte[] solidityAddress = contract.execute(data).getRight();
    Assert.assertArrayEquals(solidityAddress,new DataWord(Hex.decode(WITNESS_ADDRESS_BASE)).getData());
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
