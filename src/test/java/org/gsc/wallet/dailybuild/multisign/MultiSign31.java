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

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.GrpcAPI.TransactionSignWeight;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Permission;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign31 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private final String testWitnesses = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;


  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private ManagedChannel channelConfirmed = null;
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] test002Address = ecKey2.getAddress();
  private String sendAccountKey2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] test003Address = ecKey3.getAddress();
  String sendAccountKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] test004Address = ecKey4.getAddress();
  String sendAccountKey4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] test005Address = ecKey5.getAddress();
  String sendAccountKey5 = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);


  }


  @Test(enabled = true, description =
      "Sendcoin,use active address sign,  meet all requirements.Then use the same"
          + " permissionID active address to sign, meet all requirements,broadcastTransaction.")
  public void testMultiUpdatepermissions_34() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1000000;
    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    long balance = test001AddressAccount.getBalance();
    logger.info("balance:" + balance);
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":\""
        + "owner\",\"threshold\":1,\"keys\":[{\"address\":\""
        + "" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name"
        + "\":\"active0\",\"threshold\":1,\"operations\":\""
        + "0200000000000000000000000000000000000000000000000000000000000000\""
        + ",\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2) + "\","
        + "\"weight\":1},{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    long balance1 = test001AddressAccount1.getBalance();
    logger.info("balance1:" + balance1);

    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1L, test001Address, 2, dev001Key,
            blockingStubFull);

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    logger.info("returnResult1:" + returnResult1);
    Assert.assertTrue(returnResult1.getResult());
    Account test001AddressAccount2 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    logger.info("balance2:" + balance2);

    Assert.assertEquals(balance1 - balance2, 1L);
    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance3, balance2);


  }


  @Test(enabled = true, description =
      "Sendcoin,use active address sign,not meet the requirements.Then use the same"
          + " permissionID active address to sign,broadcastTransaction.")
  public void testMultiUpdatepermissions_35() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    final long balance = test001AddressAccount.getBalance();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":\""
        + "owner\",\"threshold\":1,\"keys\":[{\"address\":\""
        + "" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name"
        + "\":\"active0\",\"threshold\":1,\"operations\":\""
        + "0100000000000000000000000000000000000000000000000000000000000000\""
        + ",\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2) + "\","
        + "\"weight\":1},{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1L, test001Address, 2, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission denied"));
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    Account test001AddressAccount2 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    logger.info("balance2:" + balance2);
    Assert.assertEquals(balance1, balance2);

    logger.info("returnResult1:" + returnResult1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(),
            containsString("validate signature error Permission denied"));
    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);

    logger.info("transaction1:" + transactionSignWeight1);

    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Permission denied"));

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    logger.info("balance3:" + balance3);
    Assert.assertEquals(balance3, balance2);


  }

  @Test(enabled = true, description =
      "Sendcoin,use owner address sign,Then use owner address to sign,broadcastTransaction.")
  public void testMultiUpdatepermissions_36() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    final long balance = test001AddressAccount.getBalance();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\":"
        + "\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
        + "\"threshold\":1,\"operations\":"
        + "\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1, test001Address, 0, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);

    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(returnResult1.getResult());

    logger.info("returnResult1:" + returnResult1);
    Account test001AddressAccount2 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, 1);
    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult1:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2, balance3);
  }

  @Test(enabled = true, description =
      "Sendcoin,use active address sign,not meet requirements,Then use the other permissionID "
          + "in active address to sign, meet all requirements.broadcastTransaction.")
  public void testMultiUpdatepermissions_37() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    final long balance = test001AddressAccount.getBalance();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1L, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(),
            containsString("but it is not contained of permission"));
    Account test001AddressAccount2 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1, balance2);
    //Assert.assertTrue(returnResult1.getResult());
    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey3, 3, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Signature count is 2 more than key counts of permission : 1"));

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2, balance3);

  }

  @Test(enabled = true, description =
      "Sendcoin,use active address sign, meet all requirements,Then use the other permissionID "
          + "in active address to sign,not meet the requirements.broadcastTransaction.")
  public void testMultiUpdatepermissions_38() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 2;

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    final long balance = test001AddressAccount.getBalance();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    logger.info("returnResult1:" + returnResult1);
    Account test001AddressAccount2 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, 1);

    Transaction transaction2 = PublicMethed
        .addTransactionSign(transaction1, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("transaction1:" + transactionSignWeight1);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Signature count is 2 more than key counts of permission : 1"));

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("DUP_TRANSACTION_ERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance3, balance2);

  }

  @Test(enabled = true, description =
      "Sendcoin,use active address sign, meet all requirements,Then use the other permissionID "
          + "in active address to sign, meet all requirements.broadcastTransaction.")
  public void testMultiUpdatepermissions_39() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 2;

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    final long balance = test001AddressAccount.getBalance();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1L, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    Account test001AddressAccount2 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, 1L);

    logger.info("returnResult1:" + returnResult1);
    Assert.assertTrue(returnResult1.getResult());
    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 2, blockingStubFull);

    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Signature count is 2 more than key counts of permission : 1"));
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance3, balance2);

  }

  @Test(enabled = true, description =
      "Sendcoin,use active address sign, not meet the requirements,Then use the other permissionID "
          + "in active address to sign,not meet the requirements.broadcastTransaction.")
  public void testMultiUpdatepermissions_40() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 2;

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    final long balance = test001AddressAccount.getBalance();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1L, test001Address, 3, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey3, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission denied"));
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    Account test001AddressAccount2 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1, balance2);
    logger.info("returnResult1:" + returnResult1);
    Assert
        .assertThat(returnResult1.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult1.getMessage().toStringUtf8(),
            containsString("Permission denied"));
    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey2, 2, blockingStubFull);

    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Permission denied"));

    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult2:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2, balance3);

  }

  @Test(enabled = true, description =
      "Sendcoin,use owner address sign,  meet all requirements,Then use active address to sign"
          + ",not meet the requirements.broadcastTransaction.")
  public void testMultiUpdatepermissions_41() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = updateAccountPermissionFee + 2;

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    final long balance = test001AddressAccount.getBalance();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\":"
        + "\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
        + "\"threshold\":1,\"operations\":"
        + "\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1L, test001Address, 0, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transaction:" + transactionSignWeight);
    Return returnResult1 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    Assert.assertTrue(returnResult1.getResult());
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("returnResult1:" + returnResult1);
    Account test001AddressAccount2 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, 1);
    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, sendAccountKey3, 2, blockingStubFull);
    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight1.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight1.getResult().getMessage(),
            containsString("Signature count is 2 more than key counts of permission : 1"));
    logger.info("transaction1:" + transactionSignWeight1);

    Return returnResult2 = PublicMethedForMutiSign
        .broadcastTransaction1(transaction2, blockingStubFull);
    logger.info("returnResult1:" + returnResult2);
    Assert
        .assertThat(returnResult2.getCode().toString(), containsString("SIGERROR"));
    Account test001AddressAccount3 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance3, balance2);
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
