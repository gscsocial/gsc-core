package org.gsc.core.sync;

import org.gsc.common.exception.HeaderNotFound;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.chain.BlockId;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;

public interface ChainController {

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
