package org.gsc.core.wrapper;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
