package org.gsc.core.sync;

import org.gsc.net.message.gsc.GscMessage;

public class SyncManager {


  public void onConnectPeer(PeerConnection peer) {

  }


  public void onDisconnectPeer(PeerConnection peer) {

  }

  public void onMessage(PeerConnection peer, GscMessage msg) {
    switch (msg.getType()) {
      case BLOCK:
        //TODO: handle block msg
        break;
      case TRANSACTION:
        //TODO: handle tx msg
        break;
        default:
        throw new IllegalArgumentException("No such message");
    }
  }


  }
