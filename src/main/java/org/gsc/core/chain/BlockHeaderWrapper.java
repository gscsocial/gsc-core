package org.gsc.core.chain;

import com.google.protobuf.ByteString;
import org.gsc.common.utils.Sha256Hash;

public class BlockHeaderWrapper {

  public Sha256Hash getParentHash() {
    return Sha256Hash.ZERO_HASH;
  }

  public long getNum() {
    return 0;
  }


  public Sha256Hash getMerkleRoot() {
    //  return Sha256Hash.wrap(this.block.getBlockHeader().getRawData().getTxTrieRoot());
    return Sha256Hash.ZERO_HASH;
  }

  public ByteString getWitnessAddress() {
    //  return this.block.getBlockHeader().getRawData().getWitnessAddress();
    return null;
  }

}
