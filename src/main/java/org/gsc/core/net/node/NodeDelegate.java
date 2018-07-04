package org.gsc.core.net.node;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.gsc.common.overlay.message.Message;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.capsule.BlockCapsule;
import org.gsc.core.capsule.BlockCapsule.BlockId;
import org.gsc.core.capsule.TransactionCapsule;
import org.gsc.core.exception.BadBlockException;
import org.gsc.core.exception.BadTransactionException;
import org.gsc.core.exception.NonCommonBlockException;
import org.gsc.core.exception.StoreException;
import org.gsc.core.exception.GscException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.core.net.message.MessageTypes;
import org.gsc.common.overlay.message.Message;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.capsule.BlockCapsule;
import org.gsc.core.capsule.BlockCapsule.BlockId;
import org.gsc.core.capsule.TransactionCapsule;
import org.gsc.core.exception.BadBlockException;
import org.gsc.core.exception.BadTransactionException;
import org.gsc.core.exception.NonCommonBlockException;
import org.gsc.core.exception.StoreException;
import org.gsc.core.exception.GscException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.core.net.message.MessageTypes;
import sun.security.krb5.internal.crypto.Nonce;

public interface NodeDelegate {

  LinkedList<Sha256Hash> handleBlock(BlockCapsule block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException, InterruptedException, NonCommonBlockException;

  boolean handleTransaction(TransactionCapsule trx) throws BadTransactionException;

  LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary) throws StoreException;

  Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, Deque<BlockId> blockIds)
      throws GscException;

  Message getData(Sha256Hash msgId, MessageTypes type);

  void syncToCli(long unSyncNum);

  long getBlockTime(BlockId id);

  BlockId getHeadBlockId();

  BlockId getSolidBlockId();

  boolean contain(Sha256Hash hash, MessageTypes type);

  boolean containBlock(BlockId id);

  long getHeadBlockTimeStamp();

  boolean containBlockInMainChain(BlockId id);

  BlockCapsule getGenesisBlock();

  boolean canChainRevoke(long num);
}
