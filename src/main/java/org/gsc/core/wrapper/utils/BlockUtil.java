/*
 * java-gsc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-gsc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gsc.core.wrapper.utils;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.config.args.Args;
import org.gsc.config.args.GenesisBlock;
import org.gsc.protos.Protocol.Transaction;

public class BlockUtil {

  /**
   * create genesis block from transactions.
   */
  public static BlockWrapper newGenesisBlockCapsule() {

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

    BlockWrapper blockWrapper = new BlockWrapper(timestamp, parentHash, number, transactionList);

    blockWrapper.setMerkleRoot();
    blockWrapper.setWitness("A new system must allow existing systems to be linked together without requiring any central control or coordination");
    blockWrapper.generatedByMyself = true;

    return blockWrapper;
  }

  /**
   * Whether the hash of the judge block is equal to the hash of the parent block.
   */
  public static boolean isParentOf(BlockWrapper blockWrapper1, BlockWrapper blockWrapper2) {
    return blockWrapper1.getBlockId().equals(blockWrapper2.getParentHash());
  }
}
