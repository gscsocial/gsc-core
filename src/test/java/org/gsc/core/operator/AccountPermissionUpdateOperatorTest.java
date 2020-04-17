/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.core.operator;

import static org.testng.Assert.fail;

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
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AccountPermissionUpdateContract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Key;
import org.gsc.protos.Protocol.Permission;
import org.gsc.protos.Protocol.Permission.PermissionType;
import org.gsc.protos.Protocol.Transaction.Result.code;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
public class AccountPermissionUpdateOperatorTest {

  private static Manager dbManager;
  private static final String dbPath = "db_transfer_test";
  private static GSCApplicationContext context;
  public static Application AppT;

  private static final String OWNER_ADDRESS;
  private static final String WITNESS_ADDRESS;
  private static final String KEY_ADDRESS;
  private static final String KEY_ADDRESS1;
  private static final String KEY_ADDRESS2;
  private static final String KEY_ADDRESS3;
  private static final String KEY_ADDRESS4;
  private static final String KEY_ADDRESS5;
  private static final Key VALID_KEY;
  private static final Key VALID_KEY1;
  private static final Key VALID_KEY2;
  private static final Key VALID_KEY3;
  private static final Key VALID_KEY4;
  private static final Key VALID_KEY5;
  private static final long KEY_WEIGHT = 2;

  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOUNT;
  private static final String KEY_ADDRESS_INVALID = "bbbb";

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    WITNESS_ADDRESS = Wallet.getAddressPreFixString() + "8CFC572CC20CA18B636BDD93B4FB15EA84CC2B4E";
    KEY_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    KEY_ADDRESS1 = Wallet.getAddressPreFixString() + "BCE23C7D683B889326F762DDA2223A861EDA2E5C";
    KEY_ADDRESS2 = Wallet.getAddressPreFixString() + "B207296C464175C5124AD6DEBCE3E9EB3720D9EA";
    KEY_ADDRESS3 = Wallet.getAddressPreFixString() + "5FFAA69423DC87903948E788E0D5A7BE9BE58989";
    KEY_ADDRESS4 = Wallet.getAddressPreFixString() + "A727FD9B876A1040B14A7963AFDA8490ED2A2F00";
    KEY_ADDRESS5 = Wallet.getAddressPreFixString() + "474921F5AD0ACE57D8AFD7E878F38DB7C3977361";

    OWNER_ADDRESS_NOACCOUNT =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";

    VALID_KEY =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY1 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS1)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY2 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS2)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY3 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS3)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY4 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS4)))
            .setWeight(KEY_WEIGHT)
            .build();
    VALID_KEY5 =
        Key.newBuilder()
            .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS5)))
            .setWeight(KEY_WEIGHT)
            .build();
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    dbManager.getDynamicPropertiesStore().saveAllowMultiSign(1);
    dbManager.getDynamicPropertiesStore().saveTotalSignNum(5);
  }

  /**
   * create temp Wrapper test need.
   */
  @Before
  public void createWrapper() {
    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8("owner"),
            AccountType.Normal);
    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);

    AccountWrapper witnessWrapper =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS)),
            ByteString.copyFromUtf8("witness"),
            AccountType.Normal);
    witnessWrapper.setIsWitness(true);
    dbManager.getAccountStore().put(witnessWrapper.getAddress().toByteArray(), witnessWrapper);
  }

  /**
   * contract with default permissions
   */
  private Any getContract(String ownerAddress) {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(ownerAddress));
    Permission owner = AccountWrapper.createDefaultOwnerPermission(address);
    Permission active = AccountWrapper.createDefaultActivePermission(address, dbManager);

    Contract.AccountPermissionUpdateContract contract =
        Contract.AccountPermissionUpdateContract.newBuilder()
            .setOwnerAddress(address)
            .setOwner(owner)
            .addActives(active)
            .build();
    return Any.pack(contract);
  }

  private Any getContract(ByteString address, Permission owner, Permission witness,
      List<Permission> activeList) {
    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();
    builder.setOwnerAddress(address);
    if (owner != null) {
      builder.setOwner(owner);
    }
    if (witness != null) {
      builder.setWitness(witness);
    }
    if (activeList != null) {
      builder.addAllActives(activeList);
    }
    return Any.pack(builder.build());
  }

  private Any getInvalidContract() {
    return Any.pack(
        Contract.AccountCreateContract.newBuilder()
            .build());
  }

  private void addDefaultPermission() {
    byte[] owner_name_array = ByteArray.fromHexString(OWNER_ADDRESS);
    AccountWrapper account = dbManager.getAccountStore().get(owner_name_array);

    Permission owner = AccountWrapper.createDefaultOwnerPermission(account.getAddress());
    Permission active = AccountWrapper
        .createDefaultActivePermission(account.getAddress(), dbManager);
    List<Permission> activeList = new ArrayList<>();
    activeList.add(active);
    account.updatePermissions(owner, null, activeList);

    dbManager.getAccountStore().put(owner_name_array, account);
  }


  private void processAndCheckInvalid(
      AccountPermissionUpdateOperator operator,
      TransactionResultWrapper ret,
      String failMsg,
      String expectedMsg) {
    try {
      operator.validate();
      operator.execute(ret);

      fail(failMsg);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(expectedMsg, e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void successUpdatePermissionKey() {
    String ownerAddress = OWNER_ADDRESS;
    String keyAddress = KEY_ADDRESS;

    // step 1, init
    addDefaultPermission();

    // step2, check init data
    byte[] owner_name_array = ByteArray.fromHexString(ownerAddress);
    ByteString address = ByteString.copyFrom(owner_name_array);
    AccountWrapper owner = dbManager.getAccountStore().get(owner_name_array);

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
    Permission ownerPermission1 = owner.getInstance().getOwnerPermission();
    Permission activePermission1 = owner.getInstance().getActivePermission(0);

    Assert.assertEquals(ownerPermission, ownerPermission1);
    Assert.assertEquals(activePermission, activePermission1);

    // step 3, execute update
    // add account
    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(keyAddress)),
            ByteString.copyFromUtf8("active"),
            AccountType.Normal);
    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);

    owner.setBalance(1000_000_000L);
    dbManager.getAccountStore().put(owner.getAddress().toByteArray(), owner);

    ownerPermission =
        Permission.newBuilder()
            .setType(PermissionType.Owner)
            .setPermissionName("owner")
            .setThreshold(2)
            .addKeys(Key.newBuilder().setAddress(address).setWeight(4).build())
            .addKeys(
                Key.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                    .setWeight(5)
                    .build())
            .build();
    activePermission =
        Permission.newBuilder()
            .setType(PermissionType.Active)
            .setId(2)
            .setPermissionName("active")
            .setThreshold(2)
            .setOperations(ByteString.copyFrom(ByteArray
                .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
            .addKeys(Key.newBuilder().setAddress(address).setWeight(2).build())
            .addKeys(
                Key.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(keyAddress)))
                    .setWeight(3)
                    .build())
            .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);

      // step 4, check result after update operation
      owner = dbManager.getAccountStore().get(owner_name_array);
      Assert.assertEquals(owner.getInstance().getActivePermissionCount(), 1);
      ownerPermission1 = owner.getInstance().getOwnerPermission();
      activePermission1 = owner.getInstance().getActivePermission(0);

      Assert.assertEquals(ownerPermission1, ownerPermission);
      Assert.assertEquals(activePermission1, activePermission);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void nullContract() {
    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(null, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "No contract!",
        "No contract!");
  }

  @Test
  public void nullDbManager() {
    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(OWNER_ADDRESS), null);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "No dbManager!",
        "No dbManager!");
  }

  @Test
  public void invalidContract() {
    Any invalidContract = getInvalidContract();
    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(invalidContract, dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "contract type error",
        "contract type error,expected type [AccountPermissionUpdateContract],real type["
            + invalidContract.getClass()
            + "]");
  }

  @Test
  public void invalidOwnerAddress() {
    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(OWNER_ADDRESS_INVALID), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(operator, ret, "invalidate ownerAddress", "invalidate ownerAddress");
  }

  @Test
  public void nullAccount() {
    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(OWNER_ADDRESS_NOACCOUNT), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "ownerAddress account does not exist",
        "ownerAddress account does not exist");
  }

  @Test
  public void ownerMissed() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);
    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, null, null, activeList), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(operator, ret, "owner permission is missed",
        "owner permission is missed");
  }

  @Test
  public void activeMissed() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, null, null), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator, ret, "active permission is missed", "active permission is missed");
  }

  @Test
  public void activeToMany() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    for (int i = 0; i <= 8; i++) {
      activeList.add(activePermission);
    }

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, null, null), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator, ret, "active permission is missed", "active permission is missed");
  }

  @Test
  public void witnessNeedless() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission witnessPermission = AccountWrapper.createDefaultWitnessPermission(address);
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, witnessPermission, activeList), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator, ret, "account isn't witness can't set witness permission",
        "account isn't witness can't set witness permission");
  }

  @Test
  public void witnessMissed() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));
    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, null, activeList), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(operator, ret, "witness permission is missed",
        "witness permission is missed");
  }

  @Test
  public void invalidOwnerPermissionType() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder().setType(PermissionType.Active)
        .setPermissionName("owner").setThreshold(1).setParentId(0).build();
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "owner permission type is error",
        "owner permission type is error");
  }

  @Test
  public void invalidActivePermissionType() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder().setPermissionName("witness")
        .setThreshold(1).setParentId(0).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, null, activeList), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(operator, ret, "active permission type is error",
        "active permission type is error");
  }

  @Test
  public void invalidWitnessPermissionType() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission witnessPermission =
        Permission.newBuilder().setPermissionName("witness").setThreshold(1).setParentId(0).build();
    Permission activePermission = AccountWrapper.createDefaultWitnessPermission(address);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, witnessPermission, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "witness permission type is error",
        "witness permission type is error");
  }

  @Test
  public void ownerPermissionNoKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder().setPermissionName("owner").setThreshold(1)
        .build();
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "key's count should be greater than 0",
        "key's count should be greater than 0");
  }

  @Test
  public void ownerPermissionToManyKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder()
        .setPermissionName("owner")
        .addKeys(VALID_KEY)
        .addKeys(VALID_KEY1)
        .addKeys(VALID_KEY2)
        .addKeys(VALID_KEY3)
        .addKeys(VALID_KEY4)
        .addKeys(VALID_KEY5)
        .setThreshold(1)
        .build();
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "number of keys in permission should not be greater than 5",
        "number of keys in permission should not be greater than 5");
  }

  @Test
  public void activePermissionNoKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission =
        Permission.newBuilder().setType(PermissionType.Active).setPermissionName("active")
            .setThreshold(1).setParentId(0).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "key's count should be greater than 0",
        "key's count should be greater than 0");
  }

  @Test
  public void activePermissionToManyKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder()
        .setType(PermissionType.Active)
        .setPermissionName("active")
        .addKeys(VALID_KEY)
        .addKeys(VALID_KEY1)
        .addKeys(VALID_KEY2)
        .addKeys(VALID_KEY3)
        .addKeys(VALID_KEY4)
        .addKeys(VALID_KEY5)
        .setThreshold(1)
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "number of keys in permission should not be greater than 5",
        "number of keys in permission should not be greater than 5");
  }

  @Test
  public void witnessPermissionNoKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);
    Permission witnessPermission =
        Permission.newBuilder().setType(PermissionType.Witness).setPermissionName("active")
            .setThreshold(1).setParentId(0).build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, witnessPermission, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "key's count should be greater than 0",
        "key's count should be greater than 0");
  }

  @Test
  public void witnessPermissionToManyKey() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);
    Permission witnessPermission = Permission.newBuilder()
        .setType(PermissionType.Witness)
        .setPermissionName("witness")
        .addKeys(VALID_KEY)
        .addKeys(VALID_KEY1)
        .addKeys(VALID_KEY2)
        .addKeys(VALID_KEY3)
        .addKeys(VALID_KEY4)
        .addKeys(VALID_KEY5)
        .setThreshold(1)
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, witnessPermission, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "number of keys in permission should not be greater than 5",
        "number of keys in permission should not be greater than 5");
  }

  @Test
  public void witnessPermissionToManyKey1() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);
    Permission witnessPermission = Permission.newBuilder()
        .setType(PermissionType.Witness)
        .setPermissionName("witness")
        .addKeys(VALID_KEY)
        .addKeys(VALID_KEY1)
        .addKeys(VALID_KEY2)
        .addKeys(VALID_KEY3)
        .addKeys(VALID_KEY4)
        .setThreshold(1)
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, witnessPermission, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "Witness permission's key count should be 1",
        "Witness permission's key count should be 1");
  }

  @Test
  public void invalidThreshold() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission =
        Permission.newBuilder()
            .setPermissionName("owner")
            .setThreshold(0)
            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
            .build();
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "permission's threshold should be greater than 0",
        "permission's threshold should be greater than 0");
  }

  @Test
  public void permissionNameTooLong() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission =
        Permission.newBuilder()
            .setThreshold(1)
            .setPermissionName("0123456789ABCDEF0123456789ABCDEF0")
            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
            .build();
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "permission's name is too long",
        "permission's name is too long");
  }

  @Test
  public void invalidPermissionParent() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission =
        Permission.newBuilder()
            .setType(PermissionType.Active)
            .setPermissionName("active")
            .setParentId(1)
            .setThreshold(1)
            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
            .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "permission's parent should be owner",
        "permission's parent should be owner");
  }

  @Test
  public void addressNotDistinctInPermission() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission =
        Permission.newBuilder()
            .setType(PermissionType.Active)
            .setPermissionName("active")
            .setParentId(0)
            .setThreshold(1)
            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
            .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "address should be distinct in permission",
        "address should be distinct in permission Active");
  }

  @Test
  public void invalidKeyAddress() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission =
        Permission.newBuilder()
            .setType(PermissionType.Active)
            .setPermissionName("active")
            .setParentId(0)
            .setThreshold(1)
            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
            .addKeys(
                Key.newBuilder()
                    .setAddress(ByteString.copyFrom(ByteArray.fromHexString(KEY_ADDRESS_INVALID)))
                    .setWeight(1)
                    .build())
            .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator, ret, "key is not a validate address", "key is not a validate address");
  }


  @Test
  public void weighValueInvalid() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission =
        Permission.newBuilder()
            .setType(PermissionType.Active)
            .setPermissionName("active")
            .setParentId(0)
            .setThreshold(1)
            .addKeys(Key.newBuilder().setAddress(address).setWeight(0).build())
            .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "key's weight should be greater than 0",
        "key's weight should be greater than 0");
  }

  @Test
  public void sumWeightLessThanThreshold() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission =
        Permission.newBuilder()
            .setType(PermissionType.Active)
            .setPermissionName("active")
            .setParentId(0)
            .setThreshold(2)
            .addKeys(Key.newBuilder().setAddress(address).setWeight(1).build())
            .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "sum of all keys weight should not be less that threshold",
        "sum of all key's weight should not be less than threshold in permission Active");
  }

  @Test
  public void onwerPermissionOperationNeedless() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = Permission.newBuilder()
        .setType(PermissionType.Owner)
        .setPermissionName("owner")
        .setThreshold(1)
        .setOperations(ByteString.copyFrom(ByteArray
            .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
        .setParentId(0)
        .addKeys(VALID_KEY)
        .build();
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "Owner permission needn't operations",
        "Owner permission needn't operations");
  }

  @Test
  public void activePermissionNoOperation() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder()
        .setType(PermissionType.Active)
        .setPermissionName("active")
        .setThreshold(1)
        .setParentId(0)
        .addKeys(VALID_KEY)
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "operations size must 32",
        "operations size must 32");
  }

  @Test
  public void activePermissionInvalidOperationSize() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder()
        .setType(PermissionType.Active)
        .setPermissionName("active")
        .setThreshold(1)
        .setOperations(ByteString.copyFrom(ByteArray
            .fromHexString("00000000000000000000000000000000000000000000000000000000000000")))
        .setParentId(0)
        .addKeys(VALID_KEY)
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "operations size must 32",
        "operations size must 32");
  }

  @Test
  public void activePermissionInvalidOperationBit() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = Permission.newBuilder()
        .setType(PermissionType.Active)
        .setPermissionName("active")
        .setThreshold(1)
        .setOperations(ByteString.copyFrom(ByteArray
            .fromHexString("8000000000000000000000000000000000000000000000000000000000000000")))
        .setParentId(0)
        .addKeys(VALID_KEY)
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(getContract(address, ownerPermission, null, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "7 isn't a validate ContractType",
        "7 isn't a validate ContractType");
  }

  @Test
  public void witnessPermissionOperationNeedless() {
    ByteString address = ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS));

    Permission ownerPermission = AccountWrapper.createDefaultOwnerPermission(address);
    Permission activePermission = AccountWrapper.createDefaultActivePermission(address, dbManager);
    Permission witnessPermission = Permission.newBuilder()
        .setType(PermissionType.Witness)
        .setPermissionName("witness")
        .setThreshold(1)
        .setOperations(ByteString.copyFrom(ByteArray
            .fromHexString("0000000000000000000000000000000000000000000000000000000000000000")))
        .setParentId(0)
        .addKeys(VALID_KEY)
        .build();

    List<Permission> activeList = new ArrayList<>();
    activeList.add(activePermission);

    AccountPermissionUpdateOperator operator =
        new AccountPermissionUpdateOperator(
            getContract(address, ownerPermission, witnessPermission, activeList),
            dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    processAndCheckInvalid(
        operator,
        ret,
        "Witness permission needn't operations",
        "Witness permission needn't operations");
  }

  @Test
  public void checkAvailableContractTypeCorrespondingToCode() {
    // note: The aim of this test case is to show how the current codes work.
    // The default value is 7fff1fc0037e0000000000000000000000000000000000000000000000000000,
    // and it should call the addSystemContractAndSetPermission to add new contract type
    String validContractType = "7fff1fc0037e0000000000000000000000000000000000000000000000000000";

    byte[] availableContractType = new byte[32];
    for (ContractType contractType : ContractType.values()) {
      if (contractType == org.gsc.protos.Protocol.Transaction.Contract.ContractType.UNRECOGNIZED
          || contractType == ContractType.ClearABIContract
          //|| contractType == ContractType.CancelDeferredTransactionContract
      ) {
        continue;
      }
      int id = contractType.getNumber();
      System.out.println("id is " + id);
      availableContractType[id / 8] |= (1 << id % 8);
    }

    System.out.println(ByteArray.toHexString(availableContractType));

    Assert.assertEquals(ByteArray.toHexString(availableContractType), validContractType);

  }

  @Test
  public void checkActiveDefaultOperationsCorrespondingToCode() {
    // note: The aim of this test case is to show how the current codes work.
    // The default value is 7fff1fc0033e0000000000000000000000000000000000000000000000000000,
    // and it should call the addSystemContractAndSetPermission to add new contract type
    String validContractType = "7fff1fc0033e0000000000000000000000000000000000000000000000000000";

    byte[] availableContractType = new byte[32];
    for (ContractType contractType : ContractType.values()) {
      if (contractType == org.gsc.protos.Protocol.Transaction.Contract.ContractType.UNRECOGNIZED
          || contractType == ContractType.AccountPermissionUpdateContract
          || contractType == ContractType.ClearABIContract
          //|| contractType == ContractType.CancelDeferredTransactionContract
      ) {
        continue;
      }
      int id = contractType.getNumber();
      System.out.println("id is " + id);
      availableContractType[id / 8] |= (1 << id % 8);
    }

    System.out.println(ByteArray.toHexString(availableContractType));

    Assert.assertEquals(ByteArray.toHexString(availableContractType), validContractType);

  }


  @Test
  public void checkAvailableContractType() {
    String validContractType = "7fff1fc0037e0100000000000000000000000000000000000000000000000000";

    byte[] availableContractType = new byte[32];
    for (ContractType contractType : ContractType.values()) {
      if (contractType == org.gsc.protos.Protocol.Transaction.Contract.ContractType.UNRECOGNIZED) {
        continue;
      }
      int id = contractType.getNumber();
      System.out.println("id is " + id);
      availableContractType[id / 8] |= (1 << id % 8);
    }

    System.out.println(ByteArray.toHexString(availableContractType));

    Assert.assertEquals(ByteArray.toHexString(availableContractType), validContractType);

  }

  @Test
  public void checkActiveDefaultOperations() {
    String validContractType = "7fff1fc0033e0100000000000000000000000000000000000000000000000000";

    byte[] availableContractType = new byte[32];
    for (ContractType contractType : ContractType.values()) {
      if (contractType == org.gsc.protos.Protocol.Transaction.Contract.ContractType.UNRECOGNIZED
          || contractType == ContractType.AccountPermissionUpdateContract) {
        continue;
      }
      int id = contractType.getNumber();
      System.out.println("id is " + id);
      availableContractType[id / 8] |= (1 << id % 8);
    }

    System.out.println(ByteArray.toHexString(availableContractType));

    Assert.assertEquals(ByteArray.toHexString(availableContractType), validContractType);

  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}