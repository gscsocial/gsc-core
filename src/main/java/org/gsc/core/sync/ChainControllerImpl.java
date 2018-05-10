package org.gsc.core.sync;

import org.gsc.common.exception.HeaderNotFound;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.chain.BlockId;
import org.gsc.core.chain.BlockWrapper;
import org.gsc.core.chain.TransactionWrapper;

public class ChainControllerImpl implements ChainController {

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
