package org.gsc.core.chain;

import com.google.protobuf.ByteString;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.protos.Protocol;

public class BlockHeader {

  protected Protocol.BlockHeader blockHeader;

  public BlockHeader(long number, Sha256Hash hash, long when, ByteString producerAddress) {
    // blockheader raw
    Protocol.BlockHeader.raw.Builder blockHeaderRawBuild = Protocol.BlockHeader.raw.newBuilder();
    Protocol.BlockHeader.raw blockHeaderRaw = blockHeaderRawBuild
        .setNumber(number)
        .setParentHash(hash.getByteString())
        .setTimestamp(when)
        .setProducerAddress(producerAddress).build();

    // block header
    Protocol.BlockHeader.Builder blockHeaderBuild = Protocol.BlockHeader.newBuilder();
    blockHeader = blockHeaderBuild.setRawData(blockHeaderRaw).build();
  }

  public BlockHeader() {

  }

  public BlockHeader(Protocol.BlockHeader blockHeader) {
    this.blockHeader = blockHeader;
  }

  protected Sha256Hash getRawHash() {
    return Sha256Hash.of(this.blockHeader.getRawData().toByteArray());
  }

  public BlockId getBlockId() {
    return new BlockId(Sha256Hash.of(this.blockHeader.toByteArray()), getNum());
  }

  public BlockId getParentBlockId() {
    return new BlockId(getParentHash(), getNum() - 1);
  }

  public ByteString getParentHashStr() {
    return this.blockHeader.getRawData().getParentHash();
  }

  public long getNum() {
    return this.blockHeader.getRawData().getNumber();
  }

  public long getTimeStamp() {
    return this.blockHeader.getRawData().getTimestamp();
  }

  public Sha256Hash getParentHash() {
    return Sha256Hash.wrap(this.blockHeader.getRawData().getParentHash());
  }

  public Sha256Hash getMerkleRoot() {
    return Sha256Hash.wrap(this.blockHeader.getRawData().getTxTrieRoot());
  }

  public ByteString getProducerAddress() {
    return this.blockHeader.getRawData().getProducerAddress();
  }

}
