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

package org.gsc.wallet.onlinestress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Permission;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class CycleMultiSign {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);


  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;

  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmedInFullnode = null;

  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private ManagedChannel channelConfirmed = null;
  private ManagedChannel channelConfirmedInFullnode = null;
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);


  private ECKey ecKey = new ECKey(Utils.getRandom());
  private byte[] test001Address = ecKey.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());


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

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext(true)
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
        .usePlaintext(true)
        .build();
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);

  }

  //(use no id)
  @Test(enabled = true)
  public void testMultiSignActiveAddress() {
    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 10000000000000L, fromAddress, testKey002,
            blockingStubFull));

    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    logger.info("wei-----------------------");

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\""
            + "keys\":[{\"address\":\"" + PublicMethed.getAddressString(dev001Key)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey5)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\":\""
            + "0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(dev001Key)
            + "\",\"weight\":1}]}]} ";

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    logger.info("1-----------------------");

    String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    String accountPermissionJson3 = "{\"owner_permission\":{\"type\":0,\"permission_name\":"
        + "\"owner\",\"threshold\":1,\"keys\":[{\"address\""
        + ":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
        + "\"active0\",\"threshold\":1,\"operations"
        + "\":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(dev001Key)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations"
        + "\":\"0100000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    String accountPermissionJson4 = "{\"owner_permission\":{\"type\":0,\""
        + "permission_name\":\"owner\",\"threshold\":1,\"keys\":"
        + "[{\"address\":\"" + PublicMethed.getAddressString(dev001Key) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
        + "\"operations\":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\":"
        + "\"0200000000000000000000000000000000000000000000000000000000000000\",\"keys\":"
        + "[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey4)
        + "\",\"weight\":1}]},{\"type\":2,"
        + "\"permission_name\":\"active0\",\"threshold\":1,\"operations\""
        + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
        + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\""
        + ":\"0100000000000000000000000000000000000000000000000000000000000000\",\"keys\""
        + ":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\":"
        + "\"0200000000000000000000000000000000000000000000000000000000000000\",\"keys\""
        + ":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
        + "\",\"weight\":1}]},{\"type\":2,"
        + "\"permission_name\":\"active0\",\"threshold\":1,\"operations\":\""
        + "0200000000000000000000000000000000000000000000000000000000000000\",\"keys\":"
        + "[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\""
        + ":\"0200000000000000000000000000000000000000000000000000000000000000\",\""
        + "keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey5)
        + "\",\"weight\":1}]},"
        + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,\"operations\":"
        + "\"0020000000000000000000000000000000000000000000000000000000000000\",\"keys\""
        + ":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
        + "\",\"weight\":1}]}]}";
    while (true) {
      PublicMethedForMutiSign
          .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address,
              dev001Key,
              blockingStubFull, 0,
              permissionKeyString);
      addressPermission(dev001Key, accountPermissionJson2);
      addressPermission(dev001Key, accountPermissionJson3);
      addressPermission(dev001Key, accountPermissionJson4);
      Account test001AddressAccount2 = PublicMethed.queryAccount(test001Address, blockingStubFull);
      List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
      Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
      Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
      PublicMethedForMutiSign.printPermissionList(permissionsList2);
      logger.info(PublicMethedForMutiSign.printPermission(ownerPermission2));
      logger.info(PublicMethedForMutiSign.printPermission(witnessPermission2));
    }
  }

  /**
   * constructor.
   */

  public void addressPermission(String addKey, String accountPermissionJson) {
    PublicMethed.freezeBalanceForReceiver(test001Address,
        10000000L, 5, 0,
        com.google.protobuf.ByteString.copyFrom(fromAddress), testKey002, blockingStubFull);

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = addKey;
    PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString);
    PublicMethed.unFreezeBalance(fromAddress, testKey002, 0,
        test001Address, blockingStubFull);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
