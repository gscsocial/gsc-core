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

package org.gsc.wallet.dailybuild.multisign;

import static org.hamcrest.CoreMatchers.containsString;
import static org.gsc.api.GrpcAPI.TransactionSignWeight.Result.response_code.ENOUGH_PERMISSION;
import static org.gsc.api.GrpcAPI.TransactionSignWeight.Result.response_code.NOT_ENOUGH_PERMISSION;
import static org.gsc.api.GrpcAPI.TransactionSignWeight.Result.response_code.OTHER_ERROR;
import static org.gsc.api.GrpcAPI.TransactionSignWeight.Result.response_code.PERMISSION_ERROR;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.TransactionExtention;
import org.gsc.api.GrpcAPI.TransactionSignWeight;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign25 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress001 = PublicMethed.getFinalAddress(witnessKey001);

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] ownerAddress = ecKey1.getAddress();
  private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] normalAddr001 = ecKey2.getAddress();
  private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private ECKey tmpEcKey01 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr01 = tmpEcKey01.getAddress();
  private String tmpKey01 = ByteArray.toHexString(tmpEcKey01.getPrivKeyBytes());

  private ECKey tmpEcKey02 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr02 = tmpEcKey02.getAddress();
  private String tmpKey02 = ByteArray.toHexString(tmpEcKey02.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private static final String AVAILABLE_OPERATION
      = "7fff1fc0037e0000000000000000000000000000000000000000000000000000";
  private static final String DEFAULT_OPERATION
      = "7fff1fc0033e0000000000000000000000000000000000000000000000000000";

  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
  }

  @Test(enabled = true, description = "Get sign for multi sign normal transaction")
  public void test01GetSignMultiSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long amount = 2 * updateAccountPermissionFee + 100000000;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(ownerKey);
    Account test001AddressAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    final long balance = test001AddressAccount.getBalance();

    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,"
        + "\"permission_name\":\"owner1\",\"threshold\":2,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);
    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, tmpKey02, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before Sign2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(1, txWeight.getCurrentWeight());

    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, testKey002, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction2, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    PublicMethedForMutiSign
        .recoverAccountPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("After recover permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertEquals("Signature count is 2 more than key counts of permission : 1",
        txWeight.getResult().getMessage());
    Account test001AddressAccount1 = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, 2 * updateAccountPermissionFee + 2 * multiSignFee + 1);
  }

  @Test(enabled = true, description = "Get sign for multi sign permission transaction")
  public void test02GetSignMultiSignPermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    long amount = 4 * updateAccountPermissionFee + 100000000;

    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    Integer[] ints = {ContractType.AccountPermissionUpdateContract_VALUE};
    String operations = PublicMethedForMutiSign.getOperations(ints);
    Account test001AddressAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    final long balance = test001AddressAccount.getBalance();
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + operations + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Transaction transaction = PublicMethedForMutiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before sign permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, testKey002, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before sign2 permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("Before broadcast permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(5, txWeight.getCurrentWeight());

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction2, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    PublicMethedForMutiSign
        .recoverAccountPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("After recover permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertEquals("Signature count is 2 more than key counts of permission : 1",
        txWeight.getResult().getMessage());
    Account test001AddressAccount1 = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, 3 * updateAccountPermissionFee + multiSignFee);
  }

  @Test(enabled = true, description = "Get sign for single sign normal transaction")
  public void test03GetSignSingleSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long amount = 2 * updateAccountPermissionFee + 100000000;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);
    Account test001AddressAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    final long balance = test001AddressAccount.getBalance();
    Integer[] ints = {ContractType.TransferContract_VALUE};
    String operations = PublicMethedForMutiSign.getOperations(ints);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + operations + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethedForMutiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, testKey002, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    PublicMethedForMutiSign
        .recoverAccountPermission(ownerKey, ownerPermissionKeys, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After recover permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("but it is not contained of permission"));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, 2 * updateAccountPermissionFee + 1000_000);
  }

  @Test(enabled = true, description = "Get sign for not sign transaction")
  public void test04GetSignNotSignPermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long amount = 2 * updateAccountPermissionFee + 100000000;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);
    Account test001AddressAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    final long balance = test001AddressAccount.getBalance();
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Transaction transaction = PublicMethedForMutiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethedForMutiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Account test001AddressAccount1 = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

  }

  @Test(enabled = true, description = "Get sign for not complete multi sign normal transaction")
  public void test05GetSignMultiSignNotCompletePermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long amount = 4 * updateAccountPermissionFee + 100000000;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);
    Account test001AddressAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    final long balance = test001AddressAccount.getBalance();
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Transaction transaction = PublicMethedForMutiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethedForMutiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, testKey002, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());
    Account test001AddressAccount1 = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
  }

  @Test(enabled = true, description = "Get sign for failed transaction")
  public void test06GetSignFailedTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long amount = 4 * updateAccountPermissionFee + 100000000;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);
    Account test001AddressAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    final long balance = test001AddressAccount.getBalance();
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethedForMutiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, tmpKey02, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("but it is not contained of permission"));

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(
        transaction1, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("but it is not contained of permission"));
    Account test001AddressAccount1 = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

  }

  @Test(enabled = true, description = "Get sign for timeout normal transaction")
  public void test07GetSignTimeoutTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long amount = 4 * updateAccountPermissionFee + 100000000;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.printAddress(ownerKey);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(ownerKey);
    Account test001AddressAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    final long balance = test001AddressAccount.getBalance();
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethedForMutiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, testKey002, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    try {
      Thread.sleep(70000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());
    Account test001AddressAccount1 = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
  }

  @Test(enabled = true, description = "Get sign for empty transaction")
  public void test08GetSignEmptyTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long amount = 4 * updateAccountPermissionFee + 100000000;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.printAddress(ownerKey);
    Account test001AddressAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    final long balance = test001AddressAccount.getBalance();
    logger.info("** created an empty transaction");

    Contract.AccountPermissionUpdateContract.Builder builder =
        Contract.AccountPermissionUpdateContract.newBuilder();

    Contract.AccountPermissionUpdateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    Transaction transaction = transactionExtention.getTransaction();

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(OTHER_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertEquals("class java.lang.IndexOutOfBoundsException : Index: 0",
        txWeight.getResult().getMessage());

    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("After recover permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(OTHER_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertEquals("class java.lang.IndexOutOfBoundsException : Index: 0",
        txWeight.getResult().getMessage());
    Account test001AddressAccount1 = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance, balance1);
  }

  @Test(enabled = true, description = "Get sign for error transaction")
  public void test09GetSignErrorTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long amount = 4 * updateAccountPermissionFee + 100000000;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, amount, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);
    Account test001AddressAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    final long balance = test001AddressAccount.getBalance();
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
            + "\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"" + AVAILABLE_OPERATION + "\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a fake transaction");
    Transaction transaction = PublicMethedForMutiSign
        .createFakeTransaction(ownerAddress, 1_000_000L, ownerAddress);

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, testKey002, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
    Account test001AddressAccount1 = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, 1 * updateAccountPermissionFee);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
