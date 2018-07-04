package org.gsc.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
