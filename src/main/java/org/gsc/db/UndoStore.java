package org.gsc.db;

public class UndoStore extends AbstractUndoStore {

  private UndoStore() {
  }

  public static UndoStore getInstance() {
    return UndoEnum.INSTANCE.getInstance();
  }

  private enum UndoEnum {
    INSTANCE;

    private UndoStore instance;

    UndoEnum() {
      instance = new UndoStore();
    }

    private UndoStore getInstance() {
      return instance;
    }
  }
}
