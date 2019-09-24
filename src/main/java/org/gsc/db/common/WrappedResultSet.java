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

package org.gsc.db.common;

import com.google.common.collect.Iterables;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;

public abstract class WrappedResultSet<T> extends ResultSet<T> {

    private ResultSet<WrappedByteArray> resultSet;

    public WrappedResultSet(ResultSet<WrappedByteArray> resultSet) {
        this.resultSet = resultSet;
    }

//  @Override
//  public Iterator<T> iterator() {
//    return null;
//  }

    @Override
    public boolean contains(T object) {
        return Iterables.contains(this, object);
    }

    @Override
    public boolean matches(T object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query<T> getQuery() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryOptions getQueryOptions() {
        return resultSet.getQueryOptions();
    }

    @Override
    public int getRetrievalCost() {
        return resultSet.getRetrievalCost();
    }

    @Override
    public int getMergeCost() {
        return resultSet.getMergeCost();
    }

    @Override
    public int size() {
        return resultSet.size();
    }

    @Override
    public void close() {
        resultSet.close();
    }
}
