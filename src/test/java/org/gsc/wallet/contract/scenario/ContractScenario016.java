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

package org.gsc.wallet.contract.scenario;

import static org.gsc.protos.Protocol.Transaction.Result.contractResult.BAD_JUMP_DESTINATION_VALUE;
import static org.gsc.protos.Protocol.Transaction.Result.contractResult.OUT_OF_CPU_VALUE;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.gsc.wallet.common.client.Configuration;
import org.gsc.wallet.common.client.Parameter;
import org.gsc.wallet.common.client.utils.PublicMethed;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Result.contractResult;
import org.gsc.protos.Protocol.TransactionInfo;

@Slf4j
public class ContractScenario016 {
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
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
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String compilerVersion = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.solidityCompilerVersion");


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForGrammarAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "ContractResult is BAD_JUMP_DESTINATION")
  public void test1Grammar001() {
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName = "Test";

    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600"
        + "080fd5b5061011f8061003a6000396000f30060806040526004361060485763ffffffff7c01000000000000"
        + "000000000000000000000000000000000000000000006000350416634ef5a0088114604d5780639093b95b1"
        + "4608c575b600080fd5b348015605857600080fd5b50d38015606457600080fd5b50d28015607057600080fd"
        + "5b50607a60043560b8565b60408051918252519081900360200190f35b348015609757600080fd5b50d3801"
        + "560a357600080fd5b50d2801560af57600080fd5b5060b660ee565b005b6000606082604051908082528060"
        + "20026020018201604052801560e5578160200160208202803883390190505b50905050919050565b6001805"
        + "600a165627a7a7230582092ba162087e13f41c6d6c00ba493edc5a5a6250a3840ece5f99aa38b66366a7000"
        + "29";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"x\",\"type\":\"uint256\"}],\"name\""
        + ":\"testOutOfMem\",\"outputs\":[{\"name\":\"r\",\"type\":\"bytes32\"}],\"payable\":false"
        + ",\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs"
        + "\":[],\"name\":\"testBadJumpDestination\",\"outputs\":[],\"payable\":false,\"stateMutab"
        + "ility\":\"nonpayable\",\"type\":\"function\"}]";

    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress, grammarAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    org.testng.Assert.assertTrue(smartContract.getAbi().toString() != null);
    String txid = null;
    Optional<TransactionInfo> infoById = null;
    txid = PublicMethed.triggerContract(contractAddress,
        "testBadJumpDestination()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("Txid is " + txid);
    logger.info("Trigger cputotal is " + infoById.get().getReceipt().getCpuUsageTotal());

    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet：" + byId.get().getRet(0));
    logger.info("getNumber：" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue：" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet：" + byId.get().getRet(0).getContractRet());
    logger.info("ById：" + byId);

    logger.info("infoById：" + infoById);

    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), BAD_JUMP_DESTINATION_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.BAD_JUMP_DESTINATION);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert
        .assertEquals(contractResult.BAD_JUMP_DESTINATION, infoById.get().getReceipt().getResult());

    Assert.assertEquals(byId.get().getRet(0).getRet().getNumber(), 0);
    Assert.assertEquals(byId.get().getRet(0).getRetValue(), 0);
  }

  @Test(enabled = true, description = "ContractResult is OUT_OF_CPU")
  public void test2Grammar002() {

    String filePath = "src/test/resources/soliditycode_v0.5.4/contractUnknownException.sol";
    String contractName = "testC";
    HashMap retMap = PublicMethed.getByCodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abi").toString();
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("Trigger cputotal is " + infoById.get().getReceipt().getCpuUsageTotal());

    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet：" + byId.get().getRet(0));
    logger.info("getNumber：" + byId.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue：" + byId.get().getRet(0).getContractRetValue());
    logger.info("getContractRet：" + byId.get().getRet(0).getContractRet());
    logger.info("ById：" + byId.toString());

    logger.info("infoById：" + infoById);

    Assert.assertEquals(byId.get().getRet(0).getContractRetValue(), OUT_OF_CPU_VALUE);
    Assert.assertEquals(byId.get().getRet(0).getContractRet(), contractResult.OUT_OF_CPU);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert
        .assertEquals(contractResult.OUT_OF_CPU, infoById.get().getReceipt().getResult());

    Assert.assertEquals(byId.get().getRet(0).getRet().getNumber(), 0);
    Assert.assertEquals(byId.get().getRet(0).getRetValue(), 0);

  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
