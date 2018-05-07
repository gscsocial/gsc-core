package org.gsc.net.message;

public interface MessageFactory {
  Message create(byte code, byte[] encoded);
}
