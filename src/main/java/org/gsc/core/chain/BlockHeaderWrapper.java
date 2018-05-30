package org.gsc.core.chain;

import com.google.protobuf.ByteString;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.wrapper.StoreWrapper;
import org.gsc.protos.Protocol.BlockHeader;

public class BlockHeaderWrapper extends org.gsc.core.chain.BlockHeader implements StoreWrapper<BlockHeader> {

  public BlockHeaderWrapper(long number, Sha256Hash hash, long when, ByteString producerAddress) {
    super(number, hash, when, producerAddress);
  }

  public BlockHeaderWrapper(org.gsc.protos.Protocol.BlockHeader blockHeader) {
     super(blockHeader);
  }

  @Override
  public byte[] getData() {
    return this.blockHeader.toByteArray();
  }

  @Override
  public BlockHeader getInstance() {
    return this.blockHeader;
  }

}
