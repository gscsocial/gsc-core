package org.gsc.net.message.gsc;

import org.gsc.common.exception.BadItemException;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.chain.BlockId;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.net.message.MessageTypes;

public class BlockMessage extends GscMessage{
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

  public BlockId getBlockId() {
    return getBlockWrapper().getBlockId();
  }

  public BlockWrapper getBlockWrapper() {
    return block;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public Sha256Hash getMessageId() {
    return getBlockWrapper().getBlockId();
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
