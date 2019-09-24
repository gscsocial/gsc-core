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
import java.util.ArrayList;
import java.util.HashMap;
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
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class WalletTestMutiSign018 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  ArrayList<String> txidList = new ArrayList<String>();

  Optional<TransactionInfo> infoById = null;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;
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


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = false, threadPoolSize = 1, invocationCount = 1)
  public void test1MutiSignForClearContractAbi() {
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

    long needcoin = updateAccountPermissionFee + multiSignFee * 4;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needcoin + 100000000L, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 1, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
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
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
        blockingStubFull, ownerKeyString);

    Long maxFeeLimit = 1000000000L;
    String filePath = "src/test/resources/soliditycode_v0.5.4/TriggerConstant004.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    byte[] contractAddress = PublicMethedForMutiSign.deployContract1(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, ownerKey, ownerAddress, blockingStubFull, 2, permissionKeyString);
    logger.info("address:" + Base58.encode58Check(contractAddress));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null);
    Assert.assertTrue(PublicMethedForMutiSign
        .clearContractAbi(contractAddress, ownerAddress, ownerKey,
            blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, ownerAddress, blockingStubFull));
  }

  @Test(enabled = false, threadPoolSize = 1, invocationCount = 1)
  public void test2MutiSignForClearContractAbiForDefault() {
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

    long needcoin = updateAccountPermissionFee + multiSignFee * 4;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needcoin + 100000000L, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 1, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    String operationsDefault = "7fff1fc0034e0100000000000000000000000000000000000000000000000000";
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operationsDefault + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
        blockingStubFull, ownerKeyString);

    Long maxFeeLimit = 1000000000L;
    String filePath = "src/test/resources/soliditycode_v0.5.4/TriggerConstant004.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    byte[] contractAddress = PublicMethedForMutiSign.deployContract1(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, ownerKey, ownerAddress, blockingStubFull, 2, permissionKeyString);
    logger.info("address:" + Base58.encode58Check(contractAddress));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null);
    Assert.assertTrue(PublicMethedForMutiSign
        .clearContractAbi(contractAddress, ownerAddress, ownerKey,
            blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, ownerAddress, blockingStubFull));
  }


  @Test(enabled = false, threadPoolSize = 1, invocationCount = 1)
  public void test3MutiSignForClearContractAbiForDefault() {
    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(ownerKey);

    long needcoin = updateAccountPermissionFee + multiSignFee * 4;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needcoin + 100000000L, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 1, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    String[] activeDefaultKeyString = new String[1];

    activeDefaultKeyString[0] = ownerKey;
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long maxFeeLimit = 1000000000L;
    String filePath = "src/test/resources/soliditycode_v0.5.4/TriggerConstant004.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    byte[] contractAddress = PublicMethedForMutiSign.deployContract1(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, ownerKey, ownerAddress, blockingStubFull, 2, activeDefaultKeyString);
    logger.info("address:" + Base58.encode58Check(contractAddress));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null);
    Assert.assertTrue(PublicMethedForMutiSign
        .clearContractAbi(contractAddress, ownerAddress, ownerKey,
            blockingStubFull, 2, activeDefaultKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, ownerAddress, blockingStubFull));
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}