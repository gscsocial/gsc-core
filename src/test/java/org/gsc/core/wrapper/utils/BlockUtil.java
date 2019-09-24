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

package org.gsc.core.wrapper.utils;


import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;

import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.utils.ByteArray;
import org.gsc.config.args.Args;
import org.gsc.config.args.GenesisBlock;
import org.gsc.protos.Protocol.Transaction;

public class BlockUtil {

  /**
   * create genesis block from transactions.
   */
  public static BlockWrapper newGenesisBlockWrapper() {

    Args args = Args.getInstance();
    GenesisBlock genesisBlockArg = args.getGenesisBlock();
    List<Transaction> transactionList =
        genesisBlockArg.getAssets().stream()
            .map(key -> {
              byte[] address = key.getAddress();
              long balance = key.getBalance();
              return TransactionUtil.newGenesisTransaction(address, balance);
            })
            .collect(Collectors.toList());

    long timestamp = Long.parseLong(genesisBlockArg.getTimestamp());
    ByteString parentHash =
        ByteString.copyFrom(ByteArray.fromHexString(genesisBlockArg.getParentHash()));
    long number = Long.parseLong(genesisBlockArg.getNumber());

    BlockWrapper blockWrapper = new BlockWrapper(timestamp, parentHash, number, 1, ByteString.EMPTY, transactionList);

    blockWrapper.setMerkleRoot();
    blockWrapper.setWitness("The genesis witness to the revolution.");
    blockWrapper.generatedByMyself = true;

    return blockWrapper;
  }

  public static boolean isParentOf(BlockWrapper blockWrapper1, BlockWrapper blockWrapper2) {
    return blockWrapper1.getBlockId().equals(blockWrapper2.getParentHash());
  }

}