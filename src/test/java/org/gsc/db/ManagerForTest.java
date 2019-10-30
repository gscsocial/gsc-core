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

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Sha256Hash;
import org.gsc.utils.Utils;
import org.gsc.core.wrapper.WitnessWrapper;
import org.gsc.core.witness.WitnessController;
import org.gsc.protos.Protocol.Account;

public class ManagerForTest {

  private Manager dbManager;

  public ManagerForTest(Manager dbManager) {
    this.dbManager = dbManager;
  }

  private Map<ByteString, String> addTestWitnessAndAccount() {
    dbManager.getWitnesses().clear();
    return IntStream.range(0, 2)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessWrapper witnessWrapper = new WitnessWrapper(address);
              dbManager.getWitnessStore().put(address.toByteArray(), witnessWrapper);
              dbManager.getWitnessController().addWitness(address);

              AccountWrapper accountWrapper =
                  new AccountWrapper(Account.newBuilder().setAddress(address).build());
              dbManager.getAccountStore().put(address.toByteArray(), accountWrapper);

              return Maps.immutableEntry(address, privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private ByteString getWitnessAddress(long time) {
    WitnessController witnessController = dbManager.getWitnessController();
    return witnessController.getScheduledWitness(witnessController.getSlotAtTime(time));
  }

  public boolean pushNTestBlock(int count) {
    try {
      for (int i = 1; i <= count; i++) {
        ByteString hash = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
            .getByteString();
        long time = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 3000L;
        long number = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;
        BlockWrapper blockWrapper = createTestBlockWrapper(time, number, hash);
        dbManager.pushBlock(blockWrapper);
      }
    } catch (Exception ignore) {
      return false;
    }
    return true;
  }

    public BlockWrapper createTestBlockWrapper(long time,
                                               long number, ByteString hash) {

        Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();
        ByteString witnessAddress = getWitnessAddress(time);

        BlockWrapper blockWrapper = new BlockWrapper(number, Sha256Hash.wrap(hash), time,ByteString.EMPTY,
                witnessAddress);
        blockWrapper.generatedByMyself = true;
        blockWrapper.setMerkleRoot();
        blockWrapper.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
        return blockWrapper;
    }
}