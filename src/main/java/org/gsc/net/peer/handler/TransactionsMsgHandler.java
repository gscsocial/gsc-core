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

package org.gsc.net.peer.handler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.config.args.Args;
import org.gsc.core.exception.P2pException;
import org.gsc.core.exception.P2pException.TypeEnum;
import org.gsc.net.GSCNetDelegate;
import org.gsc.net.peer.message.TransactionMessage;
import org.gsc.net.peer.message.TransactionsMessage;
import org.gsc.net.peer.message.GSCMessage;
import org.gsc.net.peer.Item;
import org.gsc.net.service.AdvService;
import org.gsc.net.peer.PeerConnection;
import org.gsc.protos.Protocol.Inventory.InventoryType;
import org.gsc.protos.Protocol.ReasonCode;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j(topic = "net")
@Component
public class TransactionsMsgHandler implements GSCMsgHandler {

    @Autowired
    private GSCNetDelegate gscNetDelegate;

    @Autowired
    private AdvService advService;

    private static int MAX_TRX_SIZE = 50_000;

    private static int MAX_SMART_CONTRACT_SUBMIT_SIZE = 100;

//  private static int TIME_OUT = 10 * 60 * 1000;

    private BlockingQueue<TrxEvent> smartContractQueue = new LinkedBlockingQueue(MAX_TRX_SIZE);

    private BlockingQueue<Runnable> queue = new LinkedBlockingQueue();

    private int threadNum = Args.getInstance().getValidateSignThreadNum();
    private ExecutorService trxHandlePool = new ThreadPoolExecutor(threadNum, threadNum, 0L,
            TimeUnit.MILLISECONDS, queue);

    private ScheduledExecutorService smartContractExecutor = Executors
            .newSingleThreadScheduledExecutor();

    class TrxEvent {

        @Getter
        private PeerConnection peer;
        @Getter
        private TransactionMessage msg;
        @Getter
        private long time;

        public TrxEvent(PeerConnection peer, TransactionMessage msg) {
            this.peer = peer;
            this.msg = msg;
            this.time = System.currentTimeMillis();
        }
    }

    public void init() {
        handleSmartContract();
    }

    public void close() {
        smartContractExecutor.shutdown();
    }

    public boolean isBusy() {
        return queue.size() + smartContractQueue.size() > MAX_TRX_SIZE;
    }

    @Override
    public void processMessage(PeerConnection peer, GSCMessage msg) throws P2pException {
        TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
        check(peer, transactionsMessage);
        for (Transaction trx : transactionsMessage.getTransactions().getTransactionsList()) {
            int type = trx.getRawData().getContract(0).getType().getNumber();
            if (type == ContractType.TriggerSmartContract_VALUE
                    || type == ContractType.CreateSmartContract_VALUE) {
                if (!smartContractQueue.offer(new TrxEvent(peer, new TransactionMessage(trx)))) {
                    logger.warn("Add smart contract failed, queueSize {}:{}", smartContractQueue.size(),
                            queue.size());
                }
            } else {
                trxHandlePool.submit(() -> handleTransaction(peer, new TransactionMessage(trx)));
            }
        }
    }

    private void check(PeerConnection peer, TransactionsMessage msg) throws P2pException {
        for (Transaction trx : msg.getTransactions().getTransactionsList()) {
            Item item = new Item(new TransactionMessage(trx).getMessageId(), InventoryType.TRX);
            if (!peer.getAdvInvRequest().containsKey(item)) {
                throw new P2pException(TypeEnum.BAD_MESSAGE,
                        "trx: " + msg.getMessageId() + " without request.");
            }
            peer.getAdvInvRequest().remove(item);
        }
    }

    private void handleSmartContract() {
        smartContractExecutor.scheduleWithFixedDelay(() -> {
            try {
                while (queue.size() < MAX_SMART_CONTRACT_SUBMIT_SIZE) {
                    TrxEvent event = smartContractQueue.take();
                    trxHandlePool.submit(() -> handleTransaction(event.getPeer(), event.getMsg()));
                }
            } catch (Exception e) {
                logger.error("Handle smart contract exception.", e);
            }
        }, 1000, 20, TimeUnit.MILLISECONDS);
    }

    private void handleTransaction(PeerConnection peer, TransactionMessage trx) {
        if (peer.isDisconnect()) {
            logger.warn("Drop trx {} from {}, peer is disconnect.", trx.getMessageId(),
                    peer.getInetAddress());
            return;
        }

        if (advService.getMessage(new Item(trx.getMessageId(), InventoryType.TRX)) != null) {
            return;
        }

        try {
            gscNetDelegate.pushTransaction(trx.getTransactionWrapper());
            advService.broadcast(trx);
        } catch (P2pException e) {
            logger.warn("Trx {} from peer {} process failed. type: {}, reason: {}",
                    trx.getMessageId(), peer.getInetAddress(), e.getType(), e.getMessage());
            if (e.getType().equals(TypeEnum.BAD_TRX)) {
                peer.disconnect(ReasonCode.BAD_TX);
            }
        } catch (Exception e) {
            logger.error("Trx {} from peer {} process failed.", trx.getMessageId(), peer.getInetAddress(),
                    e);
        }
    }
}