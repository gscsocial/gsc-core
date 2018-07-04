package org.gsc.core.db;

import org.gsc.core.db.AbstractRevokingStore.Dialog;
import org.gsc.core.db.AbstractRevokingStore.RevokingState;
import org.gsc.core.db.AbstractRevokingStore.RevokingTuple;
import org.gsc.core.exception.RevokingStoreIllegalStateException;
import org.gsc.core.db.AbstractRevokingStore.Dialog;
import org.gsc.core.db.AbstractRevokingStore.RevokingState;
import org.gsc.core.db.AbstractRevokingStore.RevokingTuple;
import org.gsc.core.exception.RevokingStoreIllegalStateException;

public interface RevokingDatabase {

  Dialog buildDialog();

  Dialog buildDialog(boolean forceEnable);

  void onCreate(RevokingTuple tuple, byte[] value);

  void onModify(RevokingTuple tuple, byte[] value);

  void onRemove(RevokingTuple tuple, byte[] value);

  void merge() throws RevokingStoreIllegalStateException;

  void revoke() throws RevokingStoreIllegalStateException;

  void commit() throws RevokingStoreIllegalStateException;

  void pop() throws RevokingStoreIllegalStateException;

  RevokingState head();

  void enable();

  int size();

  void disable();

  void shutdown();
}
