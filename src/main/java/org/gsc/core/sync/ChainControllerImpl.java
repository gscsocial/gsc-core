package org.gsc.core.sync;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.gsc.common.exception.BadBlockException;
import org.gsc.common.exception.BadTransactionException;
import org.gsc.common.exception.GscException;
import org.gsc.common.exception.HeaderNotFound;
import org.gsc.common.exception.NonCommonBlockException;
import org.gsc.common.exception.StoreException;
import org.gsc.common.exception.UnLinkedBlockException;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.chain.BlockId;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.net.message.MessageTypes;
import org.gsc.net.message.gsc.GscMessage;
import org.springframework.stereotype.Component;

@Component
public class ChainControllerImpl implements ChainController {

  @Override
  public LinkedList<Sha256Hash> handleBlock(BlockWrapper block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException, InterruptedException, NonCommonBlockException {
    return null;
  }

  @Override
  public boolean handleTransaction(TransactionWrapper trx) throws BadTransactionException {
    return false;
  }

  @Override
  public LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary)
      throws StoreException {
    return null;
  }

  @Override
  public Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, Deque<BlockId> blockIds)
      throws GscException {
    return null;
  }

  @Override
  public GscMessage getData(Sha256Hash msgId, MessageTypes type) {
    return null;
  }

  @Override
  public void syncToCli(long unSyncNum) {

  }

  @Override
  public long getBlockTime(BlockId id) {
    return 0;
  }

  @Override
  public BlockId getSolidBlockId() {
    return null;
  }

  @Override
  public boolean contain(Sha256Hash hash, MessageTypes type) {
    return false;
  }

  @Override
  public boolean containBlockInMainChain(BlockId id) {
    return false;
  }

  @Override
  public boolean canChainRevoke(long num) {
    return false;
  }

  @Override
  public BlockWrapper getHead() throws HeaderNotFound {
    return null;
  }

  @Override
  public BlockId getHeadBlockId() {
    return null;
  }

  @Override
  public long getHeadBlockTimeStamp() {
    return 0;
  }

  @Override
  public BlockId getGenesisBlockId() {
    return null;
  }

  @Override
  public BlockWrapper getGenesisBlock() {
    return null;
  }

  @Override
  public void initGenesis() {

  }

  @Override
  public void initAccount() {

  }

  @Override
  public void initWitness() {

  }

  @Override
  public boolean pushTransactions(TransactionWrapper tx) {
    return false;
  }

  @Override
  public boolean pushBlock(BlockWrapper block) {
    return false;
  }

  @Override
  public boolean containBlock(BlockId id) {
    return false;
  }

  @Override
  public BlockWrapper getBlockById(BlockId id) {
    return null;
  }

  @Override
  public TransactionWrapper getTransactionById(Sha256Hash id) {
    return null;
  }
}
