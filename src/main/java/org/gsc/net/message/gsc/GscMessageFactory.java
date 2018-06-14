package org.gsc.net.message.gsc;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.exception.P2pException;
import org.gsc.net.message.MessageFactory;
import org.gsc.net.message.MessageTypes;

/**
 * msg factory.
 */
public class GscMessageFactory implements MessageFactory {

  @Override
  public GscMessage create(byte[] data) throws Exception {
    try {
      byte type = data[0];
      byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
      return create(type, rawData);
    } catch (Exception e) {
      if (e instanceof P2pException) {
        throw e;
      } else {
        throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED,
            "type=" + data[0] + ", len=" + data.length);
      }
    }
  }

  private GscMessage create(byte type, byte[] packed) throws Exception {
    MessageTypes receivedTypes = MessageTypes.fromByte(type);
    if (receivedTypes == null) {
      throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
          "type=" + type + ", len=" + packed.length);
    }
    switch (receivedTypes) {
      case TRANSACTION:
        return new TransactionMessage(packed);
      case BLOCK:
        return new BlockMessage(packed);
      case INVENTORY:
        return new InventoryMessage(packed);
      case FETCH:
        return new FetchMessage(packed);
      case SYNC:
        return new SyncMessage(packed);
//      case BLOCK_CHAIN_INVENTORY:
//        return new ChainInventoryMessage(packed);
//      case ITEM_NOT_FOUND:
//        return new ItemNotFound();
//      case FETCH_BLOCK_HEADERS:
//        return new FetchBlockHeadersMessage(packed);
//      case TRX_INVENTORY:
//        return new TransactionInventoryMessage(packed);
      default:
        throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
            receivedTypes.toString() + ", len=" + packed.length);
    }
  }
}
