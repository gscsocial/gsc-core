package org.gsc.core.chain;

import com.google.protobuf.ByteString;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.BlockHeader;

public class BlockHeaderWrapper {

  protected Protocol.BlockHeader blockHeader;

  public Sha256Hash getParentHash() {
    return Sha256Hash.ZERO_HASH;
  }

  public long getNum() {
    return 0;
  }

  public BlockHeaderWrapper(long number, Sha256Hash hash, long when, ByteString producerAddress) {
    // blockheader raw
    BlockHeader.raw.Builder blockHeaderRawBuild = BlockHeader.raw.newBuilder();
    BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
        .setNumber(number)
        .setParentHash(hash.getByteString())
        .setTimestamp(when)
        .setProducerAddress(producerAddress).build();

    // block header
    BlockHeader.Builder blockHeaderBuild = BlockHeader.newBuilder();
    blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();
  }

  public BlockHeaderWrapper(BlockHeader blockHeader) {
    this.blockHeader = blockHeader;
  }

  private Sha256Hash getRawHash() {
    return Sha256Hash.of(this.blockHeader.getRawData().toByteArray());
  }

  public BlockId getBlockId() {
     return new BlockId(Sha256Hash.of(this.blockHeader.toByteArray()), getNum())
  }


  public BlockHeaderWrapper() {}

  public Sha256Hash getMerkleRoot() {
    return Sha256Hash.wrap(this.blockHeader.getRawData().getTxTrieRoot());
  }

  public ByteString getProducerAddress() {
    return this.blockHeader.getRawData().getProducerAddress();
  }

}
