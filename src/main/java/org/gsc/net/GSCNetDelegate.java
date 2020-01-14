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

package org.gsc.net;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.net.peer.p2p.Message;
import org.gsc.net.server.ChannelManager;
import org.gsc.net.server.SyncPool;
import org.gsc.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.db.Manager;
import org.gsc.db.WitnessScheduleStore;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.BadBlockException;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.BadNumberBlockException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractSizeNotEqualToOneException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.DupTransactionException;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.core.exception.NonCommonBlockException;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.P2pException.TypeEnum;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.StoreException;
import org.gsc.core.exception.TaposException;
import org.gsc.core.exception.TooBigTransactionException;
import org.gsc.core.exception.TooBigTransactionResultException;
import org.gsc.core.exception.TransactionExpirationException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.core.exception.ValidateScheduleException;
import org.gsc.core.exception.ValidateSignatureException;
import org.gsc.net.peer.message.BlockMessage;
import org.gsc.net.peer.message.MessageTypes;
import org.gsc.net.peer.message.TransactionMessage;
import org.gsc.net.peer.PeerConnection;
import org.gsc.protos.Protocol.Inventory.InventoryType;

@Slf4j(topic = "net")
@Component
public class GSCNetDelegate {

    @Autowired
    private SyncPool syncPool;

    @Autowired
    private ChannelManager channelManager;

    @Autowired
    private Manager dbManager;

    @Autowired
    private WitnessScheduleStore witnessScheduleStore;

    @Getter
    private Object blockLock = new Object();

    private final static int blockIdCacheSize = 100;

    private Queue<BlockId> freshBlockId = new ConcurrentLinkedQueue<BlockId>() {
        @Override
        public boolean offer(BlockId blockId) {
            if (size() > blockIdCacheSize) {
                super.poll();
            }
            return super.offer(blockId);
        }
    };

    public void trustNode(PeerConnection peer) {
        channelManager.getTrustNodes().put(peer.getInetAddress(), peer.getNode());
    }

    public Collection<PeerConnection> getActivePeer() {
        return syncPool.getActivePeers();
    }

    public long getSyncBeginNumber() {
        return dbManager.getSyncBeginNumber();
    }

    public long getBlockTime(BlockId id) throws P2pException {
        try {
            return dbManager.getBlockById(id).getTimeStamp();
        } catch (BadItemException | ItemNotFoundException e) {
            throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND, id.getString());
        }
    }

    public BlockId getSolidBlockId() {
        return dbManager.getConfirmedBlockId();
    }

    public BlockId getHeadBlockId() {
        return dbManager.getHeadBlockId();
    }

    public BlockId getBlockIdByNum(long num) throws P2pException {
        try {
            return dbManager.getBlockIdByNum(num);
        } catch (ItemNotFoundException e) {
            throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND, "num: " + num);
        }
    }

    public BlockId getGenesisBlockId() {
        return dbManager.getGenesisBlockId();
    }

    public BlockWrapper getGenesisBlock() {
        return dbManager.getGenesisBlock();
    }

    public long getHeadBlockTimeStamp() {
        return dbManager.getHeadBlockTimeStamp();
    }

    public boolean containBlock(BlockId id) {
        return dbManager.containBlock(id);
    }

    public boolean containBlockInMainChain(BlockId id) {
        return dbManager.containBlockInMainChain(id);
    }

    public LinkedList<BlockId> getBlockChainHashesOnFork(BlockId forkBlockHash) throws P2pException {
        try {
            return dbManager.getBlockChainHashesOnFork(forkBlockHash);
        } catch (NonCommonBlockException e) {
            throw new P2pException(TypeEnum.HARD_FORKED, forkBlockHash.getString());
        }
    }

    public boolean contain(Sha256Hash hash, MessageTypes type) {
        if (type.equals(MessageTypes.BLOCK)) {
            return dbManager.containBlock(hash);
        } else if (type.equals(MessageTypes.TRX)) {
            return dbManager.getTransactionStore().has(hash.getBytes());
        }
        return false;
    }

    public boolean canChainRevoke(long num) {
        return num >= dbManager.getSyncBeginNumber();
    }

    public Message getData(Sha256Hash hash, InventoryType type) throws P2pException {
        try {
            switch (type) {
                case BLOCK:
                    return new BlockMessage(dbManager.getBlockById(hash));
                case TRX:
                    TransactionWrapper tx = dbManager.getTransactionStore().get(hash.getBytes());
                    if (tx != null) {
                        return new TransactionMessage(tx.getInstance());
                    }
                    throw new StoreException();
                default:
                    throw new StoreException();
            }
        } catch (StoreException e) {
            throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND,
                    "type: " + type + ", hash: " + hash.getByteString());
        }
    }

    public void processBlock(BlockWrapper block) throws P2pException {
        synchronized (blockLock) {
            try {
                if (!freshBlockId.contains(block.getBlockId())) {
                    if (block.getNum() <= getHeadBlockId().getNum()) {
                        logger.warn("Receive a fork block {} witness {}, head {}",
                                block.getBlockId().getString(),
                                Hex.toHexString(block.getWitnessAddress().toByteArray()),
                                getHeadBlockId().getString());
                    }
                    dbManager.pushBlock(block);
                    freshBlockId.add(block.getBlockId());
                    logger.info("Success process block {}.", block.getBlockId().getString());
                }
            } catch (ValidateSignatureException
                    | ContractValidateException
                    | ContractExeException
                    | UnLinkedBlockException
                    | ValidateScheduleException
                    | AccountResourceInsufficientException
                    | TaposException
                    | TooBigTransactionException
                    | TooBigTransactionResultException
                    | DupTransactionException
                    | TransactionExpirationException
                    | BadNumberBlockException
                    | BadBlockException
                    | NonCommonBlockException
                    | ReceiptCheckErrException
                    | VMIllegalException e) {
                throw new P2pException(TypeEnum.BAD_BLOCK, e);
            }
        }
    }

    public void pushTransaction(TransactionWrapper trx) throws P2pException {
        try {
            dbManager.pushTransaction(trx);
        } catch (ContractSizeNotEqualToOneException
                | VMIllegalException e) {
            throw new P2pException(TypeEnum.BAD_TRX, e);
        } catch (ContractValidateException
                | ValidateSignatureException
                | ContractExeException
                | DupTransactionException
                | TaposException
                | TooBigTransactionException
                | TransactionExpirationException
                | ReceiptCheckErrException
                | TooBigTransactionResultException
                | AccountResourceInsufficientException e) {
            throw new P2pException(TypeEnum.TRX_EXE_FAILED, e);
        }
    }

    public boolean validBlock(BlockWrapper block) throws P2pException {
        try {
            return witnessScheduleStore.getActiveWitnesses().contains(block.getWitnessAddress())
                    && block.validateSignature(dbManager);
        } catch (ValidateSignatureException e) {
            throw new P2pException(TypeEnum.BAD_BLOCK, e);
        }
    }
}
