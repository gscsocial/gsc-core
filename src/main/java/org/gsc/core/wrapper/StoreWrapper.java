package org.gsc.core.wrapper;

public interface StoreWrapper<T> {

  byte[] getData();

  T getInstance();

}
