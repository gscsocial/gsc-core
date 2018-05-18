package org.gsc.core.wrapper;

public interface ProtoWrapper<T> {

  byte[] getData();

  T getInstance();

}
