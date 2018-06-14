package org.gsc.net.message.gsc;

import java.util.List;
import org.gsc.core.chain.BlockId;
import org.gsc.net.message.MessageTypes;

public class SyncMessage extends BlockInventoryMessage {

  public SyncMessage(byte[] packed) throws Exception {
    super(packed);
    this.type = MessageTypes.SYNC.asByte();
  }

  public SyncMessage(List<BlockId> blockIds) {
    super(blockIds);
    this.type = MessageTypes.SYNC.asByte();
  }

  @Override
  public String toString() {
    List<BlockId> blockIdList = getBlockIds();
    StringBuilder sb = new StringBuilder();
    int size = blockIdList.size();
    sb.append(super.toString()).append("size: ").append(size);
    if (size >= 1) {
      sb.append(", start block: " + blockIdList.get(0).getString());
      if (size > 1) {
        sb.append(", end block " + blockIdList.get(blockIdList.size() - 1).getString());
      }
    }
    return sb.toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return AttentionMessage.class;
  }
}
