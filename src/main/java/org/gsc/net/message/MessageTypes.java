package org.gsc.net.message;

import java.util.HashMap;
import java.util.Map;

public enum MessageTypes {

  FIRST(0x00),

  DISCOVER_PING(0x01),

  DISCOVER_PONG(0x02),

  DISCOVER_FIND_PEER(0x03),

  DISCOVER_PEERS(0x04),

  P2P_HELLO(0x05),

  P2P_DISCONNECT(0x06),

  P2P_PING(0x07),

  P2P_PONG(0x08),

  BLOCK(0x10),

  TRANSACTION(0x11),

  FETCH(0x12),

  SYNC(0x13),

  ATTENTION(0x14),

  TIME(0x15),

  LAST(0xFF);

  private final int type;

  private static final Map<Integer, MessageTypes> intToTypeMap = new HashMap<>();

  static {
    for (MessageTypes type : MessageTypes.values()) {
      intToTypeMap.put(type.type, type);
    }
  }

  private MessageTypes(int type) {
    this.type = type;
  }

  public static MessageTypes fromByte(byte i) {
    return intToTypeMap.get((int) i);
  }

  public static boolean inRange(byte code) {
    return code < LAST.asByte();
  }

  public byte asByte() {
    return (byte) (type);
  }

  public static boolean inP2pRange(byte code) {
    return code <= P2P_PONG.asByte() && code >= P2P_HELLO.asByte();
  }

  public static boolean inGscRange(byte code) {
    return false;
  }

  @Override
  public String toString() {
    switch (type) {
      case 1:
        return "DISCOVER_PING";
      case 2:
        return "DISCOVER_PONG";
      case 3:
        return "DISCOVER_FIND_PEER";
      case 4:
        return "DISCOVER_PEERS";
      case 5:
        return "P2P_HELLO";
      case 6:
        return "P2P_DISCONNECT";
      case 7:
        return "P2P_PING";
      case 8:
        return "P2P_PONG";
      default:
        break;
    }
    return super.toString();
  }
}



