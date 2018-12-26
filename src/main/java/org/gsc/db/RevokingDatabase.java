package org.gsc.db;

import org.gsc.core.db2.common.IRevokingDB;
import org.gsc.core.db2.core.ISession;
import org.gsc.core.exception.RevokingStoreIllegalStateException;

public interface RevokingDatabase {

  ISession buildSession();

  ISession buildSession(boolean forceEnable);

  void add(IRevokingDB revokingDB);

  void merge() throws RevokingStoreIllegalStateException;

  void revoke() throws RevokingStoreIllegalStateException;

  void commit() throws RevokingStoreIllegalStateException;

  void pop() throws RevokingStoreIllegalStateException;

  void enable();

  void check();

  int size();
  
  void setMaxSize(int maxSize);

  void disable();

  void shutdown();
}
