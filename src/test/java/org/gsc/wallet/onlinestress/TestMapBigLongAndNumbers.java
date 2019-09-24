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

package org.gsc.wallet.onlinestress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.Random;
//import java.io.FileWriter;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
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
import org.gsc.wallet.common.client.utils.Base58;
//import java.io.BufferedWriter;

import org.gsc.wallet.common.client.utils.PublicMethed;


@Slf4j
public class TestMapBigLongAndNumbers {

  //testng001、testng002、testng003、testng004
  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  //private final String testAddress41 = ByteArray.toHexString(fromAddress);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);


  String kittyCoreAddressAndCut = "";
  byte[] kittyCoreContractAddress = null;
  byte[] saleClockAuctionContractAddress = null;
  byte[] siringClockAuctionContractAddress = null;
  byte[] geneScienceInterfaceContractAddress = null;
  //Integer consumeUserResourcePercent = 20;
  Integer consumeUserResourcePercent = 100;
  String txid = "";
  Optional<TransactionInfo> infoById = null;


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] triggerAddress = ecKey2.getAddress();
  String triggerKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


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
    PublicMethed.printAddress(triggerKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);


  }

  @Test(enabled = true, threadPoolSize = 10, invocationCount = 10)
  public void deployErc721KittyCore() {

    Long maxFeeLimit = 1000000000L;

    String contractName = "MappingExample";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_TestMapBigLongAndNumbers_deployErc721KittyCore");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_TestMapBigLongAndNumbers_deployErc721KittyCore");
    kittyCoreContractAddress = PublicMethed.deployContract(contractName, abi, code, "",
        maxFeeLimit, 0L, consumeUserResourcePercent, null, testKey002,
        fromAddress, blockingStubFull);

    String data1 = "a";
    String data2 = "b";
    String data3 = "c";
    String data4 = "d";

    for (int i = 0; i < 13; i++) {
      data1 += data1;
    }

    for (int i = 0; i < 12; i++) {
      data2 += data2;
    }
    for (int i = 0; i < 11; i++) {
      data3 += data3;
    }
    for (int i = 0; i < 10; i++) {
      data4 += data4;
    }
    String data;
    data = data1 + data2 + data3 + data4;

    String data5 = "a";

    Account account = PublicMethed.queryAccountByAddress(fromAddress, blockingStubFull);
    System.out.println(Long.toString(account.getBalance()));
    long accountBalance = account.getBalance();

    Random random = new Random();
    int randNumber = random.nextInt(15) + 15;

    System.out.println("random number:" + randNumber);

    try {
      Thread.sleep(randNumber);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    for (int ii = 1; ii < 111100000; ii++) {
      ECKey ecKey1 = new ECKey(Utils.getRandom());
      byte[] userAddress = ecKey1.getAddress();
      String inputKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
      String addresstest = Base58.encode58Check(userAddress);

      String saleContractString = "\"" + data + "\"" + "," + "\""
          + Base58.encode58Check(userAddress) + "\"";

      System.out.println("long string address:" + addresstest);

      txid = PublicMethed.triggerContract(kittyCoreContractAddress, "update2(string,address)",
          saleContractString, false, 0, 1000000000L, fromAddress, testKey002, blockingStubFull);
      logger.info(txid);

      String saleContractString1 = "\"" + data5 + "\"" + "," + "\""
          + Base58.encode58Check(userAddress) + "\"";

      System.out.println("short string address:" + addresstest);

      txid = PublicMethed.triggerContract(kittyCoreContractAddress, "update2(string,address)",
          saleContractString1, false, 0, 1000000000L, fromAddress, testKey002, blockingStubFull);
      logger.info(txid);

      System.out.println("time out");

      txid = PublicMethed.triggerContract(kittyCoreContractAddress, "testUseCpu(uint256)",
          "1000000000", false, 0, 1000000000L, fromAddress, testKey002, blockingStubFull);

      infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

      infoById.get().getResultValue();

      String isSuccess;

      if (infoById.get().getResultValue() == 0) {
        logger.info("success:" + " Number：" + ii);
        isSuccess = "success";
      } else {
        logger.info("failed" + " Number:" + ii);
        isSuccess = "fail";
      }
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

}


