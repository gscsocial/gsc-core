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

package org.gsc.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.TransactionUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.EmptyMessage;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract.FreezeBalanceContract;
import org.gsc.protos.Contract.UnfreezeBalanceContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;

@Slf4j
public class WalletTestAccount004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final String noFrozenBalanceTestKey =
      "8CB4480194192F30907E14B52498F594BD046E21D7C4D8FE866563A6760AC891";


  private final byte[] noFrozenAddress = PublicMethed.getFinalAddress(noFrozenBalanceTestKey);

  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);


  Long freezeAmount = 2000000L;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
  }

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


  }


  @Test(enabled = true)
  public void testFreezeBalance() {

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] account004AddressForFreeze = ecKey2.getAddress();
    String account004KeyForFreeze = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    Assert.assertTrue(PublicMethed.sendcoin(account004AddressForFreeze, 10000000,
        fromAddress, testKey002, blockingStubFull));
    //Freeze failed when freeze amount is large than currently balance.
    Assert.assertFalse(freezeBalance(account004AddressForFreeze, 9000000000000000000L,
        5L, account004KeyForFreeze));
    //Freeze failed when freeze amount less than 1Gsc
    Assert.assertFalse(freezeBalance(account004AddressForFreeze, 999999L, 5L,
        account004KeyForFreeze));
    //Freeze failed when freeze duration isn't 5 days.
    //Assert.assertFalse(freezeBalance(fromAddress, 1000000L, 2L, testKey002));
    //Unfreeze balance failed when 5 days hasn't come.
    Assert.assertFalse(PublicMethed.unFreezeBalance(account004AddressForFreeze,
        account004KeyForFreeze, 0, null, blockingStubFull));
    //Freeze failed when freeze amount is 0.
    Assert.assertFalse(freezeBalance(account004AddressForFreeze, 0L, 5L,
        account004KeyForFreeze));
    //Freeze failed when freeze amount is -1.
    Assert.assertFalse(freezeBalance(account004AddressForFreeze, -1L, 5L,
        account004KeyForFreeze));
    //Freeze failed when freeze duration is -1.
    //Assert.assertFalse(freezeBalance(fromAddress, 1000000L, -1L, testKey002));
    //Freeze failed when freeze duration is 0.
    //Assert.assertFalse(freezeBalance(fromAddress, 1000000L, 0L, testKey002));

  }

  @Test(enabled = true)
  public void testUnFreezeBalance() {
    //Unfreeze failed when there is no freeze balance.
    //Wait to be create account

    Assert.assertFalse(PublicMethed.unFreezeBalance(noFrozenAddress, noFrozenBalanceTestKey, 1,
        null, blockingStubFull));
    logger.info("Test unfreezebalance");
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] account004Address = ecKey1.getAddress();
    String account004Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert
        .assertTrue(PublicMethed.sendcoin(account004Address, freezeAmount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalance(account004Address, freezeAmount, 5,
        account004Key, blockingStubFull));
    Account account004;
    account004 = PublicMethed.queryAccount(account004Address, blockingStubFull);
    Assert.assertTrue(account004.getBalance() == 0);
    Assert.assertTrue(PublicMethed.unFreezeBalance(account004Address, account004Key, 0,
        null, blockingStubFull));
    account004 = PublicMethed.queryAccount(account004Address, blockingStubFull);
    Assert.assertTrue(account004.getBalance() == freezeAmount);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceGetCpu(account004Address, freezeAmount, 5,
        1, account004Key, blockingStubFull));
    account004 = PublicMethed.queryAccount(account004Address, blockingStubFull);
    Assert.assertTrue(account004.getBalance() == 0);

    Assert.assertFalse(PublicMethed.unFreezeBalance(account004Address, account004Key, 0,
        null, blockingStubFull));
    Assert.assertTrue(PublicMethed.unFreezeBalance(account004Address, account004Key, 1,
        null, blockingStubFull));
    account004 = PublicMethed.queryAccount(account004Address, blockingStubFull);
    Assert.assertTrue(account004.getBalance() == freezeAmount);

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

  /**
   * constructor.
   */

  public Boolean freezeBalance(byte[] addRess, long freezeBalance, long freezeDuration,
      String priKey) {
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;

    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Block currentBlock = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Account beforeFronzen = queryAccount(ecKey, blockingStubFull);
    Long beforeFrozenBalance = 0L;
    //Long beforeNet     = beforeFronzen.getNet();
    if (beforeFronzen.getFrozenCount() != 0) {
      beforeFrozenBalance = beforeFronzen.getFrozen(0).getFrozenBalance();
      logger.info(Long.toString(beforeFronzen.getFrozen(0).getFrozenBalance()));
    }

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    FreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      return false;
    }

    Long afterBlockNum = 0L;
    Integer wait = 0;
    PublicMethed.waitProduceNextBlock(searchBlockingStubFull);
    Account afterFronzen = queryAccount(ecKey, searchBlockingStubFull);
    Long afterFrozenBalance = afterFronzen.getFrozen(0).getFrozenBalance();
    logger.info(Long.toString(afterFronzen.getFrozen(0).getFrozenBalance()));
    logger.info(
        "beforefronen" + beforeFrozenBalance.toString() + "    afterfronzen" + afterFrozenBalance
            .toString());
    Assert.assertTrue(afterFrozenBalance - beforeFrozenBalance == freezeBalance);
    //Assert.assertTrue(afterNet - beforeNet == freezeBalance * frozen_duration);
    return true;


  }

  /**
   * constructor.
   */

  public boolean unFreezeBalance(byte[] addRess, String priKey) {
    byte[] address = addRess;

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Account search = queryAccount(ecKey, blockingStubFull);

    UnfreezeBalanceContract.Builder builder = UnfreezeBalanceContract
        .newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddreess);

    UnfreezeBalanceContract contract = builder.build();

    Transaction transaction = blockingStubFull.unfreezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * constructor.
   */

  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }
}


