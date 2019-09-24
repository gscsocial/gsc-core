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

package org.gsc.net.node.statistics;

import lombok.extern.slf4j.Slf4j;
import org.gsc.net.discover.message.UdpMessageTypeEnum;
import org.gsc.net.peer.p2p.Message;
import org.gsc.net.peer.message.FetchInvDataMessage;
import org.gsc.net.peer.message.InventoryMessage;
import org.gsc.net.peer.message.MessageTypes;
import org.gsc.net.peer.message.TransactionsMessage;

@Slf4j
public class MessageStatistics {

    //udp discovery
    public final MessageCount discoverInPing = new MessageCount();
    public final MessageCount discoverOutPing = new MessageCount();
    public final MessageCount discoverInPong = new MessageCount();
    public final MessageCount discoverOutPong = new MessageCount();
    public final MessageCount discoverInFindNode = new MessageCount();
    public final MessageCount discoverOutFindNode = new MessageCount();
    public final MessageCount discoverInNeighbours = new MessageCount();
    public final MessageCount discoverOutNeighbours = new MessageCount();

    //tcp p2p
    public final MessageCount p2pInHello = new MessageCount();
    public final MessageCount p2pOutHello = new MessageCount();
    public final MessageCount p2pInPing = new MessageCount();
    public final MessageCount p2pOutPing = new MessageCount();
    public final MessageCount p2pInPong = new MessageCount();
    public final MessageCount p2pOutPong = new MessageCount();
    public final MessageCount p2pInDisconnect = new MessageCount();
    public final MessageCount p2pOutDisconnect = new MessageCount();

    //tcp gsc
    public final MessageCount gscInMessage = new MessageCount();
    public final MessageCount gscOutMessage = new MessageCount();

    public final MessageCount gscInSyncBlockChain = new MessageCount();
    public final MessageCount gscOutSyncBlockChain = new MessageCount();
    public final MessageCount gscInBlockChainInventory = new MessageCount();
    public final MessageCount gscOutBlockChainInventory = new MessageCount();

    public final MessageCount gscInTrxInventory = new MessageCount();
    public final MessageCount gscOutTrxInventory = new MessageCount();
    public final MessageCount gscInTrxInventoryElement = new MessageCount();
    public final MessageCount gscOutTrxInventoryElement = new MessageCount();

    public final MessageCount gscInBlockInventory = new MessageCount();
    public final MessageCount gscOutBlockInventory = new MessageCount();
    public final MessageCount gscInBlockInventoryElement = new MessageCount();
    public final MessageCount gscOutBlockInventoryElement = new MessageCount();

    public final MessageCount gscInTrxFetchInvData = new MessageCount();
    public final MessageCount gscOutTrxFetchInvData = new MessageCount();
    public final MessageCount gscInTrxFetchInvDataElement = new MessageCount();
    public final MessageCount gscOutTrxFetchInvDataElement = new MessageCount();

    public final MessageCount gscInBlockFetchInvData = new MessageCount();
    public final MessageCount gscOutBlockFetchInvData = new MessageCount();
    public final MessageCount gscInBlockFetchInvDataElement = new MessageCount();
    public final MessageCount gscOutBlockFetchInvDataElement = new MessageCount();


    public final MessageCount gscInTrx = new MessageCount();
    public final MessageCount gscOutTrx = new MessageCount();
    public final MessageCount gscInTrxs = new MessageCount();
    public final MessageCount gscOutTrxs = new MessageCount();
    public final MessageCount gscInBlock = new MessageCount();
    public final MessageCount gscOutBlock = new MessageCount();
    public final MessageCount gscOutAdvBlock = new MessageCount();

    public void addUdpInMessage(UdpMessageTypeEnum type) {
        addUdpMessage(type, true);
    }

    public void addUdpOutMessage(UdpMessageTypeEnum type) {
        addUdpMessage(type, false);
    }

    public void addTcpInMessage(Message msg) {
        addTcpMessage(msg, true);
    }

    public void addTcpOutMessage(Message msg) {
        addTcpMessage(msg, false);
    }

    private void addUdpMessage(UdpMessageTypeEnum type, boolean flag) {
        switch (type) {
            case DISCOVER_PING:
                if (flag) {
                    discoverInPing.add();
                } else {
                    discoverOutPing.add();
                }
                break;
            case DISCOVER_PONG:
                if (flag) {
                    discoverInPong.add();
                } else {
                    discoverOutPong.add();
                }
                break;
            case DISCOVER_FIND_NODE:
                if (flag) {
                    discoverInFindNode.add();
                } else {
                    discoverOutFindNode.add();
                }
                break;
            case DISCOVER_NEIGHBORS:
                if (flag) {
                    discoverInNeighbours.add();
                } else {
                    discoverOutNeighbours.add();
                }
                break;
            default:
                break;
        }
    }

    private void addTcpMessage(Message msg, boolean flag) {

        if (flag) {
            gscInMessage.add();
        } else {
            gscOutMessage.add();
        }

        switch (msg.getType()) {
            case P2P_HELLO:
                if (flag) {
                    p2pInHello.add();
                } else {
                    p2pOutHello.add();
                }
                break;
            case P2P_PING:
                if (flag) {
                    p2pInPing.add();
                } else {
                    p2pOutPing.add();
                }
                break;
            case P2P_PONG:
                if (flag) {
                    p2pInPong.add();
                } else {
                    p2pOutPong.add();
                }
                break;
            case P2P_DISCONNECT:
                if (flag) {
                    p2pInDisconnect.add();
                } else {
                    p2pOutDisconnect.add();
                }
                break;
            case SYNC_BLOCK_CHAIN:
                if (flag) {
                    gscInSyncBlockChain.add();
                } else {
                    gscOutSyncBlockChain.add();
                }
                break;
            case BLOCK_CHAIN_INVENTORY:
                if (flag) {
                    gscInBlockChainInventory.add();
                } else {
                    gscOutBlockChainInventory.add();
                }
                break;
            case INVENTORY:
                InventoryMessage inventoryMessage = (InventoryMessage) msg;
                int inventorySize = inventoryMessage.getInventory().getIdsCount();
                if (flag) {
                    if (inventoryMessage.getInvMessageType() == MessageTypes.TRX) {
                        gscInTrxInventory.add();
                        gscInTrxInventoryElement.add(inventorySize);
                    } else {
                        gscInBlockInventory.add();
                        gscInBlockInventoryElement.add(inventorySize);
                    }
                } else {
                    if (inventoryMessage.getInvMessageType() == MessageTypes.TRX) {
                        gscOutTrxInventory.add();
                        gscOutTrxInventoryElement.add(inventorySize);
                    } else {
                        gscOutBlockInventory.add();
                        gscOutBlockInventoryElement.add(inventorySize);
                    }
                }
                break;
            case FETCH_INV_DATA:
                FetchInvDataMessage fetchInvDataMessage = (FetchInvDataMessage) msg;
                int fetchSize = fetchInvDataMessage.getInventory().getIdsCount();
                if (flag) {
                    if (fetchInvDataMessage.getInvMessageType() == MessageTypes.TRX) {
                        gscInTrxFetchInvData.add();
                        gscInTrxFetchInvDataElement.add(fetchSize);
                    } else {
                        gscInBlockFetchInvData.add();
                        gscInBlockFetchInvDataElement.add(fetchSize);
                    }
                } else {
                    if (fetchInvDataMessage.getInvMessageType() == MessageTypes.TRX) {
                        gscOutTrxFetchInvData.add();
                        gscOutTrxFetchInvDataElement.add(fetchSize);
                    } else {
                        gscOutBlockFetchInvData.add();
                        gscOutBlockFetchInvDataElement.add(fetchSize);
                    }
                }
                break;
            case TRXS:
                TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
                if (flag) {
                    gscInTrxs.add();
                    gscInTrx.add(transactionsMessage.getTransactions().getTransactionsCount());
                } else {
                    gscOutTrxs.add();
                    gscOutTrx.add(transactionsMessage.getTransactions().getTransactionsCount());
                }
                break;
            case TRX:
                if (flag) {
                    gscInMessage.add();
                } else {
                    gscOutMessage.add();
                }
                break;
            case BLOCK:
                if (flag) {
                    gscInBlock.add();
                }
                gscOutBlock.add();
                break;
            default:
                break;
        }
    }

}
