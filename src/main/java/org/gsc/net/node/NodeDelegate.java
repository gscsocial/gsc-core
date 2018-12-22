package org.gsc.net.node;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.gsc.common.overlay.message.Message;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.core.exception.BadBlockException;
import org.gsc.core.exception.BadTransactionException;
import org.gsc.core.exception.NonCommonBlockException;
import org.gsc.core.exception.StoreException;
import org.gsc.core.exception.GSCException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.net.message.MessageTypes;

public interface NodeDelegate {

  LinkedList<Sha256Hash> handleBlock(BlockWrapper block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException, InterruptedException, NonCommonBlockException;

  boolean handleTransaction(TransactionWrapper trx) throws BadTransactionException;

  Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, Deque<BlockId> blockIds)
      throws GSCException;

  LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary) throws StoreException;
  
  Message getData(Sha256Hash msgId, MessageTypes type);

  long getBlockTime(BlockId id);
  
  void syncToCli(long unSyncNum);

  BlockId getSolidBlockId();

  BlockId getHeadBlockId();
  
  boolean contain(Sha256Hash hash, MessageTypes type);

  boolean containBlock(BlockId id);

  long getHeadBlockTimeStamp();

  boolean containBlockInMainChain(BlockId id);

  BlockWrapper getGenesisBlock();

  boolean canChainRevoke(long num);
}
