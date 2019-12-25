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

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.runtime.GVMTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.runtime.GVMTestResult;
import org.gsc.db.dbsource.DepositImpl;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.protos.Protocol.AccountType;

@Slf4j

public class ChargeTest {

  private Manager dbManager;
  private GSCApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "db_ChargeTest";
  private String OWNER_ADDRESS;
  private Application AppT;
  private long totalBalance = 100_000_000_000_000L;


  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--db-directory", dbPath, "--debug"},
        Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), totalBalance);
    deposit.commit();
  }

  // pragma confirmed ^0.4.16;
  //
  // contract subContract {
  //   constructor () payable {}
  // }
  //
  // contract TestOverflowContract {
  //
  //   function testOverflow() payable {
  //     subContract sc = (new subContract).value(10)();
  //   }
  // }

  @Test
  public void testOverflow()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    long value = 0;
    long feeLimit = 1_000_000_000L; // dot
    long consumeUserResourcePercent = 100;

    String contractName = "testOverflow";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testOverflow\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610100806100206000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680638040cac4146044575b600080fd5b604a604c565b005b6000678ac7230489e80000605d607f565b6040518091039082f0801580156077573d6000803e3d6000fd5b509050905050565b60405160468061008f833901905600608060405260358060116000396000f3006080604052600080fd00a165627a7a723058201738d6aa899dc00d4e99de944eb74d30a9ba1fcae37b99dc6299d95e992ca8b40029a165627a7a7230582068390137ba70dfc460810603eba8500b050ed3cd01e66f55ec07d387ec1cd2750029";
    String libraryAddressPair = null;

    GVMTestResult result = GVMTestUtils
        .deployContractAndReturnGvmTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            dbManager, null);

    long expectCpuUsageTotal = 51293; // 200 * code.length() + 93
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - expectCpuUsageTotal * 10);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = GVMTestUtils.parseAbi("testOverflow()", "");
    result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 20_000_000_000L, feeLimit, dbManager, null);

    long expectCpuUsageTotal2 = feeLimit / 10;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal2);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof ArithmeticException);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - (expectCpuUsageTotal + expectCpuUsageTotal2) * 10);
  }

  // pragma confirmed ^0.4.16;
  //
  // contract subContract {
  //   constructor () payable {}
  // }
  //
  // contract TestNegativeContract {
  //
  //   event logger(uint256);
  //   function testNegative() payable {
  //     int256 a = -1;
  //     logger(uint256(a));
  //     subContract sc = (new subContract).value(uint(a))();
  //   }
  // }

  @Test
  public void testNegative()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    long value = 0;
    long feeLimit = 1_000_000_000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "testNegative";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testNegative\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"logger\",\"type\":\"event\"}]";
    String code = "608060405234801561001057600080fd5b50610154806100206000396000f300608060405260043610603f576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680638f7d8a1c146044575b600080fd5b604a604c565b005b6000807fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff91507f3a26492830c137b6cedfdd0e23db0e9c7c214e4fd1de32de8ceece1678b771b3826040518082815260200191505060405180910390a18160b060d3565b6040518091039082f08015801560ca573d6000803e3d6000fd5b50905090505050565b6040516046806100e3833901905600608060405260358060116000396000f3006080604052600080fd00a165627a7a72305820ef54aac72efff56dbe894e7218d009a87368bb70338bb385db5d3dec9927bc2c0029a165627a7a723058201620679ac2ae640d0a6c26e9cb4523e98eb0de8fff26975c5bb4c7fda1c98d720029";
    String libraryAddressPair = null;

    GVMTestResult result = GVMTestUtils
        .deployContractAndReturnGvmTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            dbManager, null);

    long expectCpuUsageTotal = 68111;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - expectCpuUsageTotal * 10);
    byte[] contractAddress = result.getContractAddress();

    /* ======================================CALL testNegative() with 0 callvalue ================================ */
    byte[] triggerData = GVMTestUtils.parseAbi("testNegative()", "");
    result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, value, feeLimit, dbManager, null);

    long expectCpuUsageTotal2 = feeLimit / 10;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal2);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof ArithmeticException);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - (expectCpuUsageTotal + expectCpuUsageTotal2) * 10);

    /* ======================================CALL testNegative() with -100 callvalue ================================ */
    triggerData = GVMTestUtils.parseAbi("testNegative()", "");
    result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 100, feeLimit, dbManager, null);

    long expectCpuUsageTotal3 = feeLimit / 10;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal3);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof ArithmeticException);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance
            - (expectCpuUsageTotal + expectCpuUsageTotal2 + expectCpuUsageTotal3) * 10);

  }

  @Test
  @Ignore
  public void testFallback()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException {
  }

  @Test
  public void testCallDepth()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    long value = 0;
    long feeLimit = 1_000_000_000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "testCallDepth";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"int256\"}],\"name\":\"CallstackExploit\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"int256\"}],\"name\":\"Call\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610174806100206000396000f3006080604052600436106100325763ffffffff60e060020a6000350416633a3c47188114610037578063eede0f0114610051575b600080fd5b34801561004357600080fd5b5061004f600435610069565b005b34801561005d57600080fd5b5061004f6004356100d7565b60008113156100d45730633a3c47186107d05a03600184036040518363ffffffff1660e060020a02815260040180828152602001915050600060405180830381600088803b1580156100ba57600080fd5b5087f11580156100ce573d6000803e3d6000fd5b50505050505b50565b3073ffffffffffffffffffffffffffffffffffffffff16633a3c4718826040518263ffffffff1660e060020a02815260040180828152602001915050600060405180830381600087803b15801561012d57600080fd5b505af1158015610141573d6000803e3d6000fd5b50505050505600a165627a7a72305820510367f4437b1af16931cacc744eb6f3102d72f0c369aa795a4dc49a7f90a3e90029";
    String libraryAddressPair = null;

    GVMTestResult result = GVMTestUtils
        .deployContractAndReturnGvmTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            dbManager, null);

    long expectCpuUsageTotal = 74517;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - expectCpuUsageTotal * 10);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    String params = "0000000000000000000000000000000000000000000000000000000000002710";
    // byte[] triggerData = GVMTestUtils.parseAbi("CallstackExploit(int)", params);
    byte[] triggerData = GVMTestUtils.parseAbi("Call(int256)", params);
    result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, value, feeLimit, dbManager, null);

    long expectCpuUsageTotal2 = 27743;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal2);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertEquals(result.getRuntime().getResult().getException(), null);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - (expectCpuUsageTotal + expectCpuUsageTotal2) * 10);

  }

  // contract subContract {
  //
  //   function CallstackExploit(uint256 counter) external {
  //     if (counter > 0) {
  //       this.CallstackExploit.gas(msg.gas - 2000)(counter - 1);
  //     } else {}
  //   }
  //
  //   function Call(uint256 counter) {
  //
  //     if (counter <= 20) {
  //       this.CallstackExploit(counter + 20);
  //     }
  //     else {
  //       for (uint256 i = 0; i < counter; i++) {
  //         this.Call(i - 20);
  //       }
  //     }
  //   }
  // }
  //
  // contract TestCallDepthAndWidthContract {
  //
  //   subContract sc = new subContract();
  //
  //   function CallstackExploit(uint256 counter) external {
  //     if (counter > 0) {
  //       this.CallstackExploit.gas(msg.gas - 2000)(counter - 1);
  //     } else {}
  //   }
  //
  //   function Call(uint256 counter) {
  //
  //     for (uint256 i = 0; i < counter; i++) {
  //       this.CallstackExploit(i + 20);
  //       sc.Call(i + 10);
  //     }
  //
  //   }
  // }

  @Test
  public void testCallDepthAndWidth()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    long value = 0;
    long feeLimit = 1_000_000_000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "testCallDepthAndWidth";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"uint256\"}],\"name\":\"CallstackExploit\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"uint256\"}],\"name\":\"Call\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405261000c61005b565b604051809103906000f080158015610028573d6000803e3d6000fd5b5060008054600160a060020a031916600160a060020a039290921691909117905534801561005557600080fd5b5061006b565b604051610265806102c683390190565b61024c8061007a6000396000f30060806040526004361061004b5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166370ed9d828114610050578063f84df1931461006a575b600080fd5b34801561005c57600080fd5b50610068600435610082565b005b34801561007657600080fd5b50610068600435610109565b600081111561010657306370ed9d826107d05a03600184036040518363ffffffff167c010000000000000000000000000000000000000000000000000000000002815260040180828152602001915050600060405180830381600088803b1580156100ec57600080fd5b5087f1158015610100573d6000803e3d6000fd5b50505050505b50565b60005b8181101561021c57604080517f70ed9d82000000000000000000000000000000000000000000000000000000008152601483016004820152905130916370ed9d8291602480830192600092919082900301818387803b15801561016e57600080fd5b505af1158015610182573d6000803e3d6000fd5b505060008054604080517ff84df193000000000000000000000000000000000000000000000000000000008152600a87016004820152905173ffffffffffffffffffffffffffffffffffffffff909216945063f84df1939350602480820193929182900301818387803b1580156101f857600080fd5b505af115801561020c573d6000803e3d6000fd5b50506001909201915061010c9050565b50505600a165627a7a72305820ad701f54dc539d976cc2af0443d5d190dbe727ce2e24d66f3e2390dfd79859640029608060405234801561001057600080fd5b50610245806100206000396000f30060806040526004361061004b5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166370ed9d828114610050578063f84df1931461006a575b600080fd5b34801561005c57600080fd5b50610068600435610082565b005b34801561007657600080fd5b50610068600435610109565b600081111561010657306370ed9d826107d05a03600184036040518363ffffffff167c010000000000000000000000000000000000000000000000000000000002815260040180828152602001915050600060405180830381600088803b1580156100ec57600080fd5b5087f1158015610100573d6000803e3d6000fd5b50505050505b50565b60006014821161018a57604080517f70ed9d82000000000000000000000000000000000000000000000000000000008152601484016004820152905130916370ed9d8291602480830192600092919082900301818387803b15801561016d57600080fd5b505af1158015610181573d6000803e3d6000fd5b50505050610215565b5060005b8181101561021557604080517ff84df193000000000000000000000000000000000000000000000000000000008152601319830160048201529051309163f84df19391602480830192600092919082900301818387803b1580156101f157600080fd5b505af1158015610205573d6000803e3d6000fd5b50506001909201915061018e9050565b50505600a165627a7a72305820a9e7e1401001d6c131ebf4727fbcedede08d16416dc0447cef60e0b9516c6a260029";
    String libraryAddressPair = null;

    GVMTestResult result = GVMTestUtils
        .deployContractAndReturnGvmTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            dbManager, null);

    long expectCpuUsageTotal = 286450;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - expectCpuUsageTotal * 10);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    String params = "000000000000000000000000000000000000000000000000000000000000000a";
    // byte[] triggerData = GVMTestUtils.parseAbi("CallstackExploit(int)", params);
    byte[] triggerData = GVMTestUtils.parseAbi("Call(uint256)", params);
    result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, value, feeLimit, dbManager, null);

    long expectCpuUsageTotal2 = 243698;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal2);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertEquals(result.getRuntime().getResult().getException(), null);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - (expectCpuUsageTotal + expectCpuUsageTotal2) * 10);

  }

  @Test
  public void testCreateDepthAndWidth()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    long value = 0;
    long feeLimit = 1_000_000_000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "testCallDepthAndWidth";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"counter\",\"type\":\"uint256\"}],\"name\":\"testCreate\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b506103f0806100206000396000f3006080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663b505dee58114610045575b600080fd5b34801561005157600080fd5b5061005d60043561005f565b005b6000805b828210156101255761007361012a565b604051809103906000f08015801561008f573d6000803e3d6000fd5b5090508073ffffffffffffffffffffffffffffffffffffffff1663da6d107a836040518263ffffffff167c010000000000000000000000000000000000000000000000000000000002815260040180828152602001915050600060405180830381600087803b15801561010157600080fd5b505af1158015610115573d6000803e3d6000fd5b5050600190930192506100639050565b505050565b60405161028a8061013b833901905600608060405234801561001057600080fd5b5061026a806100206000396000f3006080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663da6d107a8114610045575b600080fd5b34801561005157600080fd5b5061005d60043561005f565b005b60008082111561010f573063da6d107a6107d05a03600185036040518363ffffffff167c010000000000000000000000000000000000000000000000000000000002815260040180828152602001915050600060405180830381600088803b1580156100ca57600080fd5b5087f11580156100de573d6000803e3d6000fd5b50505050506100eb61013b565b604051809103906000f080158015610107573d6000803e3d6000fd5b509050610137565b61011761013b565b604051809103906000f080158015610133573d6000803e3d6000fd5b5090505b5050565b60405160f48061014b8339019056006080604052348015600f57600080fd5b506000805b6064821015604a5760226050565b604051809103906000f080158015603d573d6000803e3d6000fd5b5060019092019190506014565b5050605f565b6040516052806100a283390190565b60358061006d6000396000f3006080604052600080fd00a165627a7a723058203565a8abc553526f8113ab8a3f432963d88cee07cafce0ebfc61173d3797b84700296080604052348015600f57600080fd5b50603580601d6000396000f3006080604052600080fd00a165627a7a723058204855bba321c7dee00dfa91caa8926cf07c38c541a11ba36d3b2a4687acaa909c0029a165627a7a7230582093af601a9196cffc9bf82bcae83557d7f5aedeec639129c27826f38c1e2a2ea00029a165627a7a7230582071d51c39c93b0aba5baeacea0b2bd5ca5342d028bb834046eca92975a3517a4c0029";
    String libraryAddressPair = null;

    GVMTestResult result = GVMTestUtils
        .deployContractAndReturnGvmTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            dbManager, null);

    long expectCpuUsageTotal = 201839;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - expectCpuUsageTotal * 10);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    String params = "0000000000000000000000000000000000000000000000000000000000000001";
    // byte[] triggerData = GVMTestUtils.parseAbi("CallstackExploit(int)", params);
    byte[] triggerData = GVMTestUtils.parseAbi("testCreate(uint256)", params);
    result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, value, feeLimit, dbManager, null);

    long expectCpuUsageTotal2 = 4481164;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal2);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertEquals(result.getRuntime().getResult().getException(), null);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - (expectCpuUsageTotal + expectCpuUsageTotal2) * 10);

  }

  /**
   * Release resources.
   */
  @After
  public void destroy() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}
