package org.gsc.core.sync;

import java.util.Deque;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;

public class PendingStatus {
  Deque<TransactionWrapper> pedingTxs;

  BlockWrapper pedingBlock;
}
