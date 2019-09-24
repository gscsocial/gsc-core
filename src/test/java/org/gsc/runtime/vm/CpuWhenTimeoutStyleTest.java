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
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.runtime.GVMTestResult;
import org.gsc.runtime.vm.program.Program.OutOfCpuException;
import org.gsc.runtime.vm.program.Program.OutOfTimeException;
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
public class CpuWhenTimeoutStyleTest {

  private Manager dbManager;
  private GSCApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "db_CPUTimeTest";
  private String OWNER_ADDRESS;
  private Application AppT;
  private long totalBalance = 30_000_000_000_000L;


  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--db-directory", dbPath},
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

  // confirmed for endlessLoopTest
  // pragma confirmed ^0.4.0;
  //
  // contract TestForEndlessLoop {
  //
  //   uint256 vote;
  //   constructor () public {
  //     vote = 0;
  //   }
  //
  //   function getVote() public constant returns (uint256 _vote) {
  //     _vote = vote;
  //   }
  //
  //   function setVote(uint256 _vote) public {
  //     vote = _vote;
  //     while(true)
  //     {
  //       vote += 1;
  //     }
  //   }
  // }

  @Test
  public void endlessLoopTest()
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {

    long value = 0;
    long feeLimit = 1000_000_000L;
    byte[] address = Hex.decode(OWNER_ADDRESS);
    long consumeUserResourcePercent = 0;
    GVMTestResult result = deployEndlessLoopContract(value, feeLimit,
        consumeUserResourcePercent);

    if (null != result.getRuntime().getResult().getException()) {
      long expectCpuUsageTotal = feeLimit / 100;
      Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal);
      Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
          totalBalance - expectCpuUsageTotal * 10);
      return;
    }
    long expectCpuUsageTotal = 55107;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal);
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - expectCpuUsageTotal * 10);

    byte[] contractAddress = result.getContractAddress();

    /* =================================== CALL setVote(uint256) =================================== */
    String params = "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = GVMTestUtils.parseAbi("setVote(uint256)", params);
    boolean haveException = false;
    result = GVMTestUtils
        .triggerContractAndReturnGvmTestResult(Hex.decode(OWNER_ADDRESS), contractAddress,
            triggerData, value, feeLimit, dbManager, null);

    long expectCpuUsageTotal2 = feeLimit / 10;
    Assert.assertEquals(result.getReceipt().getCpuUsageTotal(), expectCpuUsageTotal2);
    Exception exception = result.getRuntime().getResult().getException();
    Assert.assertTrue((exception instanceof OutOfTimeException)
        || (exception instanceof OutOfCpuException));
    Assert.assertEquals(dbManager.getAccountStore().get(address).getBalance(),
        totalBalance - (expectCpuUsageTotal + expectCpuUsageTotal2) * 10);
  }

  public GVMTestResult deployEndlessLoopContract(long value, long feeLimit,
                                                 long consumeUserResourcePercent)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "EndlessLoopContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":true,\"inputs\":[],\"name\":\"getVote\",\"outputs\":[{\"name\":\"_vote\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_vote\",\"type\":\"uint256\"}],\"name\":\"setVote\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    String code = "608060405234801561001057600080fd5b506000808190555060fa806100266000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680630242f35114604e578063230796ae146076575b600080fd5b348015605957600080fd5b50606060a0565b6040518082815260200191505060405180910390f35b348015608157600080fd5b50609e6004803603810190808035906020019092919050505060a9565b005b60008054905090565b806000819055505b60011560cb576001600080828254019250508190555060b1565b505600a165627a7a72305820290a38c9bbafccaf6c7f752ab56d229e354da767efb72715ee9fdb653b9f4b6c0029";
    String libraryAddressPair = null;

    return GVMTestUtils
        .deployContractAndReturnGvmTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            dbManager, null);
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
