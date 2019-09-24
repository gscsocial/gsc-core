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
import org.gsc.core.wrapper.AccountWrapper;
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
 * pragma confirmed ^0.4.2;
 *
 * contract Fibonacci {
 *
 * event Notify(uint input, uint result);
 *
 * function fibonacci(uint number) constant returns(uint result) { if (number == 0) { return 0; }
 * else if (number == 1) { return 1; } else { uint256 first = 0; uint256 second = 1; uint256 ret =
 * 0; for(uint256 i = 2; i <= number; i++) { ret = first + second; first = second; second = ret; }
 * return ret; } }
 *
 * function fibonacciNotify(uint number) returns(uint result) { result = fibonacci(number);
 * Notify(number, result); } }
 */
public class NetRuntimeOutOfTimeTest {

  public static final long totalBalance = 1000_0000_000_000L;
  private static String dbPath = "db_NetRuntimeOutOfTimeTest_test";
  private static String dbDirectory = "db_NetRuntimeOutOfTimeTest_test";
  private static String indexDirectory = "index_NetRuntimeOutOfTimeTest_test";
  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;

  private static String OwnerAddress = "GSCZV38r7WQiMaGmA56gRBVSf8sq8qbyckaT";
  private String trx2ContractAddress = "GSCQd4ZYsbJ4k5Ngbb23P95ZskzDqQxFifNF";
  private static String TriggerOwnerAddress = "GSCQRFkHVKrwc1AkbK9fH852t1BS7Q2FDaUz";

  static {
    Args.setParam(
        new String[]{
            "--db-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory,
            "-w",
            "--debug"
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
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647828000L);
    dbManager.getDynamicPropertiesStore().saveTotalCpuWeight(10_000_000L);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);

    AccountWrapper accountWrapper = new AccountWrapper(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(OwnerAddress)), AccountType.Normal,
        totalBalance);

    accountWrapper.setFrozenForCpu(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(OwnerAddress), accountWrapper);

    AccountWrapper accountWrapper2 = new AccountWrapper(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(TriggerOwnerAddress)), AccountType.Normal,
        totalBalance);

    accountWrapper2.setFrozenForCpu(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(TriggerOwnerAddress), accountWrapper2);
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
      long balance = triggerOwner.getBalance();
      TriggerSmartContract triggerContract = GVMTestUtils.createTriggerContract(contractAddress,
          "fibonacciNotify(uint256)", "500000", false,
          0, Wallet.decodeFromBase58Check(TriggerOwnerAddress));
      Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
          Contract.newBuilder().setParameter(Any.pack(triggerContract))
              .setType(ContractType.TriggerSmartContract)).setFeeLimit(100000000000L)).build();
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
      cpu = triggerOwner.getCpuUsage() - cpu;
      balance = balance - triggerOwner.getBalance();
      Assert.assertNotNull(runtime.getRuntimeError());
      Assert.assertTrue(runtime.getRuntimeError().contains(" timeout "));
      Assert.assertEquals(9950000, trace.getReceipt().getCpuUsageTotal());
      Assert.assertEquals(50000, cpu);
      Assert.assertEquals(990000000, balance);
      Assert.assertEquals(9950000 * Constant.DOT_PER_CPU,
          balance + cpu * Constant.DOT_PER_CPU);
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

    String contractName = "Fibonacci3";
    String code = "608060405234801561001057600080fd5b506101ba806100206000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680633c7fdc701461005157806361047ff414610092575b600080fd5b34801561005d57600080fd5b5061007c600480360381019080803590602001909291905050506100d3565b6040518082815260200191505060405180910390f35b34801561009e57600080fd5b506100bd60048036038101908080359060200190929190505050610124565b6040518082815260200191505060405180910390f35b60006100de82610124565b90507f71e71a8458267085d5ab16980fd5f114d2d37f232479c245d523ce8d23ca40ed8282604051808381526020018281526020019250505060405180910390a1919050565b60008060008060008086141561013d5760009450610185565b600186141561014f5760019450610185565b600093506001925060009150600290505b85811115156101815782840191508293508192508080600101915050610160565b8194505b505050509190505600a165627a7a7230582071f3cf655137ce9dc32d3307fb879e65f3960769282e6e452a5f0023ea046ed20029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],\"name\":\"fibonacciNotify\",\"outputs\":[{\"name\":\"result\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],\"name\":\"fibonacci\",\"outputs\":[{\"name\":\"result\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"input\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"result\",\"type\":\"uint256\"}],\"name\":\"Notify\",\"type\":\"event\"}]";
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
    System.out.println(trace.getReceipt().getCpuUsageTotal());
    Assert.assertEquals(88529, trace.getReceipt().getCpuUsageTotal());
    Assert.assertEquals(12500, cpu);
    Assert.assertEquals(760290, balance);
    System.out.println();
    Assert.assertEquals(88529 * 10, balance + cpu * 10);
    if (runtime.getRuntimeError() != null) {
      return runtime.getResult().getContractAddress();
    }
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