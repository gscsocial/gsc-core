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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.SmartContract;
import org.zeromq.ZMQ;
import org.gsc.wallet.common.client.utils.PublicMethed;
import zmq.ZMQ.Event;

@Slf4j
public class ContractEvent001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

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

  String txid;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] event001Address = ecKey1.getAddress();
  String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] event002Address = ecKey2.getAddress();
  String event002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

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
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true)
  public void test1ContractEventAndLog() {
    ecKey1 = new ECKey(Utils.getRandom());
    event001Address = ecKey1.getAddress();
    event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(event001Key);

    ecKey2 = new ECKey(Utils.getRandom());
    event002Address = ecKey2.getAddress();
    event002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(event001Key);
    PublicMethed.printAddress(testKey002);

    Assert.assertTrue(PublicMethed.sendcoin(event001Address, maxFeeLimit * 30, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(event002Address, maxFeeLimit * 30, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull1);

    AccountResourceMessage accountResource = PublicMethed.getAccountResource(event001Address,
        blockingStubFull);
    Long cpuLimit = accountResource.getCpuLimit();
    Long cpuUsage = accountResource.getCpuUsed();
    Long balanceBefore = PublicMethed.queryAccount(event001Key, blockingStubFull).getBalance();

    logger.info("before cpu limit is " + Long.toString(cpuLimit));
    logger.info("before cpu usage is " + Long.toString(cpuUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));

    String contractName = "addressDemo";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractEventAndLog1");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractEventAndLog1");
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 50, null, event001Key, event001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);

    Integer i = 0;
    for (i = 0; i < 1; i++) {
      txid = PublicMethed.triggerContract(contractAddress,
          "depositForEventCycle(uint256)", "100", false,
          1L, 100000000L, event002Address, event002Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForLogCycle(uint256)", "100", false,
          2L, 100000000L, event002Address, event002Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "triggerUintEvent()", "#", false,
          0, maxFeeLimit, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "triggerintEvent()", "#", false,
          0, maxFeeLimit, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForEventAndLog()", "#", false,
          1, maxFeeLimit, event001Address, event001Key, blockingStubFull);
      logger.info(txid);
      txid = PublicMethed.triggerContract(contractAddress,
          "depositForEventNoIndex()", "#", false,
          0L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);
      txid = PublicMethed.triggerContract(contractAddress,
          "depositForLog()", "#", false,
          1L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForEventNoIndex()", "#", false,
          1L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForEventOneIndex()", "#", false,
          1L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForEventTwoIndex()", "#", false,
          2L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForEvent()", "#", false,
          3L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForEventCycle(uint256)", "100", false,
          1L, 100000000L, event002Address, event002Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForLogCycle(uint256)", "100", false,
          2L, 100000000L, event002Address, event002Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForAnonymousHasLog()", "#", false,
          4L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      txid = PublicMethed.triggerContract(contractAddress,
          "depositForAnonymousNoLog()", "#", false,
          5L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      String param = "\"" + code + "\"" + "," + "\"" + code + "\"";
      txid = PublicMethed.triggerContract(contractAddress,
          "triggerStringEvent(string,string)", param, false,
          0L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);

      param = "\"" + "true1" + "\"" + "," + "\"" + "false1" + "\"";
      txid = PublicMethed.triggerContract(contractAddress,
          "triggerBoolEvent(bool,bool)", param, false,
          0L, 100000000L, event001Address, event001Key, blockingStubFull);
      logger.info(txid);
      String filename = "/Users/wangzihe/Documents/modify_fullnode/java-gsc/tooLongString.txt";
      try {
        FileReader fr = new FileReader(
            filename);
        InputStreamReader read = new InputStreamReader(new FileInputStream(new File(filename)));
        BufferedReader reader = new BufferedReader(read);
        String tooLongString = reader.readLine();
        param = "\"" + tooLongString + "\"" + "," + "\"" + tooLongString + "\"";
        txid = PublicMethed.triggerContract(contractAddress,
            "triggerStringEventAnonymous(string,string)", param, false,
            0L, 100000000L, event001Address, event001Key, blockingStubFull);
        logger.info(txid);

        txid = PublicMethed.triggerContract(contractAddress,
            "triggerStringEvent(string,string)", param, false,
            0L, 100000000L, event001Address, event001Key, blockingStubFull);
        logger.info(txid);

      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }


    }

    contractName = "addressDemo";
    code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractEventAndLog2");
    abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractEventAndLog2");
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 50, null, event001Key, event001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);

    txid = PublicMethed.triggerContract(contractAddress,
        "triggerEventBytes()", "#", false,
        0, maxFeeLimit, event001Address, event001Key, blockingStubFull);
    logger.info(txid);
  }

  @Test(enabled = true, description = "Subscribe event client")
  public void testCpuCostDetail() {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket req = context.socket(ZMQ.SUB);

    req.subscribe("blockTrigger");
    req.subscribe("transactionTrigger");
    req.subscribe("contractLogTrigger");
    req.subscribe("contractEventTrigger");
    req.monitor("inproc://reqmoniter", ZMQ.EVENT_CONNECTED | ZMQ.EVENT_DISCONNECTED);
    final ZMQ.Socket moniter = context.socket(ZMQ.PAIR);
    moniter.connect("inproc://reqmoniter");
    new Thread(new Runnable() {
      public void run() {
        while (true) {
          Event event = Event.read(moniter.base());
          System.out.println(event.event +  "  " + event.addr);
        }
      }

    }).start();
    req.connect("tcp://47.94.197.215:55555");
    req.setReceiveTimeOut(10000);

    while (true) {
      byte[] message = req.recv();
      if (message != null) {
        System.out.println("receive : " + new String(message));
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


