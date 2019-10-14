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
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;


@Slf4j
public class WalletTestAccount001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String invalidTestKey =
      "592BB6C9BB255409A6A45EFD18E9A74FECDDCCE93A40D96B70FBE334E6361E36";

  private ManagedChannel channelFull = null;
  private ManagedChannel channelConfirmed = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String confirmedNode = Configuration.getByPath("testng.conf")
      .getStringList("confirmedNode.ip.list").get(0);

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

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmedNode)
        .usePlaintext(true)
        .build();
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);
  }


  @Test
  public void testqueryaccountfromfullnode() {
    //Query success, get the right balance,net and the account name.
    Account queryResult = queryAccount(testKey002, blockingStubFull);
    logger.info(ByteArray.toStr(queryResult.getAccountName().toByteArray()));
    logger.info(Long.toString(queryResult.getBalance()));
    logger.info(ByteArray.toStr(queryResult.getAddress().toByteArray()));
    Assert.assertTrue(queryResult.getBalance() > 0);
    Assert.assertTrue(queryResult.getAccountName().toByteArray().length > 0);
    Assert.assertFalse(queryResult.getAddress().isEmpty());

    //Query failed
    Account invalidQueryResult = queryAccount(invalidTestKey, blockingStubFull);
    Assert.assertTrue(invalidQueryResult.getAccountName().isEmpty());
    Assert.assertTrue(invalidQueryResult.getAddress().isEmpty());

    //Improve coverage.
    queryResult.hashCode();
    queryResult.getSerializedSize();
    queryResult.equals(queryResult);
    queryResult.equals(invalidQueryResult);
  }

  @Test
  public void testqueryaccountfromconfirmednode() {
    //Query success, get the right balance,net and the account name.
    Account queryResult = confirmedQueryAccount(testKey002, blockingStubConfirmed);
    Assert.assertTrue(queryResult.getBalance() > 0);
    Assert.assertTrue(queryResult.getAccountName().toByteArray().length > 0);
    Assert.assertFalse(queryResult.getAddress().isEmpty());

    //Query failed
    Account invalidQueryResult = confirmedQueryAccount(invalidTestKey, blockingStubConfirmed);
    Assert.assertTrue(invalidQueryResult.getAccountName().isEmpty());
    Assert.assertTrue(invalidQueryResult.getAddress().isEmpty());


  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelConfirmed != null) {
      channelConfirmed.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public Account queryAccount(String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
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
    logger.info(Integer.toString(ecKey.getAddress().length));

    // PublicMethed.AddPreFix();
    logger.info("address: ", Hex.toHexString(ecKey.getAddress()));
    logger.info(Integer.toString(ecKey.getAddress().length));
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
//    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  /**
   * constructor.
   */

  public Account confirmedQueryAccount(String priKey,
                                       WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    if (ecKey == null) {
      String pubKey = loadPubKey();
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccountConfirmed(ecKey.getAddress(), blockingStubConfirmed);
//     return grpcQueryAccountConfirmed(ecKey.getAddress(), blockingStubConfirmed);
  }

  /**
   * constructor.
   */

  public String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccount(byte[] address,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */

  public Account grpcQueryAccountConfirmed(byte[] address,
      WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubConfirmed.getAccount(request);
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


