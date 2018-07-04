package org.gsc.core.chain;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.utils.TransactionUtil;
import org.gsc.config.Args;
import org.gsc.config.args.GenesisBlock;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.protos.Protocol.Transaction;

public class Genesis {
  public static BlockWrapper newGenesisBlock() {

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
    Sha256Hash parentHash = Sha256Hash.wrap(genesisBlockArg.getParentHash().getBytes());
    long number = Long.parseLong(genesisBlockArg.getNumber());

    BlockWrapper blockWrapper = new BlockWrapper(timestamp, parentHash, number,
        ByteString.copyFromUtf8("0x00"), transactionList);

    blockWrapper.setMerkleRoot();
    blockWrapper.generatedByMyself = true;

    return blockWrapper;
  }
}
