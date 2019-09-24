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

package org.gsc.db.api.index;

import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;
import org.gsc.db.common.WrappedByteArray;

public class Index {

    public interface Iface<T> extends Iterable<T> {

        ResultSet<T> retrieve(Query<WrappedByteArray> query);

        ResultSet<T> retrieve(Query<WrappedByteArray> query, QueryOptions options);

        boolean add(byte[] bytes);

        boolean add(WrappedByteArray bytes);

        boolean update(byte[] bytes);

        boolean update(WrappedByteArray bytes);

        boolean remove(byte[] bytes);

        boolean remove(WrappedByteArray bytes);

        long size();

        String getName();

        void fill();
    }
}
