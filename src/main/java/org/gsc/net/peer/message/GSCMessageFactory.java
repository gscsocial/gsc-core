/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.net.peer.message;

import org.apache.commons.lang3.ArrayUtils;
import org.gsc.net.peer.p2p.MessageFactory;
import org.gsc.core.exception.P2pException;

/**
 * msg factory.
 */
public class GSCMessageFactory extends MessageFactory {

    @Override
    public GSCMessage create(byte[] data) throws Exception {
        try {
            byte type = data[0];
            byte[] rawData = ArrayUtils.subarray(data, 1, data.length);
            return create(type, rawData);
        } catch (final P2pException e) {
            throw e;
        } catch (final Exception e) {
            throw new P2pException(P2pException.TypeEnum.PARSE_MESSAGE_FAILED,
                    "type=" + data[0] + ", len=" + data.length + ", error msg: " + e.getMessage());
        }
    }

    private GSCMessage create(byte type, byte[] packed) throws Exception {
        MessageTypes receivedTypes = MessageTypes.fromByte(type);
        if (receivedTypes == null) {
            throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
                    "type=" + type + ", len=" + packed.length);
        }
        switch (receivedTypes) {
            case TRX:
                return new TransactionMessage(packed);
            case BLOCK:
                return new BlockMessage(packed);
            case TRXS:
                return new TransactionsMessage(packed);
            case BLOCKS:
                return new BlocksMessage(packed);
            case INVENTORY:
                return new InventoryMessage(packed);
            case FETCH_INV_DATA:
                return new FetchInvDataMessage(packed);
            case SYNC_BLOCK_CHAIN:
                return new SyncBlockChainMessage(packed);
            case BLOCK_CHAIN_INVENTORY:
                return new ChainInventoryMessage(packed);
            case ITEM_NOT_FOUND:
                return new ItemNotFound();
            case FETCH_BLOCK_HEADERS:
                return new FetchBlockHeadersMessage(packed);
            case TRX_INVENTORY:
                return new TransactionInventoryMessage(packed);
            default:
                throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE,
                        receivedTypes.toString() + ", len=" + packed.length);
        }
    }
}
