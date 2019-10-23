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

package org.gsc.wallet.witness;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.WalletClient;
import org.gsc.wallet.common.client.utils.TransactionUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;

@Slf4j
public class WalletTestWitness002 {


  private ManagedChannel channelFull = null;
  private ManagedChannel channelConfirmed = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String confirmednode = Configuration.getByPath("testng.conf")
      .getStringList("confirmednode.ip.list").get(0);

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
    WalletClient.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
        .usePlaintext(true)
        .build();
    blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);
  }

  @Test(enabled = true)
  public void testQueryAllWitness() {
    GrpcAPI.WitnessList witnesslist = blockingStubFull
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<GrpcAPI.WitnessList> result = Optional.ofNullable(witnesslist);
    if (result.isPresent()) {
      GrpcAPI.WitnessList witnessList = result.get();
      List<Protocol.Witness> list = witnessList.getWitnessesList();
      List<Protocol.Witness> newList = new ArrayList();
      newList.addAll(list);
      newList.sort(new WitnessComparator());
      GrpcAPI.WitnessList.Builder builder = GrpcAPI.WitnessList.newBuilder();
      newList.forEach(witness -> builder.addWitnesses(witness));
      result = Optional.of(builder.build());
    }
    logger.info(Integer.toString(result.get().getWitnessesCount()));
    Assert.assertTrue(result.get().getWitnessesCount() > 0);
    for (int j = 0; j < result.get().getWitnessesCount(); j++) {
      Assert.assertFalse(result.get().getWitnesses(j).getAddress().isEmpty());
      Assert.assertFalse(result.get().getWitnesses(j).getUrl().isEmpty());
      //Assert.assertTrue(result.get().getWitnesses(j).getLatestSlotNum() > 0);
      result.get().getWitnesses(j).getUrlBytes();
      result.get().getWitnesses(j).getLatestBlockNum();
      result.get().getWitnesses(j).getLatestSlotNum();
      result.get().getWitnesses(j).getTotalMissed();
      result.get().getWitnesses(j).getTotalProduced();
    }

    //Improve coverage.
    witnesslist.equals(result.get());
    witnesslist.hashCode();
    witnesslist.getSerializedSize();
    witnesslist.equals(null);
  }

  @Test(enabled = true)
  public void testConfirmedQueryAllWitness() {
    GrpcAPI.WitnessList confirmedWitnessList = blockingStubConfirmed
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<GrpcAPI.WitnessList> result = Optional.ofNullable(confirmedWitnessList);
    if (result.isPresent()) {
      GrpcAPI.WitnessList witnessList = result.get();
      List<Protocol.Witness> list = witnessList.getWitnessesList();
      List<Protocol.Witness> newList = new ArrayList();
      newList.addAll(list);
      newList.sort(new WitnessComparator());
      GrpcAPI.WitnessList.Builder builder = GrpcAPI.WitnessList.newBuilder();
      newList.forEach(witness -> builder.addWitnesses(witness));
      result = Optional.of(builder.build());
    }
    logger.info(Integer.toString(result.get().getWitnessesCount()));
    Assert.assertTrue(result.get().getWitnessesCount() > 0);
    for (int j = 0; j < result.get().getWitnessesCount(); j++) {
      Assert.assertFalse(result.get().getWitnesses(j).getAddress().isEmpty());
      Assert.assertFalse(result.get().getWitnesses(j).getUrl().isEmpty());
    }
  }

  class WitnessComparator implements Comparator {

    public int compare(Object o1, Object o2) {
      return Long
          .compare(((Protocol.Witness) o2).getVoteCount(), ((Protocol.Witness) o1).getVoteCount());
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

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
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

}


