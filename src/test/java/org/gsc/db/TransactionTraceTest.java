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

package org.gsc.db;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;

import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.runtime.GVMTestUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.ContractWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Account.AccountResource;
import org.gsc.protos.Protocol.Account.Frozen;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.Transaction.raw;

public class TransactionTraceTest {

  public static final long totalBalance = 1000_0000_000_000L;
  private static String dbPath = "db_TransactionTrace_test";
  private static String dbDirectory = "db_TransactionTrace_test";
  private static String indexDirectory = "index_TransactionTrace_test";
  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;
  private static ByteString ownerAddress = ByteString.copyFrom(ByteArray.fromInt(1));
  private static ByteString contractAddress = ByteString.copyFrom(ByteArray.fromInt(2));

  private static String OwnerAddress = "GSCmtiNVfUJybdYRbfV98uytyD6DCCFn63ta";
  private static String TriggerOwnerAddress = "GSCbuT1GT3zpz9pYULLBfXgFBaZtF5MhxEkQ";

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
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore().saveTotalCpuWeight(100_000L);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);

  }

  @Test
  public void testUseFee()
      throws InvalidProtocolBufferException, VMIllegalException, BalanceInsufficientException, ContractExeException, ContractValidateException {
    String contractName = "tracetestContract";
    String code = "608060405234801561001057600080fd5b5060005b6103e8811015610037576000818152602081905260409020819055600a01610014565b5061010f806100476000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416634903b0d181146057578063da31158814607e578063fe4ba936146093575b600080fd5b348015606257600080fd5b50606c60043560ad565b60408051918252519081900360200190f35b348015608957600080fd5b50606c60043560bf565b348015609e57600080fd5b5060ab60043560243560d1565b005b60006020819052908152604090205481565b60009081526020819052604090205490565b600091825260208290526040909120555600a165627a7a723058200596e6c0a5371c2c533eb97ba4c1c19b0521750a5624cb5d2e93249c8b7219d20029";
    String abi = "[{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"balances\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"account\",\"type\":\"uint256\"}],\"name\":\"getCoin\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"receiver\",\"type\":\"uint256\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"setCoin\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    CreateSmartContract smartContract = GVMTestUtils.createSmartContract(
        Wallet.decodeFromBase58Check(OwnerAddress), contractName, abi, code, 0, 100);
    Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
        Contract.newBuilder().setParameter(Any.pack(smartContract))
            .setType(ContractType.CreateSmartContract)).setFeeLimit(1000000000)).build();

    deployInit(transaction);
  }

  @Test
  public void testTriggerUseFee()
      throws InvalidProtocolBufferException, VMIllegalException, ContractExeException, ContractValidateException, BalanceInsufficientException {
    String contractName = "tracetestContract";
    String code = "608060405234801561001057600080fd5b5060005b6103e8811015610037576000818152602081905260409020819055600a01610014565b5061010f806100476000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416634903b0d181146057578063da31158814607e578063fe4ba936146093575b600080fd5b348015606257600080fd5b50606c60043560ad565b60408051918252519081900360200190f35b348015608957600080fd5b50606c60043560bf565b348015609e57600080fd5b5060ab60043560243560d1565b005b60006020819052908152604090205481565b60009081526020819052604090205490565b600091825260208290526040909120555600a165627a7a723058200596e6c0a5371c2c533eb97ba4c1c19b0521750a5624cb5d2e93249c8b7219d20029";
    String abi = "[{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"balances\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"account\",\"type\":\"uint256\"}],\"name\":\"getCoin\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"receiver\",\"type\":\"uint256\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"setCoin\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    CreateSmartContract smartContract = GVMTestUtils.createSmartContract(
        Wallet.decodeFromBase58Check(OwnerAddress), contractName, abi, code, 0, 100);
    Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
        Contract.newBuilder().setParameter(Any.pack(smartContract))
            .setType(ContractType.CreateSmartContract)).setFeeLimit(1000000000)
        .setTimestamp(System.currentTimeMillis())).build();

    byte[] contractAddress = deployInit(transaction);
    AccountWrapper ownerWrapper = new AccountWrapper(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(TriggerOwnerAddress)), AccountType.Normal,
        totalBalance);
    AccountWrapper originWrapper = new AccountWrapper(ByteString.copyFrom("origin".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(OwnerAddress)), AccountType.Normal,
        totalBalance);
    ownerWrapper.setFrozenForCpu(5_000_000_000L, 0L);
    originWrapper.setFrozenForCpu(5_000_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(TriggerOwnerAddress), ownerWrapper);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(TriggerOwnerAddress), originWrapper);
    TriggerSmartContract triggerContract = GVMTestUtils.createTriggerContract(contractAddress,
        "setCoin(uint256,uint256)", "133,133", false,
        0, Wallet.decodeFromBase58Check(TriggerOwnerAddress));
    Transaction transaction2 = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
        Contract.newBuilder().setParameter(Any.pack(triggerContract))
            .setType(ContractType.TriggerSmartContract)).setFeeLimit(1000000000L)).build();
    TransactionWrapper transactionWrapper = new TransactionWrapper(transaction2);
    TransactionTrace trace = new TransactionTrace(transactionWrapper, dbManager);

    trace.init(null);
    trace.exec();
    trace.pay();
    Assert.assertEquals(20252, trace.getReceipt().getCpuUsage());
    Assert.assertEquals(0, trace.getReceipt().getCpuFee());
    ownerWrapper = dbManager.getAccountStore().get(ownerWrapper.getAddress().toByteArray());
    Assert.assertEquals(totalBalance,
        trace.getReceipt().getCpuFee() + ownerWrapper
            .getBalance());
  }

  @Test
  public void testTriggerUseUsage()
      throws VMIllegalException, ContractExeException, ContractValidateException, BalanceInsufficientException {
    String contractName = "tracetestContract";
    String code = "608060405234801561001057600080fd5b5060005b6103e8811015610037576000818152602081905260409020819055600a01610014565b5061010f806100476000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416634903b0d181146057578063da31158814607e578063fe4ba936146093575b600080fd5b348015606257600080fd5b50606c60043560ad565b60408051918252519081900360200190f35b348015608957600080fd5b50606c60043560bf565b348015609e57600080fd5b5060ab60043560243560d1565b005b60006020819052908152604090205481565b60009081526020819052604090205490565b600091825260208290526040909120555600a165627a7a723058200596e6c0a5371c2c533eb97ba4c1c19b0521750a5624cb5d2e93249c8b7219d20029";
    String abi = "[{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"balances\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"account\",\"type\":\"uint256\"}],\"name\":\"getCoin\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"receiver\",\"type\":\"uint256\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"setCoin\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    CreateSmartContract smartContract = GVMTestUtils.createSmartContract(
        Wallet.decodeFromBase58Check(OwnerAddress), contractName, abi, code, 0, 100);
    Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
        Contract.newBuilder().setParameter(Any.pack(smartContract))
            .setType(ContractType.CreateSmartContract)).setFeeLimit(1000000000)
        .setTimestamp(System.currentTimeMillis()))
        .build();

    byte[] contractAddress = deployInit(transaction);
    AccountWrapper accountWrapper = new AccountWrapper(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(TriggerOwnerAddress)),
        AccountType.Normal,
        totalBalance);

    accountWrapper.setFrozenForCpu(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(TriggerOwnerAddress), accountWrapper);
    TriggerSmartContract triggerContract = GVMTestUtils.createTriggerContract(contractAddress,
        "setCoin(uint256,uint256)", "133,133", false,
        0, Wallet.decodeFromBase58Check(TriggerOwnerAddress));
    Transaction transaction2 = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
        Contract.newBuilder().setParameter(Any.pack(triggerContract))
            .setType(ContractType.TriggerSmartContract)).setFeeLimit(1000000000L)).build();
    TransactionWrapper transactionWrapper = new TransactionWrapper(transaction2);
    TransactionTrace trace = new TransactionTrace(transactionWrapper, dbManager);

    trace.init(null);
    trace.exec();
    trace.pay();
    Assert.assertEquals(20252, trace.getReceipt().getCpuUsage());
    Assert.assertEquals(0, trace.getReceipt().getCpuFee());
    Assert.assertEquals(2025200,
        trace.getReceipt().getCpuUsage() * 100 + trace.getReceipt().getCpuFee());
    accountWrapper = dbManager.getAccountStore().get(accountWrapper.getAddress().toByteArray());
    Assert.assertEquals(totalBalance,
        accountWrapper.getBalance() + trace.getReceipt().getCpuFee());

  }

  private byte[] deployInit(Transaction transaction)
      throws VMIllegalException, ContractExeException, ContractValidateException, BalanceInsufficientException {

    AccountWrapper accountWrapper = new AccountWrapper(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(OwnerAddress)), AccountType.Normal,
        totalBalance);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(OwnerAddress), accountWrapper);

    TransactionWrapper transactionWrapper = new TransactionWrapper(transaction);
    TransactionTrace trace = new TransactionTrace(transactionWrapper, dbManager);

    trace.init(null);
    trace.exec();
    trace.pay();
    Assert.assertEquals(0, trace.getReceipt().getCpuUsage());
    Assert.assertEquals(20508310L, trace.getReceipt().getCpuFee());
    accountWrapper = dbManager.getAccountStore().get(accountWrapper.getAddress().toByteArray());
    Assert.assertEquals(totalBalance,
        trace.getReceipt().getCpuFee() + accountWrapper
            .getBalance());
    return trace.getRuntime().getResult().getContractAddress();

  }

  @Test
  public void testUseUsage()
          throws VMIllegalException, BalanceInsufficientException, ContractValidateException, ContractExeException {

    AccountWrapper accountWrapper = new AccountWrapper(ByteString.copyFrom("owner".getBytes()),
            ByteString.copyFrom(Wallet.decodeFromBase58Check(OwnerAddress)), AccountType.Normal,
            totalBalance);

    accountWrapper.setFrozenForCpu(5_000_000_000L, 0L);
    dbManager.getAccountStore()
            .put(Wallet.decodeFromBase58Check(OwnerAddress), accountWrapper);
    String contractName = "tracetestContract";
    String code = "608060405234801561001057600080fd5b5060005b6103e8811015610037576000818152602081905260409020819055600a01610014565b5061010f806100476000396000f30060806040526004361060525763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416634903b0d181146057578063da31158814607e578063fe4ba936146093575b600080fd5b348015606257600080fd5b50606c60043560ad565b60408051918252519081900360200190f35b348015608957600080fd5b50606c60043560bf565b348015609e57600080fd5b5060ab60043560243560d1565b005b60006020819052908152604090205481565b60009081526020819052604090205490565b600091825260208290526040909120555600a165627a7a723058200596e6c0a5371c2c533eb97ba4c1c19b0521750a5624cb5d2e93249c8b7219d20029";
    String abi = "[{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"balances\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"account\",\"type\":\"uint256\"}],\"name\":\"getCoin\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"receiver\",\"type\":\"uint256\"},{\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"setCoin\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    CreateSmartContract smartContract = GVMTestUtils.createSmartContract(
            Wallet.decodeFromBase58Check(OwnerAddress), contractName, abi, code, 0, 100);
    Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
            Contract.newBuilder().setParameter(Any.pack(smartContract))
                    .setType(ContractType.CreateSmartContract)).setFeeLimit(1000000000)
            .setTimestamp(System.currentTimeMillis()))
            .build();

    TransactionWrapper transactionWrapper = new TransactionWrapper(transaction);
    TransactionTrace trace = new TransactionTrace(transactionWrapper, dbManager);

    trace.init(null);
    trace.exec();
    trace.pay();
    Assert.assertEquals(2050831L, trace.getReceipt().getCpuUsage());
    Assert.assertEquals(0L, trace.getReceipt().getCpuFee());
    Assert.assertEquals(205083100L,
            trace.getReceipt().getCpuUsage() * 100 + trace.getReceipt().getCpuFee());
    accountWrapper = dbManager.getAccountStore().get(accountWrapper.getAddress().toByteArray());
    Assert.assertEquals(totalBalance,
            accountWrapper.getBalance() + trace.getReceipt().getCpuFee());

  }

  @Test
  public void testPay() throws BalanceInsufficientException {
    Account account = Account.newBuilder()
        .setAddress(ownerAddress)
        .setBalance(1000000)
        .setAccountResource(
            AccountResource.newBuilder()
                .setCpuUsage(1111111L)
                .setFrozenBalanceForCpu(
                    Frozen.newBuilder()
                        .setExpireTime(100000)
                        .setFrozenBalance(100000)
                        .build())
                .build()).build();

    AccountWrapper accountWrapper = new AccountWrapper(account);
    dbManager.getAccountStore().put(accountWrapper.getAddress().toByteArray(), accountWrapper);
    TriggerSmartContract contract = TriggerSmartContract.newBuilder()
        .setContractAddress(contractAddress)
        .setOwnerAddress(ownerAddress)
        .build();

    SmartContract smartContract = SmartContract.newBuilder()
        .setOriginAddress(ownerAddress)
        .setContractAddress(contractAddress)
        .build();

    CreateSmartContract createSmartContract = CreateSmartContract.newBuilder()
        .setOwnerAddress(ownerAddress)
        .setNewContract(smartContract)
        .build();

    Transaction transaction = Transaction.newBuilder()
        .setRawData(
            raw.newBuilder()
                .addContract(
                    Contract.newBuilder()
                        .setParameter(Any.pack(contract))
                        .setType(ContractType.TriggerSmartContract)
                        .build())
                .build()
        )
        .build();

    dbManager.getContractStore().put(
        contractAddress.toByteArray(),
        new ContractWrapper(smartContract));

    TransactionWrapper transactionWrapper = new TransactionWrapper(transaction);
    TransactionTrace transactionTrace = new TransactionTrace(transactionWrapper, dbManager);
    transactionTrace.setBill(0L);
    transactionTrace.pay();
    AccountWrapper accountWrapper1 = dbManager.getAccountStore().get(ownerAddress.toByteArray());
  }

  /**
   * destroy clear data of testing.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}
