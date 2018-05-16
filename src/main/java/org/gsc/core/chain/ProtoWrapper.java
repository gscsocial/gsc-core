package org.gsc.core.chain;

public interface ProtoWrapper<T> {

  byte[] getData();

  T getInstance();

}
