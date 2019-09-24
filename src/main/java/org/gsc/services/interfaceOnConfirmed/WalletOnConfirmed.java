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

package org.gsc.services.interfaceOnConfirmed;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.core.Wallet;
import org.gsc.config.args.Args;
import org.gsc.db.Manager;

@Slf4j(topic = "API")
@Component
public class WalletOnConfirmed {

    private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
            Executors.newFixedThreadPool(Args.getInstance().getConfirmedThreadNum(),
                    new ThreadFactoryBuilder().setNameFormat("WalletOnConfirmed-%d").build()));

    @Autowired
    private Manager dbManager;
    @Autowired
    private Wallet wallet;

    public <T> T futureGet(Callable<T> callable) {
        ListenableFuture<T> future = executorService.submit(() -> {
            try {
                dbManager.setMode(false);
                return callable.call();
            } catch (Exception e) {
                logger.info("futureGet " + e.getMessage());
                return null;
            }
        });

        try {
            return future.get(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
        } catch (TimeoutException e) {
            logger.info("futureGet time out");
        }

        return null;
    }

    public void futureGet(Runnable runnable) {
        ListenableFuture<?> future = executorService.submit(() -> {
            try {
                dbManager.setMode(false);
                runnable.run();
            } catch (Exception e) {
                logger.info("futureGet " + e.getMessage());
            }
        });

        try {
            future.get(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
        } catch (TimeoutException e) {
            logger.info("futureGet time out");
        }
    }
}
