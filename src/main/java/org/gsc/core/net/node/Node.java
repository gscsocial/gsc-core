package org.gsc.core.net.node;

import org.gsc.common.overlay.message.Message;
import org.gsc.common.utils.Quitable;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.overlay.message.Message;
import org.gsc.common.utils.Quitable;
import org.gsc.common.utils.Sha256Hash;

public interface Node extends Quitable {

  void setNodeDelegate(NodeDelegate nodeDel);

  void broadcast(Message msg);

  void listen();

  void syncFrom(Sha256Hash myHeadBlockHash);

  void close() throws InterruptedException;
}
