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

package org.gsc.utils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by olivier on 2018/06/01
 */
class SlotBaseCounter {

    private int slotSize;
    private AtomicInteger[] slotCounter;

    public SlotBaseCounter(int slotSize) {
        slotSize = slotSize < 1 ? 1 : slotSize;
        this.slotSize = slotSize;
        this.slotCounter = new AtomicInteger[slotSize];
        for (int i = 0; i < this.slotSize; i++) {
            slotCounter[i] = new AtomicInteger(0);
        }
    }

    public void increaseSlot(int slotSize) {
        slotCounter[slotSize].incrementAndGet();
    }

    public void wipeSlot(int slotSize) {
        slotCounter[slotSize].set(0);
    }

    public int totalCount() {
        return Arrays.stream(slotCounter).mapToInt(slotCounter -> slotCounter.get()).sum();
    }

    @Override
    public String toString() {
        return Arrays.toString(slotCounter);
    }
}

/**
 * Created by olivier on 2018/06/01
 */
public class SlidingWindowCounter {

    private volatile SlotBaseCounter slotBaseCounter;
    private volatile int windowSize;
    private volatile int head;

    public SlidingWindowCounter(int windowSize) {
        resizeWindow(windowSize);
    }

    public synchronized void resizeWindow(int windowSize) {
        this.windowSize = windowSize;
        this.slotBaseCounter = new SlotBaseCounter(windowSize);
        this.head = 0;
    }

    public void increase() {
        slotBaseCounter.increaseSlot(head);
    }

    public int totalAndAdvance() {
        int total = totalCount();
        advance();
        return total;
    }

    public void advance() {
        int tail = (head + 1) % windowSize;
        slotBaseCounter.wipeSlot(tail);
        head = tail;
    }

    public int totalCount() {
        return slotBaseCounter.totalCount();
    }

    @Override
    public String toString() {
        return "total = " + totalCount() + " head = " + head + " >> " + slotBaseCounter;
    }
}