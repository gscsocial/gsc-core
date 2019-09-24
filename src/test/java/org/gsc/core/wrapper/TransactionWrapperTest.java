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

package org.gsc.core.wrapper;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.gsc.application.Application;
import org.gsc.application.ApplicationFactory;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.FileUtil;
import org.gsc.utils.StringUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.protos.Protocol.AccountType;

@Slf4j
public class TransactionWrapperTest {

  private static Manager dbManager;
  private static GSCApplicationContext context;
  private static Application AppT;
  private static String dbPath = "db_transactionwrapper_test";
  private static String OWNER_ADDRESS;
  private static String OWNER_KEY = "bfa67cb3dc6609b3a0c98e717d66f38ed1a159b5b3421678dfab85961c40de2f";
  private static String TO_ADDRESS;
  private static String OWNER_ACCOUNT_NOT_Exist;
  private static String KEY_11 = "1111111111111111111111111111111111111111111111111111111111111111";
  private static String KEY_12 = "1212121212121212121212121212121212121212121212121212121212121212";
  private static String KEY_13 = "1313131313131313131313131313131313131313131313131313131313131313";
  private static String KEY_21 = "2121212121212121212121212121212121212121212121212121212121212121";
  private static String KEY_22 = "2222222222222222222222222222222222222222222222222222222222222222";
  private static String KEY_23 = "2323232323232323232323232323232323232323232323232323232323232323";
  private static String KEY_31 = "3131313131313131313131313131313131313131313131313131313131313131";
  private static String KEY_32 = "3232323232323232323232323232323232323232323232323232323232323232";
  private static String KEY_33 = "3333333333333333333333333333333333333333333333333333333333333333";

  private static String KEY_ADDRESS_11;
  private static String KEY_ADDRESS_12;
  private static String KEY_ADDRESS_13;
  private static String KEY_ADDRESS_21;
  private static String KEY_ADDRESS_22;
  private static String KEY_ADDRESS_23;
  private static String KEY_ADDRESS_31;
  private static String KEY_ADDRESS_32;
  private static String KEY_ADDRESS_33;

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath},
        Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    dbManager = context.getBean(Manager.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "03702350064AD5C1A8AA6B4D74B051199CFF8EA7";
    TO_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    OWNER_ACCOUNT_NOT_Exist =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
    KEY_ADDRESS_11 = Wallet.getAddressPreFixString() + "19E7E376E7C213B7E7E7E46CC70A5DD086DAFF2A";
    KEY_ADDRESS_12 = Wallet.getAddressPreFixString() + "1C5A77D9FA7EF466951B2F01F724BCA3A5820B63";
    KEY_ADDRESS_13 = Wallet.getAddressPreFixString() + "03A1BBA60B5AA37094CF16123ADD674C01589488";

    KEY_ADDRESS_21 = Wallet.getAddressPreFixString() + "2BD0C9FE079C8FCA0E3352EB3D02839C371E5C41";
    KEY_ADDRESS_22 = Wallet.getAddressPreFixString() + "1563915E194D8CFBA1943570603F7606A3115508";
    KEY_ADDRESS_23 = Wallet.getAddressPreFixString() + "D3E442496EB66A4748912EC4A3B7A111D0B855D6";

    KEY_ADDRESS_31 = Wallet.getAddressPreFixString() + "77952CE83CA3CAD9F7ADCFABEDA85BD2F1F52008";
    KEY_ADDRESS_32 = Wallet.getAddressPreFixString() + "94622CC2A5B64A58C25A129D48A2BEEC4B65B779";
    KEY_ADDRESS_33 = Wallet.getAddressPreFixString() + "5CBDD86A2FA8DC4BDDD8A8F69DBA48572EEC07FB";
  }

  /**
   * create temp Wrapper test need.
   */
  @Before
  public void createAccountWrapper() {
    AccountWrapper ownerWrapper =
        new AccountWrapper(
            ByteString.copyFromUtf8("owner"),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            10_000_000_000L);
    dbManager.getAccountStore().put(ownerWrapper.createDbKey(), ownerWrapper);
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