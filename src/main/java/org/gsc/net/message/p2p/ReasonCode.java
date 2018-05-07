package org.gsc.net.message.p2p;

import java.util.HashMap;
import java.util.Map;

/**
 * Reason is an optional integer specifying one
 * of a number of reasons for disconnect
 */
public enum ReasonCode {

  /**
   * [0x02] Packets can not be parsed
   */
  BAD_PROTOCOL(0x01),

  /**
   * [0x04] Already too many connections with other peers
   */
  TOO_MANY_PEERS(0x02),

  /**
   * [0x05] Already have a running connection with this peer
   */
  DUPLICATE_PEER(0x03),

  /**
   * [0x06] Version of the p2p protocol is not the same as ours
   */
  INCOMPATIBLE_PROTOCOL(0x04),

  /**
   * [0x08] Peer quit voluntarily
   */
  PEER_QUITING(0x05),

  LOCAL_IDENTITY(0x06),

  PING_TIMEOUT(0x07),

  TIME_OUT(0x08),

  BAD_TX(0x09),

  BAD_BLOCK(0x0A),

  FORKED(0x0B),

  UNLINKABLE(0x0C),

  INCOMPATIBLE_VERSION(0x0D),

  INCOMPATIBLE_CHAIN(0x0E),

  /**
   * [0xFF] Reason not specified
   */
  UNKNOWN(0xFF);

  private int reason;

  private static final Map<Integer, ReasonCode> intToTypeMap = new HashMap<>();

  static {
    for (ReasonCode type : ReasonCode.values()) {
      intToTypeMap.put(type.reason, type);
    }
  }

  private ReasonCode(int reason) {
    this.reason = reason;
  }

  public static ReasonCode fromInt(int i) {
    ReasonCode type = intToTypeMap.get(i);
    if (type == null)
      return ReasonCode.UNKNOWN;
    return type;
  }

  public int getReason() {
    return reason;
  }
}

