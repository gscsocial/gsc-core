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

package org.gsc.core;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.db.CpuProcessor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.config.DefaultConfig;
import org.gsc.config.Parameter.AdaptiveResourceLimitConstants;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol.AccountType;

@Slf4j
public class CpuProcessorTest {

  private static Manager dbManager;
  private static final String dbPath = "CpuProcessorTest";
  private static GSCApplicationContext context;
  private static final String ASSET_NAME;
  private static final String CONTRACT_PROVIDER_ADDRESS;
  private static final String USER_ADDRESS;

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    ASSET_NAME = "test_token";
    CONTRACT_PROVIDER_ADDRESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    USER_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
  }

  /**
   * Init data.
   */
//  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * create temp Wrapper test need.
   */
//  @Before
  public void createWrapper() {
    AccountWrapper contractProvierWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(CONTRACT_PROVIDER_ADDRESS)),
            AccountType.Normal,
            0L);
    contractProvierWrapper.addAsset(ASSET_NAME.getBytes(), 100L);

    AccountWrapper userWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("asset"),
            ByteString.copyFrom(ByteArray.fromHexString(USER_ADDRESS)),
            AccountType.AssetIssue,
            dbManager.getDynamicPropertiesStore().getAssetIssueFee());

    dbManager.getAccountStore().reset();
    dbManager.getAccountStore()
        .put(contractProvierWrapper.getAddress().toByteArray(), contractProvierWrapper);
    dbManager.getAccountStore().put(userWrapper.getAddress().toByteArray(), userWrapper);

  }


//  todo ,replaced by smartContract later
  private AssetIssueContract getAssetIssueContract() {
    return Contract.AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(USER_ADDRESS)))
        .setName(ByteString.copyFromUtf8(ASSET_NAME))
        .setFreeAssetNetLimit(1000L)
        .setPublicFreeAssetNetLimit(1000L)
        .build();
  }

//  @Test
  public void testUseContractCreatorCpu() throws Exception {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore().saveTotalCpuWeight(10_000_000L);

    AccountWrapper ownerWrapper = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(CONTRACT_PROVIDER_ADDRESS));
    dbManager.getAccountStore().put(ownerWrapper.getAddress().toByteArray(), ownerWrapper);

    CpuProcessor processor = new CpuProcessor(dbManager);
    long cpu = 10000;
    long now = 1526647838000L;

    boolean result = processor.useCpu(ownerWrapper, cpu, now);
    Assert.assertEquals(false, result);

    ownerWrapper.setFrozenForCpu(10_000_000L, 0L);
    result = processor.useCpu(ownerWrapper, cpu, now);
    Assert.assertEquals(true, result);

    AccountWrapper ownerWrapperNew = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(CONTRACT_PROVIDER_ADDRESS));

    Assert.assertEquals(1526647838000L, ownerWrapperNew.getLatestOperationTime());
    Assert.assertEquals(1526647838000L,
        ownerWrapperNew.getAccountResource().getLatestConsumeTimeForCpu());
    Assert.assertEquals(10000L, ownerWrapperNew.getAccountResource().getCpuUsage());

  }

//  @Test
  public void updateAdaptiveTotalCpuLimit() {
    CpuProcessor processor = new CpuProcessor(dbManager);

    // open
    dbManager.getDynamicPropertiesStore().saveAllowAdaptiveCpu(1);

    // Test resource usage auto reply
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    long now = dbManager.getWitnessController().getHeadSlot();
    dbManager.getDynamicPropertiesStore().saveTotalCpuAverageTime(now);
    dbManager.getDynamicPropertiesStore().saveTotalCpuAverageUsage(4000L);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(
        1526647838000L + AdaptiveResourceLimitConstants.PERIODS_MS / 2);
    processor.updateTotalCpuAverageUsage();
    Assert.assertEquals(2000L,
        dbManager.getDynamicPropertiesStore().getTotalCpuAverageUsage());

    // test saveTotalCpuLimit
    long ratio = ChainConstant.WINDOW_SIZE_MS / AdaptiveResourceLimitConstants.PERIODS_MS;
    dbManager.getDynamicPropertiesStore().saveTotalCpuLimit(10000L * ratio);
    Assert.assertEquals(1000L,
        dbManager.getDynamicPropertiesStore().getTotalCpuTargetLimit());

    //Test exceeds resource limit
    dbManager.getDynamicPropertiesStore().saveTotalCpuCurrentLimit(10000L * ratio);
    dbManager.getDynamicPropertiesStore().saveTotalCpuAverageUsage(3000L);
    processor.updateAdaptiveTotalCpuLimit();
    Assert.assertEquals(10000L * ratio,
        dbManager.getDynamicPropertiesStore().getTotalCpuCurrentLimit());

    //Test exceeds resource limit 2
    dbManager.getDynamicPropertiesStore().saveTotalCpuCurrentLimit(20000L * ratio);
    dbManager.getDynamicPropertiesStore().saveTotalCpuAverageUsage(3000L);
    processor.updateAdaptiveTotalCpuLimit();
    Assert.assertEquals(20000L * ratio * 99 / 100L,
        dbManager.getDynamicPropertiesStore().getTotalCpuCurrentLimit());

    //Test less than resource limit
    dbManager.getDynamicPropertiesStore().saveTotalCpuCurrentLimit(20000L * ratio);
    dbManager.getDynamicPropertiesStore().saveTotalCpuAverageUsage(500L);
    processor.updateAdaptiveTotalCpuLimit();
    Assert.assertEquals(20000L * ratio * 1000 / 999L,
        dbManager.getDynamicPropertiesStore().getTotalCpuCurrentLimit());
  }
/**
   * remove resources.
   */
//  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

}
