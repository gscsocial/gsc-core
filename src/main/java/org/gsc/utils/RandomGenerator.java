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

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "utils")
public class RandomGenerator<T> {

    private static long RANDOM_GENERATOR_NUMBER = 2685821657736338717L;

    public List<T> shuffle(List<T> list, long time) {
        long headBlockTimeHi = time << 32;

        for (int i = 0; i < list.size(); i++) {
            long v = headBlockTimeHi + i * RANDOM_GENERATOR_NUMBER;
            v = v ^ (v >> 12);
            v = v ^ (v << 25);
            v = v ^ (v >> 27);
            v = v * RANDOM_GENERATOR_NUMBER;

            int index = (int) (i + v % (list.size() - i));
            if (index < 0 || index >= list.size()) {
                logger.warn("index[" + index + "] is out of range[0," + (list.size() - 1) + "],skip");
                continue;
            }
            T tmp = list.get(index);
            list.set(index, list.get(i));
            list.set(i, tmp);
        }
        return list;
    }

}
