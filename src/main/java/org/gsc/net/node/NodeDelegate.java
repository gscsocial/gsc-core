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

  LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary) throws StoreException;

  Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, Deque<BlockId> blockIds)
          throws GSCException;

  Message getData(Sha256Hash msgId, MessageTypes type);

  void syncToCli(long unSyncNum);

  long getBlockTime(BlockId id);

  BlockId getHeadBlockId();

  BlockId getSolidBlockId();

  boolean contain(Sha256Hash hash, MessageTypes type);

  boolean containBlock(BlockId id);

  long getHeadBlockTimeStamp();

  boolean containBlockInMainChain(BlockId id);

  BlockWrapper getGenesisBlock();

  boolean canChainRevoke(long num);
}
