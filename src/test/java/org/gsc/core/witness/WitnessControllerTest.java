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

package org.gsc.core.witness;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.gsc.application.GSCApplicationContext;
import org.gsc.utils.ByteArray;
import org.gsc.utils.FileUtil;
import org.gsc.core.Constant;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.config.args.Witness;
import org.gsc.db.Manager;

public class WitnessControllerTest {

  private static Manager dbManager = new Manager();
  private static GSCApplicationContext context;
  private static String dbPath = "db_witness_controller_test";

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_NET_CONF);
    context = new GSCApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  ByteString blank = ByteString.copyFrom(new byte[1]);

  @Test
  public void testSlot() {

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(19000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(1);

  }

  //  @Test
  public void testWitnessSchedule() {

    // no witness produce block
    assertEquals(0, dbManager.getHeadBlockNum());

    // test witnesses in genesis block
    assertEquals(
        "a0904fe896536f4bebc64c95326b5054a2c3d27df6", // first(current witness)
        ByteArray.toHexString(
            (dbManager.getWitnessController().getScheduledWitness(0).toByteArray())));
    assertEquals(
        "a0904fe896536f4bebc64c95326b5054a2c3d27df6",
        ByteArray.toHexString(
            (dbManager.getWitnessController().getScheduledWitness(5).toByteArray())));
    assertEquals(
        "a0807337f180b62a77576377c1d0c9c24df5c0dd62", // second(next witness)
        ByteArray.toHexString(
            (dbManager.getWitnessController().getScheduledWitness(6).toByteArray())));
    assertEquals(
        "a0807337f180b62a77576377c1d0c9c24df5c0dd62",
        ByteArray.toHexString(
            (dbManager.getWitnessController().getScheduledWitness(11).toByteArray())));
    assertEquals(
        "a05430a3f089154e9e182ddd6fe136a62321af22a7", // third
        ByteArray.toHexString(
            (dbManager.getWitnessController().getScheduledWitness(12).toByteArray())));

    // test maintenance
    ByteString a =
        ByteString.copyFrom(ByteArray.fromHexString("a0ec6525979a351a54fa09fea64beb4cce33ffbb7a"));
    ByteString b =
        ByteString.copyFrom(ByteArray.fromHexString("a0fab5fbf6afb681e4e37e9d33bddb7e923d6132e5"));
    // system.out.print("a address:" + ByteArray.toHexString(a.toByteArray()) + "\n");
    // System.out.print("b address:" + ByteArray.toHexString(b.toByteArray()));
    List<ByteString> w = new ArrayList<>();
    w.add(a);
    w.add(b);

    // update active witness
    dbManager.getWitnessScheduleStore().saveActiveWitnesses(w);
    // now 2 active witnesses
    assertEquals(2, dbManager.getWitnessScheduleStore().getActiveWitnesses().size());

    // update shuffled witness
    dbManager.getWitnessScheduleStore().saveCurrentShuffledWitnesses(w);

    assertEquals(a, dbManager.getWitnessController().getScheduledWitness(1));
    assertEquals(b, dbManager.getWitnessController().getScheduledWitness(2));
    assertEquals(a, dbManager.getWitnessController().getScheduledWitness(3));
    assertEquals(b, dbManager.getWitnessController().getScheduledWitness(4));
  }

  @Test
  public void testTryRemoveThePowerOfTheGr() {

    Witness witness = Args.getInstance().getGenesisBlock().getWitnesses().get(0);
    assertEquals(1024, witness.getVoteCount());

    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(-1);
    dbManager.getWitnessController().tryRemoveThePowerOfTheGr();
    assertEquals(1024, dbManager.getWitnessStore().get(witness.getAddress()).getVoteCount());

    dbManager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(1);
    dbManager.getWitnessController().tryRemoveThePowerOfTheGr();
    assertEquals(0, dbManager.getWitnessStore().get(witness.getAddress()).getVoteCount());

    dbManager.getWitnessController().tryRemoveThePowerOfTheGr();
    assertEquals(0, dbManager.getWitnessStore().get(witness.getAddress()).getVoteCount());


  }


}
