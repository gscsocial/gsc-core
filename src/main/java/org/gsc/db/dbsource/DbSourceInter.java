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


package org.gsc.db.dbsource;

import java.util.Map;
import java.util.Set;


public interface DbSourceInter<V> extends BatchSourceInter<byte[], V>,
        Iterable<Map.Entry<byte[], V>> {


    String getDBName();

    void setDBName(String name);

    void initDB();


    boolean isAlive();


    void closeDB();

    void resetDb();

    Set<byte[]> allKeys() throws RuntimeException;

    Set<byte[]> allValues() throws RuntimeException;

    long getTotal() throws RuntimeException;

}
