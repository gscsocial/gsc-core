package org.gsc.net.discover.dht;

import java.security.SecureRandom;
import java.nio.*;
import java.nio.charset.Charset;

public interface Utils {
  SecureRandom random = new SecureRandom();

  static SecureRandom getRandom() {
    return random;
  }

  static byte[] getBytes(char[] chars) {
    Charset cs = Charset.forName("UTF-8");
    CharBuffer cb = CharBuffer.allocate(chars.length);
    cb.put(chars);
    cb.flip();
    ByteBuffer bb = cs.encode(cb);

    return bb.array();
  }

  public static String getIdShort(String Id) {
    return Id == null ? "<null>" : Id.substring(0, 8);
  }

  static char[] getChars(byte[] bytes) {
    Charset cs = Charset.forName("UTF-8");
    ByteBuffer bb = ByteBuffer.allocate(bytes.length);
    bb.put(bytes);
    bb.flip();
    CharBuffer cb = cs.decode(bb);

    return cb.array();
  }

  static byte[] clone(byte[] value) {
    byte[] clone = new byte[value.length];
    System.arraycopy(value, 0, clone, 0, value.length);
    return clone;
  }

  static String sizeToStr(long size) {
    if (size < 2 * (1L << 10)) return size + "b";
    if (size < 2 * (1L << 20)) return String.format("%dKb", size / (1L << 10));
    if (size < 2 * (1L << 30)) return String.format("%dMb", size / (1L << 20));
    return String.format("%dGb", size / (1L << 30));
  }
}
