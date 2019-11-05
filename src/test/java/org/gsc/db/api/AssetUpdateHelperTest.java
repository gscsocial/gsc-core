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

package org.gsc.db.api;

import com.google.protobuf.ByteString;
import java.io.File;

import org.gsc.core.wrapper.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.testng.annotations.Test;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Exchange;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;

public class AssetUpdateHelperTest {

  private static Manager dbManager;
  private static GSCApplicationContext context;
  private static String dbPath = "db_AssetUpdateHelperTest_test";
  private static Application AppT;

  private static ByteString assetName = ByteString.copyFrom("assetIssueName".getBytes());

  static {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, "config-test-index.conf");
    Args.getInstance().setConfirmedNode(true);
    context = new GSCApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
  }

  @BeforeClass
  public static void init() {

    dbManager = context.getBean(Manager.class);

    AssetIssueContract contract =
        AssetIssueContract.newBuilder().setName(assetName).setNum(12581).setPrecision(5).build();
    AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(contract);
    dbManager.getAssetIssueStore().put(assetIssueWrapper.createDbKey(), assetIssueWrapper);

    BlockWrapper blockWrapper = new BlockWrapper(1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0000000000000002498b464ac0292229938a342238077182498b464ac0292222"))),
        1234,ByteString.EMPTY, ByteString.copyFrom("1234567".getBytes()));

    blockWrapper.addTransaction(new TransactionWrapper(contract, ContractType.AssetIssueContract));
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(1L);
    dbManager.getBlockIndexStore().put(blockWrapper.getBlockId());
    dbManager.getBlockStore().put(blockWrapper.getBlockId().getBytes(), blockWrapper);

    ExchangeWrapper exchangeWrapper =
        new ExchangeWrapper(
            Exchange.newBuilder()
                .setExchangeId(1L)
                .setFirstTokenId(assetName)
                .setSecondTokenId(ByteString.copyFrom("_".getBytes()))
                .build());
    dbManager.getExchangeStore().put(exchangeWrapper.createDbKey(), exchangeWrapper);

    AccountWrapper accountWrapper =
        new AccountWrapper(
            Account.newBuilder()
                .setAssetIssuedName(assetName)
                .putAsset("assetIssueName", 100)
                .putFreeAssetNetUsage("assetIssueName", 20000)
                .putLatestAssetOperationTime("assetIssueName", 30000000)
                .setAddress(ByteString.copyFrom(ByteArray.fromHexString("121212abc")))
                .build());
    dbManager.getAccountStore().put(ByteArray.fromHexString("121212abc"), accountWrapper);
  }

  @Test
  public void test() {

    if (dbManager == null) {
      init();
    }
    AssetUpdateHelper assetUpdateHelper = new AssetUpdateHelper(dbManager);
    assetUpdateHelper.init();
    {
      assetUpdateHelper.updateAsset();

      String idNum = "1000001";

      AssetIssueWrapper assetIssueWrapper =
          dbManager.getAssetIssueStore().get(assetName.toByteArray());
      Assert.assertEquals(idNum, assetIssueWrapper.getId());
      Assert.assertEquals(5L, assetIssueWrapper.getPrecision());

      AssetIssueWrapper assetIssueWrapper2 =
          dbManager.getAssetIssueV2Store().get(ByteArray.fromString(String.valueOf(idNum)));

      Assert.assertEquals(idNum, assetIssueWrapper2.getId());
      Assert.assertEquals(assetName, assetIssueWrapper2.getName());
      Assert.assertEquals(0L, assetIssueWrapper2.getPrecision());
    }

    {
      assetUpdateHelper.updateExchange();

      try {
        ExchangeWrapper exchangeWrapper =
            dbManager.getExchangeV2Store().get(ByteArray.fromLong(1L));
        Assert.assertEquals("1000001", ByteArray.toStr(exchangeWrapper.getFirstTokenId()));
        Assert.assertEquals("_", ByteArray.toStr(exchangeWrapper.getSecondTokenId()));
      } catch (Exception ex) {
        throw new RuntimeException("testUpdateExchange error");
      }
    }

    {
      assetUpdateHelper.updateAccount();

      AccountWrapper accountWrapper =
          dbManager.getAccountStore().get(ByteArray.fromHexString("121212abc"));

      Assert.assertEquals(
          ByteString.copyFrom(ByteArray.fromString("1000001")), accountWrapper.getAssetIssuedID());

      Assert.assertEquals(1, accountWrapper.getAssetMapV2().size());

      Assert.assertEquals(100L, accountWrapper.getAssetMapV2().get("1000001").longValue());

      Assert.assertEquals(1, accountWrapper.getAllFreeAssetNetUsageV2().size());

      Assert.assertEquals(
          20000L, accountWrapper.getAllFreeAssetNetUsageV2().get("1000001").longValue());

      Assert.assertEquals(1, accountWrapper.getLatestAssetOperationTimeMapV2().size());

      Assert.assertEquals(
          30000000L, accountWrapper.getLatestAssetOperationTimeMapV2().get("1000001").longValue());
    }

    removeDb();
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}
