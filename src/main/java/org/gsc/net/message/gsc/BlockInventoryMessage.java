package org.gsc.net.message.gsc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.gsc.core.chain.BlockId;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.BlockInventory;

public abstract class BlockInventoryMessage extends GscMessage {

  protected BlockInventory blockInventory;

  public BlockInventoryMessage(byte[] data) throws Exception {
    this.data = data;
    this.blockInventory = Protocol.BlockInventory.parseFrom(data);
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  private BlockInventory getBlockInventory() {
    return blockInventory;
  }

  public BlockInventoryMessage(List<BlockId> blockIds) {
    BlockInventory.Builder invBuilder = BlockInventory.newBuilder();
    blockIds.forEach(blockId -> {
      Protocol.BlockId.Builder b = Protocol.BlockId.newBuilder();
      b.setHash(blockId.getByteString());
      b.setNumber(blockId.getNum());
      invBuilder.addIds(b);
    });

    blockInventory = invBuilder.build();
    this.data = blockInventory.toByteArray();
  }

  public List<BlockId> getBlockIds() {
    return getBlockInventory().getIdsList().stream()
        .map(blockId -> new BlockId(blockId.getHash(), blockId.getNumber()))
        .collect(Collectors.toCollection(ArrayList::new));
  }

}
