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

package org.gsc.wallet.fulltest;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.WalletClient;
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.gsc.wallet.common.client.utils.TransactionUtils;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.AccountNetMessage;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.FreezeBalanceContract;
import org.gsc.protos.Contract.UnfreezeBalanceContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;

@Slf4j
public class ContinueVote {

  //testng001、testng002、testng003、testng004

  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";


  /*  //testng001、testng002、testng003、testng004
  private static final byte[] fromAddress = Base58
      .decodeFromBase58Check("THph9K2M2nLvkianrMGswRhz5hjSA9fuH7");*/
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);


  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);


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
    logger.info("Pre fix byte =====  " + WalletClient.getAddressPreFixByte());
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext(true)
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);
  }

  @Test(enabled = false, threadPoolSize = 30, invocationCount = 30)
  public void testVoteWitness() {
    ByteString addressBs = ByteString.copyFrom(fromAddress);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage accountNetMessage = blockingStubFull.getAccountNet(request);
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    Base58.encode58Check(fromAddress);
    logger.info(Base58.encode58Check(fromAddress));
    String voteStr = "TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes";
    HashMap<String, String> smallVoteMap = new HashMap<String, String>();
    smallVoteMap.put(voteStr, "1");
    Account fromInfo = PublicMethed.queryAccount(testKey002, blockingStubFull);

    Boolean ret = false;
    Integer i = 0;
    while (fromInfo.getBalance() > 100000000) {
      randNum = rand.nextInt(30) + 1;
      voteStr = "TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes";
      smallVoteMap = new HashMap<String, String>();
      smallVoteMap.put(voteStr, Integer.toString(randNum));
      if (fromInfo.getFrozen(0).getFrozenBalance() < 10000000) {
        PublicMethed.freezeBalance(fromAddress, 10000000000L, 5, testKey002, blockingStubFull);
      }
      ret = voteWitness(smallVoteMap, fromAddress, testKey002);
      if (ret) {
        logger.info("This vote num is " + Integer.toString(randNum));
        logger.info("Now the fromaddress vote is " + Long.toString(fromInfo
            .getVotes(0).getVoteCount()));
        logger.info(Integer.toString(i++));
      }
      fromInfo = PublicMethed.queryAccount(testKey002, blockingStubFull);
      accountNetMessage = blockingStubFull.getAccountNet(request);
      logger.info("Now the from net used is " + Long.toString(accountNetMessage.getNetUsed()));

    }

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

  public Boolean voteWitness(HashMap<String, String> witness, byte[] addRess, String priKey) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    Account beforeVote = queryAccount(ecKey, blockingStubFull);
    Long beforeVoteNum = 0L;
    if (beforeVote.getVotesCount() != 0) {
      beforeVoteNum = beforeVote.getVotes(0).getVoteCount();
    }

    Contract.VoteWitnessContract.Builder builder = Contract.VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(addRess));
    for (String addressBase58 : witness.keySet()) {
      String value = witness.get(addressBase58);
      final long count = Long.parseLong(value);
      Contract.VoteWitnessContract.Vote.Builder voteBuilder = Contract.VoteWitnessContract.Vote
          .newBuilder();
      byte[] address = WalletClient.decodeFromBase58Check(addressBase58);
      logger.info("address ====== " + ByteArray.toHexString(address));
      if (address == null) {
        continue;
      }
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(count);
      builder.addVotes(voteBuilder.build());
    }

    Contract.VoteWitnessContract contract = builder.build();

    Transaction transaction = blockingStubFull.voteWitnessAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);

    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    }
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Account afterVote = queryAccount(ecKey, searchBlockingStubFull);
    //Long afterVoteNum = afterVote.getVotes(0).getVoteCount();
    for (String key : witness.keySet()) {
      for (int j = 0; j < afterVote.getVotesCount(); j++) {
        logger.info(Long.toString(Long.parseLong(witness.get(key))));
        logger.info(key);
        if (key.equals("TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes")) {
          logger.info("catch it");
          logger.info(Long.toString(afterVote.getVotes(j).getVoteCount()));
          logger.info(Long.toString(Long.parseLong(witness.get(key))));
          //Assert.assertTrue(afterVote.getVotes(j).getVoteCount() == Long
          // .parseLong(witness.get(key)));
        }

      }
    }
    return true;
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
    Account beforeFronzen = queryAccount(ecKey, blockingStubFull);

    Long beforeFrozenBalance = 0L;
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

    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Block searchCurrentBlock = searchBlockingStubFull.getNowBlock(GrpcAPI
        .EmptyMessage.newBuilder().build());
    Integer wait = 0;
    while (searchCurrentBlock.getBlockHeader().getRawData().getNumber()
        < currentBlock.getBlockHeader().getRawData().getNumber() + 1 && wait < 30) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      logger.info("Another fullnode didn't syn the first fullnode data");
      searchCurrentBlock = searchBlockingStubFull.getNowBlock(GrpcAPI
          .EmptyMessage.newBuilder().build());
      wait++;
      if (wait == 9) {
        logger.info("Didn't syn,skip to next case.");
      }
    }

    Account afterFronzen = queryAccount(ecKey, searchBlockingStubFull);
    Long afterFrozenBalance = afterFronzen.getFrozen(0).getFrozenBalance();
    logger.info(
        "afterfrozenbalance =" + Long.toString(afterFrozenBalance) + "beforefrozenbalance =  "
            + beforeFrozenBalance + "freezebalance = " + Long.toString(freezeBalance));
    Assert.assertTrue(afterFrozenBalance - beforeFrozenBalance == freezeBalance);
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

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }
}


