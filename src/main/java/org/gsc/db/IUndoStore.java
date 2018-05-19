package org.gsc.db;

import org.gsc.common.exception.RevokingStoreIllegalStateException;
import org.gsc.db.UndoStore.Dialog;
import org.gsc.db.UndoStore.UndoState;
import org.gsc.db.UndoStore.UndoTuple;

public interface IUndoStore {

  Dialog buildDialog();

  Dialog buildDialog(boolean forceEnable);

  void onCreate(UndoTuple tuple, byte[] value);

  void onModify(UndoTuple tuple, byte[] value);

  void onRemove(UndoTuple tuple, byte[] value);

  void merge() throws RevokingStoreIllegalStateException;

  void revoke() throws RevokingStoreIllegalStateException;

  void commit() throws RevokingStoreIllegalStateException;

  void pop() throws RevokingStoreIllegalStateException;

  UndoState head();

  void enable();

  int size();

  void disable();

  void shutdown();
}
