package org.gsc.db;

import static org.gsc.config.Parameter.ChainConstant.MAXIMUM_TIME_UNTIL_EXPIRATION;
import static org.gsc.config.Parameter.ChainConstant.TRANSACTION_MAX_BYTE_SIZE;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javafx.util.Pair;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.gsc.common.exception.BadItemException;
import org.gsc.common.exception.BalanceInsufficientException;
import org.gsc.common.exception.ContractExeException;
import org.gsc.common.exception.ContractValidateException;
import org.gsc.common.exception.DupTransactionException;
import org.gsc.common.exception.ItemNotFoundException;
import org.gsc.common.exception.RevokingStoreIllegalStateException;
import org.gsc.common.exception.TaposException;
import org.gsc.common.exception.TooBigTransactionException;
import org.gsc.common.exception.TransactionExpirationException;
import org.gsc.common.exception.UnLinkedBlockException;
import org.gsc.common.exception.ValidateBandwidthException;
import org.gsc.common.exception.ValidateScheduleException;
import org.gsc.common.exception.ValidateSignatureException;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.DialogOptional;
import org.gsc.consensus.ProducerController;
import org.gsc.core.chain.BlockId;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.BytesWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.UndoStore.Dialog;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@Slf4j
public class Manager {

  @Autowired
  private AccountStore accountStore;

  @Autowired
  private TransactionStore transactionStore;

  @Autowired
  private BlockStore blockStore;

  @Autowired
  private ProducerStore prodStore;

  @Autowired
  private AssetIssueStore assetIssueStore;

  @Autowired
  private GlobalPropertiesStore globalPropertiesStore;

  @Autowired
  private BlockIndexStore blockIndexStore;
//  @Autowired
//  private AccountIndexStore accountIndexStore;
  @Autowired
  private ProducerScheduleStore prodScheduleStore;

  @Autowired
  private TaposBlockStore taposStore;

  @Autowired
  private VotesStore votesStore;
//
  @Autowired
  private PeersStore peersStore;

  @Autowired
  private ForkDatabase forkDB;

  @Getter
  private BlockWrapper genesisBlock;

  @Autowired
  private UndoStore undoStore;

  @Autowired
  private DialogOptional dialog;

  @Autowired
  private ProducerController prodController;

  // transactions cache
  private List<TransactionWrapper> pendingTransactions;

  // transactions popped
  @Getter
  private List<TransactionWrapper> popedTransactions =
      Collections.synchronizedList(Lists.newArrayList());

  /**
   * judge balance.
   */
  public void adjustBalance(byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    AccountWrapper account = getAccountStore().get(accountAddress);
    long balance = account.getBalance();
    if (amount == 0) {
      return;
    }

    if (amount < 0 && balance < -amount) {
      throw new BalanceInsufficientException(accountAddress + " Insufficient");
    }
    account.setBalance(Math.addExact(balance, amount));
    this.getAccountStore().put(account.getAddress().toByteArray(), account);
  }

  public long getHeadBlockTimeStamp() {
    return 0L;
    //TODO
    //return getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
  }

  public boolean lastHeadBlockIsMaintenance() {
    return getGlobalPropertiesStore().getStateFlag() == 1;
  }

  public void adjustAllowance(byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    AccountWrapper account = getAccountStore().get(accountAddress);
    long allowance = account.getAllowance();
    if (amount == 0) {
      return;
    }

    if (amount < 0 && allowance < -amount) {
      throw new BalanceInsufficientException(accountAddress + " Insufficient");
    }
    account.setAllowance(allowance + amount);
    this.getAccountStore().put(account.createDbKey(), account);
  }

  void validateTapos(TransactionWrapper transactionCapsule) throws TaposException {
    byte[] refBlockHash = transactionCapsule.getInstance()
        .getRawData().getRefBlockHash().toByteArray();
    byte[] refBlockNumBytes = transactionCapsule.getInstance()
        .getRawData().getRefBlockBytes().toByteArray();
    try {
      byte[] blockHash = this.taposStore.get(refBlockNumBytes).getData();
      if (Arrays.equals(blockHash, refBlockHash)) {
        return;
      } else {
        String str = String.format(
            "Tapos failed, different block hash, %s, %s , recent block %s, solid block %s head block %s",
            ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
            Hex.toHexString(blockHash),
            getSolidBlockId(),
            globalPropertiesStore.getLatestBlockHeaderHash());
        logger.info(str);
        throw new TaposException(str);

      }
    } catch (ItemNotFoundException e) {
      String str = String.
          format("Tapos failed, block not found, ref block %s, %s , solid block %s head block %s",
              ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
              getSolidBlockId(),
              globalPropertiesStore.getLatestBlockHeaderHash()).toString();
      logger.info(str);
      throw new TaposException(str);
    }
  }

  public BlockId getSolidBlockId() {
    try {
      long num = globalPropertiesStore.getLatestSolidifiedBlockNum();
      return getBlockIdByNum(num);
    } catch (Exception e) {
      return getGenesisBlockId();
    }
  }

  public BlockId getBlockIdByNum(final long num) throws ItemNotFoundException {
    return this.blockIndexStore.get(num);
  }

  public BlockId getGenesisBlockId() {
    return this.genesisBlock.getBlockId();
  }


  void validateCommon(TransactionWrapper transactionCapsule)
      throws TransactionExpirationException, TooBigTransactionException {
    if (transactionCapsule.getData().length > TRANSACTION_MAX_BYTE_SIZE) {
      throw new TooBigTransactionException(
          "too big transaction, the size is " + transactionCapsule.getData().length + " bytes");
    }
    long transactionExpiration = transactionCapsule.getExpiration();
    long headBlockTime = getHeadBlockTimeStamp();
    if (transactionExpiration <= headBlockTime ||
        transactionExpiration > headBlockTime + MAXIMUM_TIME_UNTIL_EXPIRATION) {
      throw new TransactionExpirationException(
          "transaction expiration, transaction expiration time is " + transactionExpiration
              + ", but headBlockTime is " + headBlockTime);
    }
  }

  void validateDup(TransactionWrapper transactionCapsule) throws DupTransactionException {
    if (getTransactionStore().get(transactionCapsule.getTransactionId().getBytes()) != null) {
      logger.debug(
          getTransactionStore().get(transactionCapsule.getTransactionId().getBytes()).toString());
      throw new DupTransactionException("dup trans");
    }
  }

  /**
   * push transaction into db.
   */
  public boolean pushTransactions(final TransactionWrapper trx)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      ValidateBandwidthException, DupTransactionException, TaposException,
      TooBigTransactionException, TransactionExpirationException {
    logger.info("push transaction");

    if (!trx.validateSignature()) {
      throw new ValidateSignatureException("trans sig validate failed");
    }

    //validateFreq(trx);
    synchronized (this) {
      if (!dialog.valid()) {
        dialog.setValue(undoStore.buildDialog());
      }

      try (Dialog tmpDialog = undoStore.buildDialog()) {
        processTransaction(trx);
        pendingTransactions.add(trx);
        tmpDialog.merge();
      } catch (RevokingStoreIllegalStateException e) {
        logger.debug(e.getMessage(), e);
      }
    }
    return true;
  }

  private void processTransaction(TransactionWrapper trx) {
    //TODO
  }

  public void consumeBandwidth(TransactionWrapper trx) throws ValidateBandwidthException {
//    BandwidthProcessor processor = new BandwidthProcessor(this);
//    processor.consumeBandwidth(trx);
  }
  /**
   * when switch fork need erase blocks on fork branch.
   */
  public void eraseBlock() throws BadItemException, ItemNotFoundException {
    dialog.reset();
    BlockWrapper oldHeadBlock =
        getBlockStore().get(globalPropertiesStore.getLatestBlockHeaderHash().getBytes());
    try {
      undoStore.pop();
    } catch (RevokingStoreIllegalStateException e) {
      logger.info(e.getMessage(), e);
    }
    logger.info("erase block:" + oldHeadBlock);
    forkDB.pop();
    popedTransactions.addAll(oldHeadBlock.getTransactions());
  }

  private void applyBlock(BlockWrapper block)
      throws ContractValidateException, ContractExeException, ValidateSignatureException, ValidateBandwidthException, TransactionExpirationException, TooBigTransactionException, DupTransactionException, TaposException {
//    processBlock(block);
//    this.blockStore.put(block.getBlockId().getBytes(), block);
//    this.blockIndexStore.put(block.getBlockId());
  }

  private void switchFork(BlockWrapper newHead) {
    Pair<LinkedList<BlockWrapper>, LinkedList<BlockWrapper>> binaryTree =
        forkDB.getBranch(
            newHead.getBlockId(), globalPropertiesStore.getLatestBlockHeaderHash());

    if (CollectionUtils.isNotEmpty(binaryTree.getValue())) {
      while (!globalPropertiesStore
          .getLatestBlockHeaderHash()
          .equals(binaryTree.getValue().peekLast().getParentHash())) {
        try {
          eraseBlock();
        } catch (BadItemException e) {
          logger.info(e.getMessage());
        } catch (ItemNotFoundException e) {
          logger.info(e.getMessage());
        }
      }
    }

    if (CollectionUtils.isNotEmpty(binaryTree.getKey())) {
      LinkedList<BlockWrapper> branch = binaryTree.getKey();
      Collections.reverse(branch);
      branch.forEach(
          item -> {
            // todo  process the exception carefully later
            try (Dialog tmpDialog = undoStore.buildDialog()) {
              applyBlock(item);
              tmpDialog.commit();
            } catch (ValidateBandwidthException e) {
              logger.debug("high freq", e);
            } catch (ValidateSignatureException e) {
              logger.debug(e.getMessage(), e);
            } catch (ContractValidateException e) {
              logger.debug(e.getMessage(), e);
            } catch (ContractExeException e) {
              logger.debug(e.getMessage(), e);
            } catch (RevokingStoreIllegalStateException e) {
              logger.debug(e.getMessage(), e);
            } catch (TaposException e) {
              logger.debug(e.getMessage(), e);
            } catch (DupTransactionException e) {
              logger.debug(e.getMessage(), e);
            } catch (TooBigTransactionException e) {
              logger.debug(e.getMessage(), e);
            } catch (TransactionExpirationException e) {
              logger.debug(e.getMessage(), e);
            }
          });
      return;
    }
  }

  // TODO: if error need to rollback.

  private synchronized void filterPendingTrx(List<TransactionWrapper> listTrx) {
  }

  /**
   * save a block.
   */
  public synchronized void pushBlock(final BlockWrapper block)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, ValidateBandwidthException, TaposException, TooBigTransactionException, DupTransactionException, TransactionExpirationException, BadNumberBlockException {

    try (PendingManager pm = new PendingManager(this)) {

      if (!block.generatedByMyself) {
        if (!block.validateSignature()) {
          logger.info("The siganature is not validated.");
          // TODO: throw exception here.
          return;
        }

        if (!block.calcMerkleRoot().equals(block.getMerkleRoot())) {
          logger.info(
              "The merkler root doesn't match, Calc result is "
                  + block.calcMerkleRoot()
                  + " , the headers is "
                  + block.getMerkleRoot());
          // TODO:throw exception here.
          return;
        }
      }

      // checkWitness
      if (!prodController.validateProducerSchedule(block)) {
        throw new ValidateScheduleException("validateWitnessSchedule error");
      }

      BlockWrapper newBlock = forkDB.push(block);

      // DB don't need lower block
      if (globalPropertiesStore.getLatestBlockHeaderHash() == null) {
        if (newBlock.getNum() != 0) {
          return;
        }
      } else {
        if (newBlock.getNum() <= globalPropertiesStore.getLatestBlockHeaderNumber()) {
          return;
        }

        if (newBlock.getTimeStamp() <= globalPropertiesStore
            .getLatestBlockHeaderTimestamp()) {
          return;
        }

        // switch fork
        if (!newBlock
            .getParentHash()
            .equals(globalPropertiesStore.getLatestBlockHeaderHash())) {
          switchFork(newBlock);
          return;
        }
        try (Dialog tmpDialog = undoStore.buildDialog()) {
          applyBlock(newBlock);
          tmpDialog.commit();
        } catch (RevokingStoreIllegalStateException e) {
          logger.error(e.getMessage(), e);
        } catch (Throwable throwable) {
          logger.error(throwable.getMessage(), throwable);
          forkDB.removeBlk(block.getBlockId());
          throw throwable;
        }
      }
      logger.info("save block: " + newBlock);
    }
  }
}
