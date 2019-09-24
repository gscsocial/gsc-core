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

package org.gsc.runtime.vm;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;

import org.gsc.core.wrapper.*;
import org.gsc.runtime.GVMTestUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.runtime.Runtime;
import org.gsc.runtime.RuntimeImpl;
import org.gsc.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.gsc.db.dbsource.DepositImpl;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.db.TransactionTrace;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.TooBigTransactionResultException;
import org.gsc.core.exception.GSCException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.Transaction.raw;

/**
 * pragma confirmed ^0.4.24;
 *
 * contract ForI{
 *
 * uint256 public balances;
 *
 * function setCoin(uint receiver) public { for(uint i=0;i<receiver;i++){ balances = balances++; } }
 * }
 */
public class NetRuntimeTest {

  public static final long totalBalance = 1000_0000_000_000L;
  private static String dbPath = "db_NetRuntimeTest_test";
  private static String dbDirectory = "db_NetRuntimeTest_test";
  private static String indexDirectory = "index_NetRuntimeTest_test";
  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;

  private static String OwnerAddress = "GSCZV38r7WQiMaGmA56gRBVSf8sq8qbyckaT";
  private static String TriggerOwnerAddress = "GSCQd4ZYsbJ4k5Ngbb23P95ZskzDqQxFifNF";
  private static String TriggerOwnerTwoAddress = "GSCQRFkHVKrwc1AkbK9fH852t1BS7Q2FDaUz";

  static {
    Args.setParam(
        new String[]{
            "--db-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory,
            "-w"
        },
        "config-test-mainnet.conf"
    );
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    //init cpu
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1565956800000L);
    dbManager.getDynamicPropertiesStore().saveTotalCpuWeight(10_000_000L);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);

    AccountWrapper accountWrapper = new AccountWrapper(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(OwnerAddress)), AccountType.Normal,
        totalBalance);

    accountWrapper.setFrozenForCpu(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(OwnerAddress), accountWrapper);

    AccountWrapper accountWrapper2 = new AccountWrapper(
        ByteString.copyFrom("triggerOwner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(TriggerOwnerAddress)), AccountType.Normal,
        totalBalance);

    accountWrapper2.setFrozenForCpu(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(TriggerOwnerAddress), accountWrapper2);
    AccountWrapper accountWrapper3 = new AccountWrapper(
        ByteString.copyFrom("triggerOwnerAddress".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(TriggerOwnerTwoAddress)),
        AccountType.Normal,
        totalBalance);
    accountWrapper3.setNetUsage(5000L);
    accountWrapper3.setLatestConsumeFreeTime(dbManager.getWitnessController().getHeadSlot());
    accountWrapper3.setFrozenForCpu(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(TriggerOwnerTwoAddress), accountWrapper3);

    dbManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(System.currentTimeMillis() / 1000);
  }

  @Test
  public void testSuccess() {
    try {
      byte[] contractAddress = createContract();
      AccountWrapper triggerOwner = dbManager.getAccountStore()
          .get(Wallet.decodeFromBase58Check(TriggerOwnerAddress));
      long cpu = triggerOwner.getCpuUsage();
      TriggerSmartContract triggerContract = GVMTestUtils.createTriggerContract(contractAddress,
          "setCoin(uint256)", "3", false,
          0, Wallet.decodeFromBase58Check(TriggerOwnerAddress));
      Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
          Contract.newBuilder().setParameter(Any.pack(triggerContract))
              .setType(ContractType.TriggerSmartContract)).setFeeLimit(1000000000)).build();
      TransactionWrapper trxCap = new TransactionWrapper(transaction);
      TransactionTrace trace = new TransactionTrace(trxCap, dbManager);
      dbManager.consumeNet(trxCap, trace);
      BlockWrapper blockWrapper = null;
      DepositImpl deposit = DepositImpl.createRoot(dbManager);
      Runtime runtime = new RuntimeImpl(trace, blockWrapper, deposit,
          new ProgramInvokeFactoryImpl());
      trace.init(blockWrapper);
      trace.exec();
      trace.finalization();

      triggerOwner = dbManager.getAccountStore()
          .get(Wallet.decodeFromBase58Check(TriggerOwnerAddress));
      cpu = triggerOwner.getCpuUsage();
      long balance = triggerOwner.getBalance();
      Assert.assertEquals(45706, trace.getReceipt().getCpuUsageTotal());
      Assert.assertEquals(45706, cpu);
      Assert.assertEquals(totalBalance, balance);
    } catch (GSCException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testSuccessNoBandd() {
    try {
      byte[] contractAddress = createContract();
      TriggerSmartContract triggerContract = GVMTestUtils.createTriggerContract(contractAddress,
          "setCoin(uint256)", "50", false,
          0, Wallet.decodeFromBase58Check(TriggerOwnerTwoAddress));
      Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
          Contract.newBuilder().setParameter(Any.pack(triggerContract))
              .setType(ContractType.TriggerSmartContract)).setFeeLimit(1000000000)).build();
      TransactionWrapper trxCap = new TransactionWrapper(transaction);
      TransactionTrace trace = new TransactionTrace(trxCap, dbManager);
      dbManager.consumeNet(trxCap, trace);
      long net = trxCap.getSerializedSize() + Constant.MAX_RESULT_SIZE_IN_TX;
      BlockWrapper blockWrapper = null;
      DepositImpl deposit = DepositImpl.createRoot(dbManager);
      Runtime runtime = new RuntimeImpl(trace, blockWrapper, deposit,
          new ProgramInvokeFactoryImpl());
      trace.init(blockWrapper);
      trace.exec();
      trace.finalization();

      AccountWrapper triggerOwnerTwo = dbManager.getAccountStore()
          .get(Wallet.decodeFromBase58Check(TriggerOwnerTwoAddress));
      long balance = triggerOwnerTwo.getBalance();
      ReceiptWrapper receipt = trace.getReceipt();

      Assert.assertEquals(net, receipt.getNetUsage());
      Assert.assertEquals(522850, receipt.getCpuUsageTotal());
      Assert.assertEquals(50000, receipt.getCpuUsage());
      Assert.assertEquals(47285000, receipt.getCpuFee());
      Assert.assertEquals(totalBalance - receipt.getCpuFee(),
          balance);
    } catch (GSCException e) {
      Assert.assertNotNull(e);
    }
  }

  private byte[] createContract()
      throws ContractValidateException, AccountResourceInsufficientException, TooBigTransactionResultException, ContractExeException, VMIllegalException {
    AccountWrapper owner = dbManager.getAccountStore()
        .get(Wallet.decodeFromBase58Check(OwnerAddress));
    long cpu = owner.getCpuUsage();
    long balance = owner.getBalance();

    String contractName = "foriContract";
    String code = "608060405234801561001057600080fd5b50610105806100206000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680637bb98a6814604e578063866edb47146076575b600080fd5b348015605957600080fd5b50606060a0565b6040518082815260200191505060405180910390f35b348015608157600080fd5b50609e6004803603810190808035906020019092919050505060a6565b005b60005481565b60008090505b8181101560d55760008081548092919060010191905055600081905550808060010191505060ac565b50505600a165627a7a72305820f4020a69fb8504d7db776726b19e5101c3216413d7ab8e91a11c4f55f772caed0029";
    String abi = "[{\"constant\":true,\"inputs\":[],\"name\":\"balances\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"receiver\",\"type\":\"uint256\"}],\"name\":\"setCoin\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    CreateSmartContract smartContract = GVMTestUtils.createSmartContract(
        Wallet.decodeFromBase58Check(OwnerAddress), contractName, abi, code, 0, 100);
    Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
        Contract.newBuilder().setParameter(Any.pack(smartContract))
            .setType(ContractType.CreateSmartContract)).setFeeLimit(1000000000)).build();
    TransactionWrapper trxCap = new TransactionWrapper(transaction);
    TransactionTrace trace = new TransactionTrace(trxCap, dbManager);
    dbManager.consumeNet(trxCap, trace);
    BlockWrapper blockWrapper = null;
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    Runtime runtime = new RuntimeImpl(trace, blockWrapper, deposit, new ProgramInvokeFactoryImpl());
    trace.init(blockWrapper);
    trace.exec();
    trace.finalization();
    owner = dbManager.getAccountStore()
        .get(Wallet.decodeFromBase58Check(OwnerAddress));
    cpu = owner.getCpuUsage() - cpu;
    balance = balance - owner.getBalance();
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(52299, trace.getReceipt().getCpuUsageTotal());
    Assert.assertEquals(12500, cpu);
    Assert.assertEquals(397990, balance);
    Assert
        .assertEquals(52299 * Constant.DOT_PER_CPU, balance + cpu * Constant.DOT_PER_CPU);
    Assert.assertNull(runtime.getRuntimeError());
    return runtime.getResult().getContractAddress();
  }

  /**
   * destroy clear data of testing.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    ApplicationFactory.create(context).shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}