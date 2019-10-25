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
//import org.gsc.protos.Protocol.DeferredTransaction;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j
public class DelayTransaction003 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final String name = "Asset008_" + Long.toString(now);
  String description = "just-test";
  String url = "https://github.com/gscsocial/wallet-cli/";
  Long delaySecond = 10L;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");
  private Long cancleDelayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.cancleDelayTransactionFee");
  ByteString assetId;



  Optional<TransactionInfo> infoById = null;
  Optional<Transaction> getTransactionById = null;


  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] assetOwnerAddress = ecKey.getAddress();
  String assetOwnerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] receiverAssetAddress = ecKey3.getAddress();
  String receiverassetKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());


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

  @Test(enabled = false, description = "Delay transfer asset")
  public void test1DelayTransferAsset() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    assetOwnerAddress = ecKey.getAddress();
    assetOwnerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethed.printAddress(assetOwnerKey);
    ecKey3 = new ECKey(Utils.getRandom());
    receiverAssetAddress = ecKey3.getAddress();
    receiverassetKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(receiverassetKey);

    Assert.assertTrue(PublicMethed.sendcoin(assetOwnerAddress, 2048000000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Create test token.
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(assetOwnerAddress,
        name, totalSupply, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 100000L, 1L,
        assetOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account assetOwnerAccount = PublicMethed.queryAccount(assetOwnerKey, blockingStubFull);
    assetId = assetOwnerAccount.getAssetIssuedID();

    //Delay transfer asset
    Long transferAssetAmount = 1L;
    final Long ownerAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, assetOwnerKey,blockingStubFull);
    final Long receiverAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId,receiverassetKey,blockingStubFull);
    Assert.assertTrue(PublicMethed.transferAssetDelay(receiverAssetAddress, assetId.toByteArray(),
        transferAssetAmount, delaySecond,assetOwnerAddress, assetOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long ownerAssetBalanceInDelayTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId,assetOwnerKey,blockingStubFull);
    final Long receiverAssetBalanceInDelayTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId,receiverassetKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long ownerAssetBalanceAfterTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId,assetOwnerKey,blockingStubFull);
    Long receiverAssetBalanceAfterTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId,receiverassetKey,blockingStubFull);


    Assert.assertEquals(ownerAssetBalanceOfbeforeTransferAsset,
        ownerAssetBalanceInDelayTransferAsset);
    Assert.assertTrue(receiverAssetBalanceOfbeforeTransferAsset
        == receiverAssetBalanceInDelayTransferAsset);
    Assert.assertTrue(ownerAssetBalanceInDelayTransferAsset - transferAssetAmount
        == ownerAssetBalanceAfterTransferAsset);
    Assert.assertTrue(receiverAssetBalanceAfterTransferAsset == transferAssetAmount);

  }


  @Test(enabled = false, description = "Cancel delay transfer asset")
  public void test2CancelDelayTransferAsset() {


    //Delay transfer asset
    Long transferAssetAmount = 1L;
    final Long ownerAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, assetOwnerKey,blockingStubFull);
    final Long receiverAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId,receiverassetKey,blockingStubFull);

    String txid = PublicMethed.transferAssetDelayGetTxid(receiverAssetAddress,
        assetId.toByteArray(), transferAssetAmount, delaySecond,assetOwnerAddress, assetOwnerKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,receiverAssetAddress,
    // receiverassetKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.cancelDeferredTransactionById(txid,assetOwnerAddress,
        assetOwnerKey,blockingStubFull));
    //Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,assetOwnerAddress,
    // assetOwnerKey,blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long ownerAssetBalanceAfterTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId,assetOwnerKey,blockingStubFull);
    Long receiverAssetBalanceAfterTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId,receiverassetKey,blockingStubFull);


    Assert.assertEquals(ownerAssetBalanceOfbeforeTransferAsset, ownerAssetBalanceAfterTransferAsset
    );
    Assert.assertTrue(receiverAssetBalanceAfterTransferAsset
        == receiverAssetBalanceOfbeforeTransferAsset);
    Assert.assertFalse(PublicMethed.cancelDeferredTransactionById(txid,assetOwnerAddress,
        assetOwnerKey,blockingStubFull));

  }

  @Test(enabled = false, description = "Delay unfreeze asset")
  public void test3DelayUnfreezeAsset() {

    final Long ownerAssetBalanceOfbeforeTransferAsset = PublicMethed
        .getAssetBalanceByAssetId(assetId, assetOwnerKey,blockingStubFull);

    String txid = PublicMethed.unfreezeAssetDelayGetTxid(assetOwnerAddress,delaySecond,
        assetOwnerKey,blockingStubFull);
  }

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


