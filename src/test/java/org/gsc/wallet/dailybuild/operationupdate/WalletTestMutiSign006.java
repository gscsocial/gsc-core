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

package org.gsc.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class WalletTestMutiSign006 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress01 = PublicMethed.getFinalAddress(testKey001);
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  private static final long now = System.currentTimeMillis();
  private static String name = "MutiSign001_" + Long.toString(now);
  private static final long totalSupply = now;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] participateAddress = ecKey4.getAddress();
  String participateKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

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
  }

  @Test(enabled = true)
  public void testMutiSign1CreateAssetissue() {
    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(ownerKey);

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 3;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needCoin + 2048000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = ownerKey;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    String txid = PublicMethedForMutiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull, ownerKeyString);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);

    long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long cpuFee = infoById.get().getReceipt().getCpuFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("cpuFee: " + cpuFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, cpuFee + netFee + updateAccountPermissionFee);

    balanceBefore = balanceAfter;

    Long start = System.currentTimeMillis() + 5000;
    Long end = System.currentTimeMillis() + 1000000000;
    logger.info("try create asset issue");

    txid = PublicMethedForMutiSign
        .createAssetIssueForTransactionId1(ownerAddress, name, totalSupply, 1,
            1, start, end, 1, description, url, 2000L, 2000L,
            1L, 1L, ownerKey, blockingStubFull, 2, permissionKeyString);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

//    System.out.println("trxId: " + txid);
    Assert.assertNotNull(txid);

    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    cpuFee = infoById.get().getReceipt().getCpuFee();
    netFee = infoById.get().getReceipt().getNetFee();
    fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("cpuFee: " + cpuFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, cpuFee + netFee + multiSignFee + 1000_000000L);

    logger.info(" create asset end");
  }

  /**
   * constructor.
   */

  @Test(enabled = true)
  public void testMutiSign2TransferAssetissue() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.printAddress(manager1Key);
    Account getAssetIdFromOwnerAccount;
    getAssetIdFromOwnerAccount = PublicMethed.queryAccount(ownerAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromOwnerAccount.getAssetIssuedID();
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    String txid = PublicMethedForMutiSign.transferAssetForTransactionId1(manager1Address,
        assetAccountId1.toByteArray(), 10, ownerAddress, ownerKey, blockingStubFull,
        2, permissionKeyString);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertNotNull(txid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long cpuFee = infoById.get().getReceipt().getCpuFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("cpuFee: " + cpuFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, cpuFee + netFee + multiSignFee);
  }

  /**
   * constructor.
   */

  @Test(enabled = true)
  public void testMutiSign3ParticipateAssetissue() {
    ecKey4 = new ECKey(Utils.getRandom());
    participateAddress = ecKey4.getAddress();
    participateKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 2;

    Assert.assertTrue(
        PublicMethed.sendcoin(participateAddress, needCoin + 2048000000L, fromAddress, testKey002,
            blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(participateAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    ownerKeyString[0] = participateKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(participateKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    String txid = PublicMethedForMutiSign
        .accountPermissionUpdateForTransactionId(accountPermissionJson, participateAddress,
            participateKey, blockingStubFull, ownerKeyString);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertNotNull(txid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    long balanceAfter = PublicMethed.queryAccount(participateAddress, blockingStubFull)
        .getBalance();
    long cpuFee = infoById.get().getReceipt().getCpuFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("cpuFee: " + cpuFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, cpuFee + netFee + updateAccountPermissionFee);

    balanceBefore = balanceAfter;

    txid = PublicMethedForMutiSign.participateAssetIssueForTransactionId(ownerAddress,
        assetAccountId1.toByteArray(), 10, participateAddress, participateKey, 2,
        blockingStubFull, permissionKeyString);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

//    logger.info("trxId: " + txid);
    Assert.assertNotNull(txid);

    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("trx: " + infoById.toString());
    balanceAfter = PublicMethed.queryAccount(participateAddress, blockingStubFull)
        .getBalance();
    cpuFee = infoById.get().getReceipt().getCpuFee();
    netFee = infoById.get().getReceipt().getNetFee();
    fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("cpuFee: " + cpuFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee + 10);
    Assert.assertEquals(fee, cpuFee + netFee + multiSignFee);
  }

  /**
   * constructor.
   */

  @Test(enabled = true)
  public void testMutiSign4updateAssetissue() {
    url = "MutiSign001_update_url" + Long.toString(now);
    ownerKeyString[0] = ownerKey;
    description = "MutiSign001_update_description" + Long.toString(now);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    String txid = PublicMethedForMutiSign
        .updateAssetForTransactionId(ownerAddress, description.getBytes(), url.getBytes(), 100L,
            100L, ownerKey, 2, blockingStubFull, permissionKeyString);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    long balanceAfter = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    long cpuFee = infoById.get().getReceipt().getCpuFee();
    long netFee = infoById.get().getReceipt().getNetFee();
    long fee = infoById.get().getFee();

    logger.info("balanceAfter: " + balanceAfter);
    logger.info("cpuFee: " + cpuFee);
    logger.info("netFee: " + netFee);
    logger.info("fee: " + fee);

    Assert.assertEquals(balanceBefore - balanceAfter, fee);
    Assert.assertEquals(fee, cpuFee + netFee + multiSignFee);
  }


  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


