package org.gsc.net.message;

import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.core.exception.BadItemException;

public class BlockMessage extends GSCMessage {

  private BlockWrapper block;

  public BlockMessage(byte[] data) throws BadItemException {
    this.type = MessageTypes.BLOCK.asByte();
    this.data = data;
    this.block = new BlockWrapper(data);
  }

  public BlockMessage(BlockWrapper block) {
    data = block.getData();
    this.type = MessageTypes.BLOCK.asByte();
    this.block = block;
  }

  public BlockWrapper getBlockCapsule() {
    return block;
  }
  
  public BlockId getBlockId() {
    return getBlockCapsule().getBlockId();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public Sha256Hash getMessageId() {
    return getBlockCapsule().getBlockId();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append(block.getBlockId().getString())
        .append(", trx size: ").append(block.getTransactions().size()).append("\n").toString();
  }
}
