package org.gsc.db.db2.common;

import java.util.Map;

import org.gsc.db.common.WrappedByteArray;

public interface Flusher {

    void flush(Map<WrappedByteArray, WrappedByteArray> batch);

    void close();

    void reset();
}
