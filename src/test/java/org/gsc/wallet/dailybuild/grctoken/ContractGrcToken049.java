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

package org.gsc.wallet.dailybuild.grctoken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class ContractGrcToken049 {


  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long TotalSupply = 10000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
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

  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }

  /**
   * constructor.
   */

  public ByteString createAssetissue(byte[] devAddress, String devKey, String tokenName) {

    ByteString assetAccountId = null;
    ByteString addressBS1 = ByteString.copyFrom(devAddress);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {
      Long start = System.currentTimeMillis() + 2000;
      Long end = System.currentTimeMillis() + 1000000000;

      logger.info("The token name: " + tokenName);

      //Create a new AssetIssue success.
      Assert.assertTrue(PublicMethed.createAssetIssue(devAddress, tokenName, TotalSupply, 1,
          100, start, end, 1, description, url, 10000L, 10000L,
          1L, 1L, devKey, blockingStubFull));

      Account getAssetIdFromThisAccount = PublicMethed.queryAccount(devAddress, blockingStubFull);
      assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    } else {
      logger.info("This account already create an assetisue");
      Optional<AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
      tokenName = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0)
          .getName().toByteArray());
    }
    return assetAccountId;
  }

  @Test(enabled = true, description = "TransferToken to myself")
  public void deployTransferTokenContract() {

    Assert
        .assertTrue(PublicMethed.sendcoin(dev001Address, 2048000000, fromAddress,
            testKey002, blockingStubFull));
    Assert
        .assertTrue(PublicMethed.sendcoin(user001Address, 4048000000L, fromAddress,
            testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // freeze balance
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(dev001Address, 204800000,
        5, 1, dev001Key, blockingStubFull));

    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(user001Address, 2048000000,
        5, 1, user001Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String tokenName = "testAI_" + randomInt(10000, 90000);
    ByteString tokenId = createAssetissue(user001Address, user001Key, tokenName);
    int i = randomInt(6666666, 9999999);

    // devAddress transfer token to A
    PublicMethed.transferAsset(dev001Address, tokenId.toByteArray(), 101, user001Address,
        user001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // deploy transferTokenContract
    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractGrcToken049.sol";
    String contractName = "tokenTest";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 10000, tokenId.toStringUtf8(),
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("Deploy cputotal is " + infoById.get().getReceipt().getCpuUsageTotal());
    byte[] transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();

    // devAddress transfer token to userAddress
    PublicMethed
        .transferAsset(transferTokenContractAddress, tokenId.toByteArray(), 100, user001Address,
            user001Key,
            blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(user001Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(user001Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeCpuUsed = resourceInfo.getCpuUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long beforeAssetIssueCount =
        PublicMethed.getAssetIssueValue(user001Address, tokenId, blockingStubFull);
    Long beforeAssetIssueContractAddress =
        PublicMethed.getAssetIssueValue(transferTokenContractAddress, tokenId,
            blockingStubFull);
    final Long beforeAssetIssueDev = PublicMethed.getAssetIssueValue(dev001Address, tokenId,
        blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeCpuUsed:" + beforeCpuUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueCount);
    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);

    // user trigger A to transfer token to B
    String param =
        "\"" + Base58.encode58Check(dev001Address) + "\",\"" + tokenId
            .toStringUtf8()
            + "\",\"1\"";
    final String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "TransferTokenTo(address,grcToken,uint256)",
        param, false, 0, 100000000L, "0",
        0, user001Address, user001Key,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account infoafter = PublicMethed.queryAccount(user001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(user001Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterCpuUsed = resourceInfoafter.getCpuUsed();
    Long afterAssetIssueCount =
        PublicMethed.getAssetIssueValue(user001Address, tokenId, blockingStubFull);
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    Long afterAssetIssueContractAddress =
        PublicMethed.getAssetIssueValue(transferTokenContractAddress, tokenId,
            blockingStubFull);
    final Long afterAssetIssueDev = PublicMethed.getAssetIssueValue(dev001Address, tokenId,
        blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterCpuUsed:" + afterCpuUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueCount);
    logger.info("afterAssetIssueContractAddress:" + afterAssetIssueContractAddress);

    infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger cputotal is " + infoById.get().getReceipt().getCpuUsageTotal());

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertEquals(beforeAssetIssueCount, afterAssetIssueCount);
    Assert.assertTrue(beforeAssetIssueContractAddress - 1 == afterAssetIssueContractAddress);

    Assert.assertTrue(beforeAssetIssueDev + 1 == afterAssetIssueDev);
    PublicMethed.unFreezeBalance(dev001Address, dev001Key, 1,
        dev001Address, blockingStubFull);
    PublicMethed.unFreezeBalance(user001Address, user001Key, 1,
        user001Address, blockingStubFull);
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


