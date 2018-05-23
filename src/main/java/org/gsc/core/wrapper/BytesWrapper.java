package org.gsc.core.wrapper;

public class BytesWrapper implements StoreWrapper {

  byte[] bytes;

  public BytesWrapper(byte[] bytes) {
    this.bytes = bytes;
  }

  @Override
  public byte[] getData() {
    return bytes;
  }

  @Override
  public Object getInstance() {
    return null;
  }
}
