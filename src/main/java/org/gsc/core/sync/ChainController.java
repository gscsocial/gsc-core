package org.gsc.core.sync;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.gsc.common.exception.BadBlockException;
import org.gsc.common.exception.BadTransactionException;
import org.gsc.common.exception.GscException;
import org.gsc.common.exception.HeaderNotFound;
import org.gsc.common.exception.StoreException;
import org.gsc.common.exception.UnLinkedBlockException;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.chain.BlockId;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.net.message.MessageTypes;
import org.gsc.net.message.gsc.GscMessage;

public interface ChainController {

  LinkedList<Sha256Hash> handleBlock(BlockWrapper block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException, InterruptedException;

  boolean handleTransaction(TransactionWrapper trx) throws BadTransactionException;

  LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary) throws StoreException;

  Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, Deque<BlockId> blockIds)
      throws GscException;

  GscMessage getData(Sha256Hash msgId, MessageTypes type);

  void syncToCli(long unSyncNum);

  long getBlockTime(BlockId id);


  BlockId getSolidBlockId();

  boolean contain(Sha256Hash hash, MessageTypes type);

  boolean containBlockInMainChain(BlockId id);

  boolean canChainRevoke(long num);

  BlockWrapper getHead() throws HeaderNotFound ;

  BlockId getHeadBlockId();

  long getHeadBlockTimeStamp();

  BlockId getGenesisBlockId();

  BlockWrapper getGenesisBlock();

  void initGenesis();

  void initAccount();

  void initWitness();

  boolean pushTransactions(TransactionWrapper tx);

  boolean pushBlock(final BlockWrapper block);

  boolean containBlock(BlockId id);

  BlockWrapper getBlockById(final BlockId id);

  TransactionWrapper getTransactionById(final Sha256Hash id);
}
