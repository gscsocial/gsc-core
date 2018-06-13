package org.gsc.net.message.gsc;

import java.util.List;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.net.message.MessageTypes;
import org.gsc.protos.Protocol.Inventory;
import org.gsc.protos.Protocol.Inventory.InventoryType;

public class FetchMessage extends InventoryMessage {
  public FetchMessage(byte[] packed) throws Exception {
    super(packed);
    this.type = MessageTypes.FETCH.asByte();
  }

  public FetchMessage(Inventory inv) {
    super(inv);
    this.type = MessageTypes.FETCH.asByte();
  }

  public FetchMessage(List<Sha256Hash> hashList, InventoryType type) {
    super(hashList, type);
    this.type = MessageTypes.FETCH.asByte();
  }

}
