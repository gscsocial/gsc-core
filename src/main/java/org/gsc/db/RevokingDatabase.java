package org.gsc.db;

import org.gsc.core.exception.RevokingStoreIllegalStateException;

public interface RevokingDatabase {

  AbstractRevokingStore.Dialog buildDialog();

  AbstractRevokingStore.Dialog buildDialog(boolean forceEnable);

  void onCreate(AbstractRevokingStore.RevokingTuple tuple, byte[] value);

  void onModify(AbstractRevokingStore.RevokingTuple tuple, byte[] value);

  void onRemove(AbstractRevokingStore.RevokingTuple tuple, byte[] value);

  void merge() throws RevokingStoreIllegalStateException;

  void revoke() throws RevokingStoreIllegalStateException;

  void commit() throws RevokingStoreIllegalStateException;

  void pop() throws RevokingStoreIllegalStateException;

  AbstractRevokingStore.RevokingState head();

  void enable();

  int size();

  void disable();

  void shutdown();
}
