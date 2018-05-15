package org.gsc.core.chain;

import com.google.protobuf.ByteString;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.BlockHeader;

public class BlockHeaderWrapper {

  private Protocol.BlockHeader blockHeader;

  public Sha256Hash getParentHash() {
    return Sha256Hash.ZERO_HASH;
  }

  public long getNum() {
    return 0;
  }

  public BlockHeaderWrapper(BlockHeader blockHeader) {
    this.blockHeader = blockHeader;
  }


  public Sha256Hash getMerkleRoot() {
    return Sha256Hash.wrap(this.blockHeader.getRawData().getTxTrieRoot());

  }

  public ByteString getProducerAddress() {
    return this.blockHeader.getRawData().getProducerAddress();
  }

}
