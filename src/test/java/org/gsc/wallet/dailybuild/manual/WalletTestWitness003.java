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

package org.gsc.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
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
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.GrpcAPI.WitnessList;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;

//import stest.gsc.wallet.common.client.AccountComparator;

@Slf4j
public class WalletTestWitness003 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final String testUpdateWitnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] updateAddress = PublicMethed.getFinalAddress(testUpdateWitnessKey);
  private static final byte[] INVAILD_ADDRESS = Base58
      .decodeFromBase58Check("27cu1ozb4mX3m2afY68FSAqn3HmMp815d48");

  private static final Long costForCreateWitness = 9999000000L;
  String createWitnessUrl = "http://www.createwitnessurl.com";
  String updateWitnessUrl = "http://www.updatewitnessurl.com";
  String nullUrl = "";
  String spaceUrl = "          ##################~!@#$%^&*()_+}{|:'/.,<>?|]=-";
  byte[] createUrl = createWitnessUrl.getBytes();
  byte[] updateUrl = updateWitnessUrl.getBytes();
  byte[] wrongUrl = nullUrl.getBytes();
  byte[] updateSpaceUrl = spaceUrl.getBytes();
  private static final String tooLongUrl = "qagwqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqas"
      + "wqaswqasw1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazx"
      + "swedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedc"
      + "vqazxswedcvqazxswedcvqazxswedcvqazxswedcv";
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] lowBalAddress = ecKey.getAddress();
  String lowBalTest = ByteArray.toHexString(ecKey.getPrivKeyBytes());

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
    logger.info(lowBalTest);
    logger.info(ByteArray.toHexString(PublicMethed.getFinalAddress(lowBalTest)));
    logger.info(Base58.encode58Check(PublicMethed.getFinalAddress(lowBalTest)));

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Invaild account to apply create witness")
  public void testInvaildToApplyBecomeWitness() {
    Assert.assertFalse(createWitnessNotBroadcast(INVAILD_ADDRESS, createUrl, testKey002));
  }

  @Test(enabled = true, description = "Create witness")
  public void testCreateWitness() {
    //If you are already is witness, apply failed
    //createWitness(fromAddress, createUrl, testKey002);
    //Assert.assertFalse(createWitness(fromAddress, createUrl, testKey002));

    //No balance,try to create witness.
    Assert.assertFalse(createWitnessNotBroadcast(lowBalAddress, createUrl, lowBalTest));

    //Send enough coin to the apply account to make that account
    // has ability to apply become witness.
    GrpcAPI.WitnessList witnesslist = blockingStubFull
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<WitnessList> result = Optional.ofNullable(witnesslist);
    GrpcAPI.WitnessList witnessList = result.get();
    if (result.get().getWitnessesCount() < 6) {
      Assert.assertTrue(PublicMethed
          .sendcoin(lowBalAddress, costForCreateWitness, fromAddress, testKey002,
              blockingStubFull));
      //null url, update failed
      Assert.assertFalse(createWitnessNotBroadcast(lowBalAddress, wrongUrl, testUpdateWitnessKey));
      //too long url, update failed
      Assert.assertFalse(createWitnessNotBroadcast(lowBalAddress,
          tooLongUrl.getBytes(), testUpdateWitnessKey));
      Assert.assertTrue(createWitnessNotBroadcast(lowBalAddress, createUrl, lowBalTest));

    }
  }

  @Test(enabled = true, description = "Update witness")
  public void testUpdateWitness() {
    GrpcAPI.WitnessList witnesslist = blockingStubFull
        .listWitnesses(GrpcAPI.EmptyMessage.newBuilder().build());
    Optional<WitnessList> result = Optional.ofNullable(witnesslist);
    GrpcAPI.WitnessList witnessList = result.get();
    if (result.get().getWitnessesCount() < 6) {
      //null url, update failed
      Assert.assertFalse(updateWitness(updateAddress, wrongUrl, testUpdateWitnessKey));
      //too long url, update failed
      Assert.assertFalse(updateWitness(updateAddress, tooLongUrl.getBytes(), testUpdateWitnessKey));
      //Content space and special char, update success
      Assert.assertTrue(updateWitness(updateAddress, updateSpaceUrl, testUpdateWitnessKey));
      //update success
      Assert.assertTrue(updateWitness(updateAddress, updateUrl, testUpdateWitnessKey));
    } else {
      logger.info("Update witness case had been test.This time skip it.");
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
  }

  /**
   * constructor.
   */

  public Boolean createWitnessNotBroadcast(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));
    Contract.WitnessCreateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    return true;
  }

  /**
   * constructor.
   */

  public Boolean updateWitness(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.WitnessUpdateContract.Builder builder = Contract.WitnessUpdateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUpdateUrl(ByteString.copyFrom(url));
    Contract.WitnessUpdateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.updateWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethed.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      logger.info("response.getRestult() == false");
      return false;
    } else {
      return true;
    }

  }

  /**
   * constructor.
   */

  public Boolean sendcoin(byte[] to, long amount, byte[] owner, String priKey) {

    //String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    Contract.TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return false;
    } else {
      return true;
    }
  }

  /**
   * constructor.
   */

  public Account queryAccount(String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
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

  private Protocol.Transaction signTransaction(ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }
}


