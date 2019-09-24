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

package org.gsc.wallet.dailybuild.delaytransaction;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class DelayTransaction005 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private static final long now = System.currentTimeMillis();
  Long delaySecond = 10L;

  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");
  private Long cancleDelayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.cancleDelayTransactionFee");
  ByteString assetId;
  private byte[] contractAddress = null;
  SmartContract smartContract;

  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] smartContractOwnerAddress = ecKey.getAddress();
  String smartContractOwnerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

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
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false, description = "Delay update cpu limit contract")
  public void test1DelayUpdateSetting() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    smartContractOwnerAddress = ecKey.getAddress();
    smartContractOwnerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(smartContractOwnerKey);


    Assert.assertTrue(PublicMethed.sendcoin(smartContractOwnerAddress, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.freezeBalance(smartContractOwnerAddress,10000000L,5,
        smartContractOwnerKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName = "GSCTOKEN";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractScenario004_deployErc20GSCToken");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractScenario004_deployErc20GSCToken");
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, 999L,"0",0,null,
        smartContractOwnerKey, smartContractOwnerAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Long oldOriginCpuLimit = 567L;
    Assert.assertTrue(PublicMethed.updateCpuLimit(contractAddress,oldOriginCpuLimit,
        smartContractOwnerKey,smartContractOwnerAddress,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getOriginCpuLimit() == oldOriginCpuLimit);

    Long newOriginCpuLimit = 8765L;
    final String txid = PublicMethed.updateCpuLimitDelayGetTxid(contractAddress,
        newOriginCpuLimit, delaySecond,smartContractOwnerKey,smartContractOwnerAddress,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getOriginCpuLimit() == oldOriginCpuLimit);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    logger.info("newOriginCpuLimit: " + smartContract.getOriginCpuLimit());
    Assert.assertTrue(smartContract.getOriginCpuLimit() == newOriginCpuLimit);

    Long netFee = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
        .getNetFee();
    Long fee = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getFee();
    Assert.assertTrue(fee - netFee == delayTransactionFee);

  }

  @Test(enabled = false, description = "Cancel delay cpu limit contract")
  public void test2CancelDelayUpdateSetting() {
    //get account
    final Long oldOriginCpuLimit = smartContract.getOriginCpuLimit();
    final Long newOriginCpuLimit = 466L;

    String txid = PublicMethed.updateCpuLimitDelayGetTxid(contractAddress,newOriginCpuLimit,
        delaySecond,smartContractOwnerKey,smartContractOwnerAddress,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account ownerAccount = PublicMethed.queryAccount(smartContractOwnerKey,blockingStubFull);
    final Long beforeCancelBalance = ownerAccount.getBalance();


    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,fromAddress,testKey002,
        blockingStubFull));
    final String cancelTxid = PublicMethed.cancelDeferredTransactionByIdGetTxid(txid,
        smartContractOwnerAddress,smartContractOwnerKey,blockingStubFull);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,smartContractOwnerAddress,
        smartContractOwnerKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    logger.info("newOriginCpuLimit: " + smartContract.getOriginCpuLimit());
    Assert.assertTrue(smartContract.getOriginCpuLimit() == oldOriginCpuLimit);

    final Long netFee = PublicMethed.getTransactionInfoById(cancelTxid,blockingStubFull).get()
        .getReceipt().getNetFee();
    final Long fee = PublicMethed.getTransactionInfoById(cancelTxid,blockingStubFull).get()
        .getFee();
    logger.info("net fee : " + PublicMethed.getTransactionInfoById(cancelTxid,blockingStubFull)
        .get().getReceipt().getNetFee());
    logger.info("Fee : " + PublicMethed.getTransactionInfoById(cancelTxid,blockingStubFull)
        .get().getFee());

    ownerAccount = PublicMethed.queryAccount(smartContractOwnerKey,blockingStubFull);
    Long afterCancelBalance = ownerAccount.getBalance();
    Assert.assertTrue(fee - netFee == cancleDelayTransactionFee);
    Assert.assertTrue(fee == beforeCancelBalance - afterCancelBalance);

    logger.info("beforeCancelBalance: " + beforeCancelBalance);
    logger.info("afterCancelBalance : " + afterCancelBalance);
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


