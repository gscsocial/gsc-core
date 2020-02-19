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

package org.gsc.core.operator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.TransactionResultWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Transaction.Result.code;

@Slf4j
public class WitnessUpdateOperatorTest {

  private static GSCApplicationContext context;
  private static Manager dbManager;
  private static final String dbPath = "db_WitnessUpdate_test";
  private static final String OWNER_ADDRESS;
  private static final String OWNER_ADDRESS_ACCOUNT_NAME = "test_account";
  private static final String OWNER_ADDRESS_NOT_WITNESS;
  private static final String OWNER_ADDRESS_NOT_WITNESS_ACCOUNT_NAME = "test_account1";
  private static final String OWNER_ADDRESS_NOTEXIST;
  private static final String URL = "https://gsc.network";
  private static final String NewURL = "https://gsc.org";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";

  static {
    Args.setParam(new String[]{"--db-directory", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "6f24fc8a9e3712e9de397643ee2db721c7242919";
    OWNER_ADDRESS_NOTEXIST =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_NOT_WITNESS =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d427122222";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  /**
   * create temp Wrapper test need.
   */
  @Before
  public void createWrapper() {
    // address in accountStore and witnessStore
    AccountWrapper accountWrapper =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS), accountWrapper);
    WitnessWrapper ownerWrapper = new WitnessWrapper(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 10_000_000L, URL);
    dbManager.getWitnessStore().put(ByteArray.fromHexString(OWNER_ADDRESS), ownerWrapper);

    // address exist in accountStore, but is not witness
    AccountWrapper accountNotWitnessWrapper =
        new AccountWrapper(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_NOT_WITNESS)),
            ByteString.copyFromUtf8(OWNER_ADDRESS_NOT_WITNESS_ACCOUNT_NAME),
            Protocol.AccountType.Normal);
    dbManager.getAccountStore()
        .put(ByteArray.fromHexString(OWNER_ADDRESS_NOT_WITNESS), accountNotWitnessWrapper);
    dbManager.getWitnessStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_NOT_WITNESS));

    // address does not exist in accountStore
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_NOTEXIST));
  }

  private Any getContract(String address, String url) {
    return Any.pack(
        Contract.WitnessUpdateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setUpdateUrl(ByteString.copyFrom(ByteArray.fromString(url)))
            .build());
  }

  private Any getContract(String address, ByteString url) {
    return Any.pack(
        Contract.WitnessUpdateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setUpdateUrl(url)
            .build());
  }

  /**
   * Update witness,result is success.
   */
  @Test
  public void rightUpdateWitness() {
    WitnessUpdateOperator operator = new WitnessUpdateOperator(getContract(OWNER_ADDRESS, NewURL),
        dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      WitnessWrapper witnessWrapper = dbManager.getWitnessStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(witnessWrapper);
      Assert.assertEquals(witnessWrapper.getUrl(), NewURL);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Invalid Address update witness,result is failed,exception is "Invalid address".
   */
  @Test
  public void InvalidAddress() {
    WitnessUpdateOperator operator = new WitnessUpdateOperator(
        getContract(OWNER_ADDRESS_INVALID, NewURL), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("Invalid address");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid address", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Invalid url createWitness,result is failed,exception is "Invalid url".
   */
  @Test
  public void InvalidUrlTest() {
    TransactionResultWrapper ret = new TransactionResultWrapper();
    //Url cannot empty
    try {
      WitnessUpdateOperator operator = new WitnessUpdateOperator(
          getContract(OWNER_ADDRESS, ByteString.EMPTY), dbManager);
      operator.validate();
      operator.execute(ret);
      fail("Invalid url");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    //256 bytes
    String url256Bytes = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    //Url length can not greater than 256
    try {
      WitnessUpdateOperator operator = new WitnessUpdateOperator(
          getContract(OWNER_ADDRESS, ByteString.copyFromUtf8(url256Bytes + "0")), dbManager);
      operator.validate();
      operator.execute(ret);
      fail("Invalid url");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Invalid url", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // 1 byte url is ok.
    try {
      WitnessUpdateOperator operator = new WitnessUpdateOperator(getContract(OWNER_ADDRESS, "0"),
          dbManager);
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      WitnessWrapper witnessWrapper = dbManager.getWitnessStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(witnessWrapper);
      Assert.assertEquals(witnessWrapper.getUrl(), "0");
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // 256 bytes url is ok.
    try {
      WitnessUpdateOperator operator = new WitnessUpdateOperator(
          getContract(OWNER_ADDRESS, url256Bytes), dbManager);
      operator.validate();
      operator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      WitnessWrapper witnessWrapper = dbManager.getWitnessStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertNotNull(witnessWrapper);
      Assert.assertEquals(witnessWrapper.getUrl(), url256Bytes);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use AccountStore not exists Address createWitness,result is failed,exception is "Witness does
   * not exist"
   */
  @Test
  public void notExistWitness() {
    WitnessUpdateOperator operator = new WitnessUpdateOperator(
        getContract(OWNER_ADDRESS_NOT_WITNESS, URL), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("witness [+OWNER_ADDRESS_NOACCOUNT+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Witness does not exist", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * if account does not exist in accountStore, the test will throw a Exception
   */
  @Test
  public void notExistAccount() {
    WitnessUpdateOperator operator = new WitnessUpdateOperator(
        getContract(OWNER_ADDRESS_NOTEXIST, URL), dbManager);
    TransactionResultWrapper ret = new TransactionResultWrapper();
    try {
      operator.validate();
      operator.execute(ret);
      fail("account does not exist");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("account does not exist", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @AfterClass
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