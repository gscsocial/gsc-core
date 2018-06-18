package org.gsc.core.sync;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.protos.Protocol.Inventory.InventoryType;

class InvToSend {
  private HashMap<PeerConnection, HashMap<InventoryType, LinkedList<Sha256Hash>>> send
      = new HashMap<>();

  public void clear() {
    this.send.clear();
  }

  public void add(Entry<Sha256Hash, InventoryType> id, PeerConnection peer) {
    if (send.containsKey(peer) && send.get(peer).containsKey(id.getValue())) {
      send.get(peer).get(id.getValue()).offer(id.getKey());
    } else if (send.containsKey(peer)) {
      send.get(peer).put(id.getValue(), new LinkedList<>());
      send.get(peer).get(id.getValue()).offer(id.getKey());
    } else {
      send.put(peer, new HashMap<>());
      send.get(peer).put(id.getValue(), new LinkedList<>());
      send.get(peer).get(id.getValue()).offer(id.getKey());
    }
  }

  public void add(PriorItem id, PeerConnection peer) {
    if (send.containsKey(peer) && send.get(peer).containsKey(id.getType())) {
      send.get(peer).get(id.getType()).offer(id.getHash());
    } else if (send.containsKey(peer)) {
      send.get(peer).put(id.getType(), new LinkedList<>());
      send.get(peer).get(id.getType()).offer(id.getHash());
    } else {
      send.put(peer, new HashMap<>());
      send.get(peer).put(id.getType(), new LinkedList<>());
      send.get(peer).get(id.getType()).offer(id.getHash());
    }
  }
}