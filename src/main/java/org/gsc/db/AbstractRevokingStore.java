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

package org.gsc.db;

import static org.gsc.db.db2.core.SnapshotManager.simpleDecode;

import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.db.dbsource.SourceInter;
import org.gsc.db.dbsource.WriteOptionsWrapper;
import org.gsc.db.dbsource.leveldb.LevelDbDataSourceImpl;
import org.gsc.utils.FileUtil;
import org.gsc.utils.Utils;
import org.gsc.config.args.Args;
import org.gsc.db.db2.common.IRevokingDB;
import org.gsc.db.db2.core.ISession;
import org.gsc.db.db2.core.RevokingDBWithCachingOldValue;
import org.gsc.core.exception.RevokingStoreIllegalStateException;

@Slf4j(topic = "DB")
@Getter // only for unit test
public abstract class AbstractRevokingStore implements RevokingDatabase {

    private static String ACTIVE_DIALOG_POSITIVE = "activeDialog has to be greater than 0";

    private static final int DEFAULT_STACK_MAX_SIZE = 256;

    private Deque<RevokingState> stack = new LinkedList<>();
    private boolean disabled = true;
    private int activeDialog = 0;
    private AtomicInteger maxSize = new AtomicInteger(DEFAULT_STACK_MAX_SIZE);
    private WriteOptionsWrapper writeOptionsWrapper = WriteOptionsWrapper.getInstance()
            .sync(Args.getInstance().getStorage().isDbSync());
    private List<LevelDbDataSourceImpl> dbs = new ArrayList<>();

    @Override
    public ISession buildSession() {
        return buildSession(false);
    }

    @Override
    public synchronized ISession buildSession(boolean forceEnable) {
        if (disabled && !forceEnable) {
            return new Dialog(this);
        }

        boolean disableOnExit = disabled && forceEnable;
        if (forceEnable) {
            disabled = false;
        }

        while (stack.size() > maxSize.get()) {
            stack.poll();
        }

        stack.add(new RevokingState());
        ++activeDialog;
        return new Dialog(this, disableOnExit);
    }

    @Override
    public void setMode(boolean mode) {

    }

    @Override
    public synchronized void check() {
        LevelDbDataSourceImpl check =
                new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName("tmp"), "tmp");
        check.initDB();

        if (!check.allKeys().isEmpty()) {
            Map<String, LevelDbDataSourceImpl> dbMap = dbs.stream()
                    .map(db -> Maps.immutableEntry(db.getDBName(), db))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            for (Map.Entry<byte[], byte[]> e : check) {
                byte[] key = e.getKey();
                byte[] value = e.getValue();
                String db = simpleDecode(key);
                if (dbMap.get(db) == null) {
                    continue;
                }
                byte[] realKey = Arrays.copyOfRange(key, db.getBytes().length + 4, key.length);

                byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
                if (realValue != null) {
                    dbMap.get(db).putData(realKey, realValue, WriteOptionsWrapper.getInstance()
                            .sync(Args.getInstance().getStorage().isDbSync()));
                } else {
                    dbMap.get(db).deleteData(realKey, WriteOptionsWrapper.getInstance()
                            .sync(Args.getInstance().getStorage().isDbSync()));
                }
            }
        }

        check.closeDB();
        FileUtil.recursiveDelete(check.getDbPath().toString());
    }

    @Override
    public void add(IRevokingDB revokingDB) {
        dbs.add(((RevokingDBWithCachingOldValue) revokingDB).getDbSource());
    }

    public synchronized void onCreate(RevokingTuple tuple, byte[] value) {
        if (disabled) {
            return;
        }

        addIfEmpty();
        RevokingState state = stack.peekLast();
        state.newIds.add(tuple);
    }

    public synchronized void onModify(RevokingTuple tuple, byte[] value) {
        if (disabled) {
            return;
        }

        addIfEmpty();
        RevokingState state = stack.peekLast();
        if (state.newIds.contains(tuple) || state.oldValues.containsKey(tuple)) {
            return;
        }

        state.oldValues.put(tuple, Utils.clone(value));
    }

    public synchronized void onRemove(RevokingTuple tuple, byte[] value) {
        if (disabled) {
            return;
        }

        addIfEmpty();
        RevokingState state = stack.peekLast();
        if (state.newIds.contains(tuple)) {
            state.newIds.remove(tuple);
            return;
        }

        if (state.oldValues.containsKey(tuple)) {
            state.removed.put(tuple, state.oldValues.get(tuple));
            state.oldValues.remove(tuple);
            return;
        }

        if (state.removed.containsKey(tuple)) {
            return;
        }

        state.removed.put(tuple, Utils.clone(value));
    }

    @Override
    public synchronized void merge() {
        if (activeDialog <= 0) {
            throw new RevokingStoreIllegalStateException(ACTIVE_DIALOG_POSITIVE);
        }

        if (activeDialog == 1 && stack.size() == 1) {
            stack.pollLast();
            --activeDialog;
            return;
        }

        if (stack.size() < 2) {
            return;
        }

        RevokingState state = stack.peekLast();
        @SuppressWarnings("unchecked")
        List<RevokingState> list = (List<RevokingState>) stack;
        RevokingState prevState = list.get(stack.size() - 2);

        state.oldValues.entrySet().stream()
                .filter(e -> !prevState.newIds.contains(e.getKey()))
                .filter(e -> !prevState.oldValues.containsKey(e.getKey()))
                .forEach(e -> prevState.oldValues.put(e.getKey(), e.getValue()));

        prevState.newIds.addAll(state.newIds);

        state.removed.entrySet().stream()
                .filter(e -> {
                    boolean has = prevState.newIds.contains(e.getKey());
                    if (has) {
                        prevState.newIds.remove(e.getKey());
                    }

                    return !has;
                })
                .filter(e -> {
                    boolean has = prevState.oldValues.containsKey(e.getKey());
                    if (has) {
                        prevState.removed.put(e.getKey(), prevState.oldValues.get(e.getKey()));
                        prevState.oldValues.remove(e.getKey());
                    }

                    return !has;
                })
                .forEach(e -> prevState.removed.put(e.getKey(), e.getValue()));

        stack.pollLast();
        --activeDialog;
    }

    @Override
    public synchronized void revoke() {
        if (disabled) {
            return;
        }

        if (activeDialog <= 0) {
            throw new RevokingStoreIllegalStateException(ACTIVE_DIALOG_POSITIVE);
        }

        disabled = true;

        try {
            RevokingState state = stack.peekLast();
            if (Objects.isNull(state)) {
                return;
            }

            state.oldValues.forEach((k, v) -> k.database.putData(k.key, v));
            state.newIds.forEach(e -> e.database.deleteData(e.key));
            state.removed.forEach((k, v) -> k.database.putData(k.key, v));
            stack.pollLast();
        } finally {
            disabled = false;
        }
        --activeDialog;
    }

    @Override
    public synchronized void commit() {
        if (activeDialog <= 0) {
            throw new RevokingStoreIllegalStateException(ACTIVE_DIALOG_POSITIVE);
        }

        --activeDialog;
    }

    @Override
    public synchronized void pop() {
        prune(writeOptionsWrapper);
    }

    @Override
    public synchronized void fastPop() {
        prune(WriteOptionsWrapper.getInstance());
    }

    private synchronized void prune(WriteOptionsWrapper optionsWrapper) {
        if (activeDialog != 0) {
            throw new RevokingStoreIllegalStateException("activeDialog has to be equal 0");
        }

        if (stack.isEmpty()) {
            throw new RevokingStoreIllegalStateException("stack is empty");
        }

        disabled = true;

        try {
            RevokingState state = stack.peekLast();
            state.oldValues.forEach((k, v) -> k.database.putData(k.key, v, optionsWrapper));
            state.newIds.forEach(e -> e.database.deleteData(e.key, optionsWrapper));
            state.removed.forEach((k, v) -> k.database.putData(k.key, v, optionsWrapper));
            stack.pollLast();
        } finally {
            disabled = false;
        }
    }

    @Override
    public synchronized void enable() {
        disabled = false;
    }

    @Override
    public synchronized void disable() {
        disabled = true;
    }

    private void addIfEmpty() {
        if (stack.isEmpty()) {
            stack.add(new RevokingState());
        }
    }

    @Override
    public synchronized int size() {
        return stack.size();
    }

    @Override
    public void setMaxSize(int maxSize) {
        this.maxSize.set(maxSize);
    }

    public int getMaxSize() {
        return maxSize.get();
    }

    @Override
    public void setMaxFlushCount(int maxFlushCount) {
    }

    public synchronized void shutdown() {
        System.err.println("******** begin to pop revokingDb ********");
        System.err.println("******** before revokingDb size:" + size());
        try {
            disable();
            while (true) {
                try {
                    commit();
                } catch (RevokingStoreIllegalStateException e) {
                    break;
                }
                if (activeDialog <= 0) {
                    break;
                }
            }

            while (true) {
                try {
                    pop();
                } catch (RevokingStoreIllegalStateException e) {
                    break;
                }
                if (activeDialog != 0) {
                    break;
                }
                if (stack.isEmpty()) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("******** failed to pop revokingStore. " + e);
        } finally {
            System.err.println("******** after revokingStore size:" + stack.size());
            System.err.println("******** after revokingStore contains:" + stack);
            System.err.println("******** end to pop revokingStore ********");
        }
    }

    @Slf4j(topic = "DB")
    @Getter // only for unit test
    public static class Dialog implements ISession {

        private RevokingDatabase revokingDatabase;
        private boolean applyRevoking = true;
        private boolean disableOnExit = false;

        public Dialog(Dialog dialog) {
            this.revokingDatabase = dialog.revokingDatabase;
            this.applyRevoking = dialog.applyRevoking;
            dialog.applyRevoking = false;
        }

        public Dialog(RevokingDatabase revokingDatabase) {
            this(revokingDatabase, false);
        }

        public Dialog(RevokingDatabase revokingDatabase, boolean disableOnExit) {
            this.revokingDatabase = revokingDatabase;
            this.disableOnExit = disableOnExit;
        }

        @Override
        public void commit() {
            applyRevoking = false;
            revokingDatabase.commit();
        }

        @Override
        public void revoke() {
            if (applyRevoking) {
                revokingDatabase.revoke();
            }

            applyRevoking = false;
        }

        @Override
        public void merge() {
            if (applyRevoking) {
                revokingDatabase.merge();
            }

            applyRevoking = false;
        }

        void copy(Dialog dialog) {
            if (this.equals(dialog)) {
                return;
            }

            if (applyRevoking) {
                revokingDatabase.revoke();
            }
            applyRevoking = dialog.applyRevoking;
            dialog.applyRevoking = false;
        }

        @Override
        public void destroy() {
            try {
                if (applyRevoking) {
                    revokingDatabase.revoke();
                }
            } catch (Exception e) {
                logger.error("revoke database error.", e);
            }
            if (disableOnExit) {
                revokingDatabase.disable();
            }
        }

        @Override
        public void close() {
            try {
                if (applyRevoking) {
                    revokingDatabase.revoke();
                }
            } catch (Exception e) {
                logger.error("revoke database error.", e);
                throw new RevokingStoreIllegalStateException(e);
            }
            if (disableOnExit) {
                revokingDatabase.disable();
            }
        }
    }

    @ToString
    @Getter // only for unit test
    static class RevokingState {

        private Map<RevokingTuple, byte[]> oldValues = new HashMap<>();
        private Set<RevokingTuple> newIds = new HashSet<>();
        private Map<RevokingTuple, byte[]> removed = new HashMap<>();
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    @Getter
    @ToString
    public static class RevokingTuple {

        private SourceInter<byte[], byte[]> database;
        private byte[] key;
    }

}
