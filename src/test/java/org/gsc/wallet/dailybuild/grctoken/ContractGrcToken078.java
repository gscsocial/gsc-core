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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.TransactionInfo;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter.CommonConstant;
import org.gsc.wallet.common.client.utils.Base58;
import org.gsc.wallet.common.client.utils.PublicMethed;

@Slf4j

public class ContractGrcToken078 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelConfirmed = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] internalTxsAddress = ecKey1.getAddress();
  String testKeyForinternalTxsAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String priKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForinternalTxsAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
  }


  @Test(enabled = true, description = "Origin test call")
  public void testOriginCall001() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractGrcToken078.sol";
    String contractName = "callerContract";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
    contractAddress = infoById.get().getContractAddress().toByteArray();

    String filePath1 = "./src/test/resources/soliditycode_v0.5.4/contractGrcToken078.sol";
    String contractName1 = "calledContract";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath1, contractName1);

    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();

    txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
    byte[] contractAddress1;
    contractAddress1 = infoById.get().getContractAddress().toByteArray();


    String filePath2 = "./src/test/resources/soliditycode_v0.5.4/contractGrcToken078.sol";
    String contractName2 = "c";
    HashMap retMap2 = PublicMethed.getByCodeAbi(filePath2, contractName2);

    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abi").toString();

    txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    byte[] contractAddress2 = infoById.get().getContractAddress().toByteArray();
    logger.info("infoById : " + infoById);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";

    String txid2 = "";
    txid2 = PublicMethed.triggerContract(contractAddress,
        "sendToB2(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    logger.info("Trigger InfobyId: " + infoById2);
    Account info1 = PublicMethed.queryAccount(internalTxsAddress, blockingStubFull);
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(internalTxsAddress,
        blockingStubFull);
    logger.info("getCpuUsed  " + resourceInfo1.getCpuUsed());
    logger.info("getCpuLimit  " + resourceInfo1.getCpuLimit());
    Assert.assertTrue(infoById2.get().getResultValue() == 0);


  }

  @Test(enabled = true, description = "Origin test delegatecall")
  public void testOriginDelegatecall001() {
    PublicMethed
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "./src/test/resources/soliditycode_v0.5.4/contractGrcToken078.sol";
    String contractName = "callerContract";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();

    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
    contractAddress = infoById.get().getContractAddress().toByteArray();

    String filePath1 = "./src/test/resources/soliditycode_v0.5.4/contractGrcToken078.sol";
    String contractName1 = "calledContract";
    HashMap retMap1 = PublicMethed.getByCodeAbi(filePath1, contractName1);

    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abi").toString();
    txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
    byte[] contractAddress1;
    contractAddress1 = infoById.get().getContractAddress().toByteArray();

    String filePath2 = "./src/test/resources/soliditycode_v0.5.4/contractGrcToken078.sol";
    String contractName2 = "c";
    HashMap retMap2 = PublicMethed.getByCodeAbi(filePath2, contractName2);

    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abi").toString();
    txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById : " + infoById);
    byte[] contractAddress2 = infoById.get().getContractAddress().toByteArray();

    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";

    String txid2 = "";
    txid2 = PublicMethed.triggerContract(contractAddress,
        "sendToB(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid2, blockingStubFull);
    logger.info("infoById : " + infoById2);

    Assert.assertTrue(infoById2.get().getResultValue() == 0);


  }

  private List<String> getStrings(byte[] data) {
    int index = 0;
    List<String> ret = new ArrayList<>();
    while (index < data.length) {
      ret.add(byte2HexStr(data, index, 32));
      index += 32;
    }
    return ret;
  }

  /**
   * constructor.
   */

  public static String byte2HexStr(byte[] b, int offset, int length) {
    String stmp = "";
    StringBuilder sb = new StringBuilder("");
    for (int n = offset; n < offset + length && n < b.length; n++) {
      stmp = Integer.toHexString(b[n] & 0xFF);
      sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
    }
    return sb.toString().toUpperCase().trim();
  }

  /**
   * constructor.
   */

  public byte[] subByte(byte[] b, int off, int length) {
    byte[] b1 = new byte[length];
    System.arraycopy(b, off, b1, 0, length);
    return b1;

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
