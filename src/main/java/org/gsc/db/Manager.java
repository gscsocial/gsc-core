package org.gsc.db;

import static org.gsc.config.Parameter.ChainConstant.MAXIMUM_TIME_UNTIL_EXPIRATION;
import static org.gsc.config.Parameter.ChainConstant.MAX_TRANSACTION_PENDING;
import static org.gsc.config.Parameter.ChainConstant.SOLIDIFIED_THRESHOLD;
import static org.gsc.config.Parameter.ChainConstant.TRANSACTION_MAX_BYTE_SIZE;
import static org.gsc.config.Parameter.ChainConstant.WITNESS_PAY_PER_BLOCK;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javafx.util.Pair;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.gsc.common.exception.AccountResourceInsufficientException;
import org.gsc.common.exception.BadItemException;
import org.gsc.common.exception.BadNumberBlockException;
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
import org.gsc.common.exception.ValidateScheduleException;
import org.gsc.common.exception.ValidateSignatureException;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.DialogOptional;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.utils.StringUtil;
import org.gsc.config.Args;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.consensus.ProducerController;
import org.gsc.core.chain.BlockId;
import org.gsc.core.chain.TransactionResultWrapper;
import org.gsc.core.operator.Operator;
import org.gsc.core.operator.OperatorFactory;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.BytesWrapper;
import org.gsc.core.wrapper.ProducerWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.AbstractUndoStore.Dialog;
import org.joda.time.DateTime;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Getter
@Slf4j
@Component
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

  private UndoStore undoStore = UndoStore.getInstance();

  private DialogOptional dialog = DialogOptional.instance();

  @Autowired
  private ProducerController prodController;

  @Autowired
  private  Args config;

  @Getter
  private Cache<Sha256Hash, Boolean> transactionIdCache = CacheBuilder
      .newBuilder().maximumSize(100_000).recordStats().build();

  // transactions cache
  private List<TransactionWrapper> pendingTransactions;

  private ExecutorService validateSignService;

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

  public BlockId getHeadBlockId() {
    return globalPropertiesStore.getLatestBlockHeaderId();
  }

  public boolean lastHeadBlockIsMaintenance() {
    return getGlobalPropertiesStore().getStateFlag() == 1;
  }

  @PostConstruct
  public void init() {
    undoStore.disable();
    this.prodController.setManager(this);
    this.pendingTransactions = Collections.synchronizedList(Lists.newArrayList());
    this.initGenesis();
    try {
      this.forkDB.start(getBlockById(globalPropertiesStore.getLatestBlockHeaderHash()));
    } catch (ItemNotFoundException e) {
      logger.error(
          "Can not find Dynamic highest block from DB! \nnumber={} \nhash={}",
          globalPropertiesStore.getLatestBlockHeaderNumber(),
          globalPropertiesStore.getLatestBlockHeaderHash());
      logger.error(
          "Please delete database directory({}) and restart",
          config.getOutputDirectory());
      System.exit(1);
    } catch (BadItemException e) {
      e.printStackTrace();
      logger.error("DB data broken!");
      logger.error(
          "Please delete database directory({}) and restart",
          config.getOutputDirectory());
      System.exit(1);
    }
    undoStore.enable();

    validateSignService = Executors
        .newFixedThreadPool(config.getValidateSignThreadNum());
  }

  /**
   * init genesis block.
   */
  public void initGenesis() {

    //TODO
//    this.genesisBlock = BlockUtil.newGenesisBlockCapsule();
//    if (this.containBlock(this.genesisBlock.getBlockId())) {
//      config.setChainId(this.genesisBlock.getBlockId().toString());
//    } else {
//      if (this.hasBlocks()) {
//        logger.error(
//            "genesis block modify, please delete database directory({}) and restart",
//            config.getOutputDirectory());
//        System.exit(1);
//      } else {
//        logger.info("create genesis block");
//        config.setChainId(this.genesisBlock.getBlockId().toString());
//        // this.pushBlock(this.genesisBlock);
//        blockStore.put(this.genesisBlock.getBlockId().getBytes(), this.genesisBlock);
//        this.blockIndexStore.put(this.genesisBlock.getBlockId());
//
//        logger.info("save block: " + this.genesisBlock);
//        // init DynamicPropertiesStore
//        globalPropertiesStore.saveLatestBlockHeaderNumber(0);
//        globalPropertiesStore.saveLatestBlockHeaderHash(
//            this.genesisBlock.getBlockId().getByteString());
//        globalPropertiesStore.saveLatestBlockHeaderTimestamp(
//            this.genesisBlock.getTimeStamp());
//        this.initAccount();
//        this.initWitness();
//        this.witnessController.initWits();
//        forkDB.start(genesisBlock);
//        this.updateRecentBlock(genesisBlock);
//      }
//    }
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
    try {
      if (getTransactionStore().get(transactionCapsule.getTransactionId().getBytes()) != null) {
        logger.debug(ByteArray.toHexString(transactionCapsule.getTransactionId().getBytes()));
        throw new DupTransactionException("dup trans");
      }
    } catch (BadItemException e) {
      logger.debug(ByteArray.toHexString(transactionCapsule.getTransactionId().getBytes()));
      throw new DupTransactionException("dup trans");
    }
  }
  /**
   * push transaction into db.
   */
  public boolean pushTransactions(final TransactionWrapper trx)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      AccountResourceInsufficientException, DupTransactionException, TaposException,
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


  public void consumeBandwidth(TransactionWrapper trx)
      throws ContractValidateException, AccountResourceInsufficientException {
    //TODO
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

  private void applyBlock(BlockWrapper block) throws ContractValidateException,
      ContractExeException, ValidateSignatureException, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      TaposException, ValidateScheduleException {
    //TODO
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
            } catch (AccountResourceInsufficientException e) {
              logger.debug(e.getMessage(), e);
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
            } catch (ValidateScheduleException e) {
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
      UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException, TaposException, TooBigTransactionException, DupTransactionException, TransactionExpirationException, BadNumberBlockException {

    try (PendingManager pm = new PendingManager(this)) {

      if (!block.generatedByMyself) {
        if (!block.validateSignature()) {
          logger.info("The signature is not validated.");
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

      BlockWrapper newBlock = this.forkDB.push(block);

      // DB don't need lower block
      if (globalPropertiesStore.getLatestBlockHeaderHash() == null) {
        if (newBlock.getNum() != 0) {
          return;
        }
      } else {
        if (newBlock.getNum() <= globalPropertiesStore.getLatestBlockHeaderNumber()) {
          return;
        }

        // switch fork
        if (!newBlock
            .getParentHash()
            .equals(globalPropertiesStore.getLatestBlockHeaderHash())) {
          logger.warn(
              "switch fork! new head num = {}, blockid = {}",
              newBlock.getNum(),
              newBlock.getBlockId());

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


  public void updateDynamicProperties(BlockWrapper block) {
    long slot = 1;
    if (block.getNum() != 1) {
      slot = prodController.getSlotAtTime(block.getTimeStamp());
    }
    for (int i = 1; i < slot; ++i) {
      if (!prodController.getScheduledProducer(i).equals(block.getProducerAddress())) {
        ProducerWrapper prod =
            prodStore.get(StringUtil.createDbKey(prodController.getScheduledProducer(i)));
        prod.setTotalMissed(prod.getTotalMissed() + 1);
        this.prodStore.put(prod.createDbKey(), prod);
        logger.info(
            "{} miss a block. totalMissed = {}", prod.createReadableString(), prod.getTotalMissed());
      }
      globalPropertiesStore.applyBlock(false);
    }
    globalPropertiesStore.applyBlock(true);

    if (slot <= 0) {
      logger.warn("missedBlocks [" + slot + "] is illegal");
    }

    logger.info("update head, num = {}", block.getNum());
    globalPropertiesStore.saveLatestBlockHeaderHash(block.getBlockId().getByteString());

    globalPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
    globalPropertiesStore.saveLatestBlockHeaderTimestamp(block.getTimeStamp());

    undoStore.setMaxSize(
            (int)
                (globalPropertiesStore.getLatestBlockHeaderNumber()
                    - globalPropertiesStore.getLatestSolidifiedBlockNum()
                    + 1));
    forkDB.setMaxSize((int)
        (globalPropertiesStore.getLatestBlockHeaderNumber()
            - globalPropertiesStore.getLatestSolidifiedBlockNum()
            + 1));
  }

  /**
   * Get the fork branch.
   */
  public LinkedList<BlockId> getBlockChainHashesOnFork(final BlockId forkBlockHash) {
    final Pair<LinkedList<BlockWrapper>, LinkedList<BlockWrapper>> branch =
        this.forkDB.getBranch(
            globalPropertiesStore.getLatestBlockHeaderHash(), forkBlockHash);

    LinkedList<BlockWrapper> blockCapsules = branch.getValue();

    if (blockCapsules.isEmpty()) {
      logger.info("empty branch {}", forkBlockHash);
      return Lists.newLinkedList();
    }

    LinkedList<BlockId> result = blockCapsules.stream()
        .map(blockCapsule -> blockCapsule.getBlockId())
        .collect(Collectors.toCollection(LinkedList::new));

    result.add(blockCapsules.peekLast().getParentBlockId());

    return result;
  }

  /**
   * judge id.
   *
   * @param blockHash blockHash
   */
  public boolean containBlock(final Sha256Hash blockHash) {
    try {
      return this.forkDB.containBlock(blockHash)
          || blockStore.get(blockHash.getBytes()) != null;
    } catch (ItemNotFoundException e) {
      return false;
    } catch (BadItemException e) {
      return false;
    }
  }

  public boolean containBlockInMainChain(BlockId blockId) {
    try {
      return blockStore.get(blockId.getBytes()) != null;
    } catch (ItemNotFoundException e) {
      return false;
    } catch (BadItemException e) {
      return false;
    }
  }

  public void setBlockReference(TransactionWrapper trans) {
    byte[] headHash = globalPropertiesStore.getLatestBlockHeaderHash().getBytes();
    long headNum =globalPropertiesStore.getLatestBlockHeaderNumber();
    trans.setReference(headNum, headHash);
  }

  /**
   * Get a BlockWrapper by id.
   */
  public BlockWrapper getBlockById(final Sha256Hash hash)
      throws BadItemException, ItemNotFoundException {
    return this.forkDB.containBlock(hash)
        ? this.forkDB.getBlock(hash)
        : blockStore.get(hash.getBytes());
  }


  /**
   * judge has blocks.
   */
  public boolean hasBlocks() {
    return blockStore.dbSource.allKeys().size() > 0 || this.forkDB.hasData();
  }

  /**
   * Process transaction.
   */
  public boolean processTransaction(final TransactionWrapper trxCap)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      AccountResourceInsufficientException, TransactionExpirationException, TooBigTransactionException,
      DupTransactionException, TaposException {

    if (trxCap == null) {
      return false;
    }
    validateTapos(trxCap);
    validateCommon(trxCap);

    if (trxCap.getInstance().getRawData().getContract() == null) {
      throw new ContractValidateException("null tx");
    }

    validateDup(trxCap);

    if (!trxCap.validateSignature()) {
      throw new ValidateSignatureException("trans sig validate failed");
    }

    final Operator op = OperatorFactory.createActuator(trxCap, this);
    TransactionResultWrapper ret = new TransactionResultWrapper();

    consumeBandwidth(trxCap);

    op.validate();
    op.execute(ret);
    trxCap.setResult(ret);

    transactionStore.put(trxCap.getTransactionId().getBytes(), trxCap);
    return true;
  }

  /**
   * Get the block id from the number.
   */
  public BlockId getBlockIdByNum(final long num) throws ItemNotFoundException {
    return this.blockIndexStore.get(num);
  }

  public BlockWrapper getBlockByNum(final long num) throws ItemNotFoundException, BadItemException {
    return getBlockById(getBlockIdByNum(num));
  }

  /**
   * Generate a block.
   */
  public synchronized BlockWrapper generateBlock(
      final ProducerWrapper witnessCapsule, final long when, final byte[] privateKey)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException {

    final long timestamp = globalPropertiesStore.getLatestBlockHeaderTimestamp();
    final long number = globalPropertiesStore.getLatestBlockHeaderNumber();
    final Sha256Hash preHash = globalPropertiesStore.getLatestBlockHeaderHash();

    // judge create block time
    if (when < timestamp) {
      throw new IllegalArgumentException("generate block timestamp is invalid.");
    }

    long postponedTrxCount = 0;

    final BlockWrapper blockCapsule =
        new BlockWrapper(number + 1, preHash, when, witnessCapsule.getAddress());
    dialog.reset();
    dialog.setValue(undoStore.buildDialog());
    Iterator iterator = pendingTransactions.iterator();
    while (iterator.hasNext()) {
      TransactionWrapper trx = (TransactionWrapper) iterator.next();
      if (DateTime.now().getMillis() - when
          > ChainConstant.BLOCK_PRODUCED_INTERVAL * 0.5 * ChainConstant.BLOCK_PRODUCED_TIME_OUT) {
        logger.warn("Processing transaction time exceeds the 50% producing time。");
        break;
      }
      // check the block size
      if ((blockCapsule.getInstance().getSerializedSize() + trx.getSerializedSize() + 3) > ChainConstant.BLOCK_SIZE) {
        postponedTrxCount++;
        continue;
      }
      // apply transaction
      try (Dialog tmpDialog = undoStore.buildDialog()) {
        processTransaction(trx);
        tmpDialog.merge();
        // push into block
        blockCapsule.addTransaction(trx);
        iterator.remove();
      } catch (ContractExeException e) {
        logger.info("contract not processed during execute");
        logger.debug(e.getMessage(), e);
      } catch (ContractValidateException e) {
        logger.info("contract not processed during validate");
        logger.debug(e.getMessage(), e);
      } catch (RevokingStoreIllegalStateException e) {
        logger.info("contract not processed during RevokingStoreIllegalState");
        logger.debug(e.getMessage(), e);
      } catch (TaposException e) {
        logger.info("contract not processed during TaposException");
        logger.debug(e.getMessage(), e);
      } catch (DupTransactionException e) {
        logger.info("contract not processed during DupTransactionException");
        logger.debug(e.getMessage(), e);
      } catch (TooBigTransactionException e) {
        logger.info("contract not processed during TooBigTransactionException");
        logger.debug(e.getMessage(), e);
      } catch (TransactionExpirationException e) {
        logger.info("contract not processed during TransactionExpirationException");
        logger.debug(e.getMessage(), e);
      }
    }

    dialog.reset();

    if (postponedTrxCount > 0) {
      logger.info("{} transactions over the block size limit", postponedTrxCount);
    }

    logger.info(
        "postponedTrxCount[" + postponedTrxCount + "],TrxLeft[" + pendingTransactions.size()
            + "]");
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(privateKey);
    blockCapsule.generatedByMyself = true;
    try {
      this.pushBlock(blockCapsule);
      return blockCapsule;
    } catch (TaposException e) {
      logger.info("contract not processed during TaposException");
    } catch (TooBigTransactionException e) {
      logger.info("contract not processed during TooBigTransactionException");
    } catch (DupTransactionException e) {
      logger.info("contract not processed during DupTransactionException");
    } catch (TransactionExpirationException e) {
      logger.info("contract not processed during TransactionExpirationException");
    } catch (BadNumberBlockException e) {
      logger.info("generate block using wrong number");
    }

    return null;
  }
  
  /**
   * process block.
   */
  public void processBlock(BlockWrapper block)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      AccountResourceInsufficientException, TaposException, TooBigTransactionException,
      DupTransactionException, TransactionExpirationException, ValidateScheduleException {
    // todo set revoking db max size.

    // checkWitness
    if (!prodController.validateProducerSchedule(block)) {
      throw new ValidateScheduleException("validateWitnessSchedule error");
    }

    for (TransactionWrapper transactionCapsule : block.getTransactions()) {
      if (block.generatedByMyself) {
        transactionCapsule.setVerified(true);
      }
      processTransaction(transactionCapsule);
    }

    boolean needMaint = needMaintenance(block.getTimeStamp());
    if (needMaint) {
      if (block.getNum() == 1) {
         globalPropertiesStore.updateNextMaintenanceTime(block.getTimeStamp());
      } else {
        this.processMaintenance(block);
      }
    }
    this.updateDynamicProperties(block);
    this.updateSignedWitness(block);
    this.updateLatestSolidifiedBlock();
    this.updateTransHashCache(block);
    updateMaintenanceState(needMaint);
    //witnessController.updateWitnessSchedule();
    updateRecentBlock(block);
  }

  private void updateTransHashCache(BlockWrapper block) {
    for (TransactionWrapper transactionCapsule : block.getTransactions()) {
      this.transactionIdCache.put(transactionCapsule.getTransactionId(), true);
    }
  }

  public void updateRecentBlock(BlockWrapper block) {
    this.taposStore.put(ByteArray.subArray(
        ByteArray.fromLong(block.getNum()), 6, 8),
        new BytesWrapper(ByteArray.subArray(block.getBlockId().getBytes(), 8, 16)));
  }

  /**
   * update the latest solidified block.
   */
  public void updateLatestSolidifiedBlock() {
    List<Long> numbers =
        prodScheduleStore
            .getActiveProducers()
            .stream()
            .map(address -> prodController.getProdByAddress(address).getLatestBlockNum())
            .sorted()
            .collect(Collectors.toList());

    long size = prodScheduleStore.getActiveProducers().size();
    int solidifiedPosition = (int) (size * (1 - SOLIDIFIED_THRESHOLD));
    if (solidifiedPosition < 0) {
      logger.warn(
          "updateLatestSolidifiedBlock error, solidifiedPosition:{},wits.size:{}",
          solidifiedPosition,
          size);
      return;
    }
    long latestSolidifiedBlockNum = numbers.get(solidifiedPosition);
    //if current value is less than the previous value，keep the previous value.
    if (latestSolidifiedBlockNum < globalPropertiesStore.getLatestSolidifiedBlockNum()) {
      logger.warn("latestSolidifiedBlockNum = 0,LatestBlockNum:{}", numbers);
      return;
    }
    globalPropertiesStore.saveLatestSolidifiedBlockNum(latestSolidifiedBlockNum);
    logger.info("update solid block, num = {}", latestSolidifiedBlockNum);
  }

  public long getSyncBeginNumber() {
    logger.info("headNumber:" + globalPropertiesStore.getLatestBlockHeaderNumber());
    logger.info(
        "syncBeginNumber:"
            + (globalPropertiesStore.getLatestBlockHeaderNumber() - undoStore.size()));
    logger.info("solidBlockNumber:" + globalPropertiesStore.getLatestSolidifiedBlockNum());
    return globalPropertiesStore.getLatestBlockHeaderNumber() - undoStore.size();
  }

  /**
   * Determine if the current time is maintenance time.
   */
  public boolean needMaintenance(long blockTime) {
    return globalPropertiesStore.getNextMaintenanceTime() <= blockTime;
  }

  /**
   * Perform maintenance.
   */
  private void processMaintenance(BlockWrapper block) {
    prodController.updateProducer();
    globalPropertiesStore.updateNextMaintenanceTime(block.getTimeStamp());
  }

  /**
   * @param block the block update signed witness. set witness who signed block the 1. the latest
   * block num 2. pay the trx to witness. 3. the latest slot num.
   */
  public void updateSignedWitness(BlockWrapper block) {
    // TODO: add verification
    ProducerWrapper witnessCapsule =
        prodStore.get(
            block.getInstance().getBlockHeader().getRawData().getProducerAddress().toByteArray());
    witnessCapsule.setTotalProduced(witnessCapsule.getTotalProduced() + 1);
    witnessCapsule.setLatestBlockNum(block.getNum());
    witnessCapsule.setLatestSlotNum(prodController.getAbSlotAtTime(block.getTimeStamp()));

    // Update memory witness status
    ProducerWrapper wit = prodController.getProdByAddress(block.getProducerAddress());
    if (wit != null) {
      wit.setTotalProduced(witnessCapsule.getTotalProduced() + 1);
      wit.setLatestBlockNum(block.getNum());
      wit.setLatestSlotNum(prodController.getAbSlotAtTime(block.getTimeStamp()));
    }

    prodStore.put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);

    AccountWrapper photon = accountStore.getPhoton();
    try {
      adjustBalance(photon.getAddress().toByteArray(), - WITNESS_PAY_PER_BLOCK);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
    }
    try {
      adjustAllowance(witnessCapsule.getAddress().toByteArray(), WITNESS_PAY_PER_BLOCK);
    } catch (BalanceInsufficientException e) {
      logger.debug(e.getMessage(), e);
    }

    logger.debug(
        "updateSignedWitness. witness address:{}, blockNum:{}, totalProduced:{}",
        witnessCapsule.createReadableString(),
        block.getNum(),
        witnessCapsule.getTotalProduced());
  }

  public void updateMaintenanceState(boolean needMaint) {
    if (needMaint) {
      globalPropertiesStore.saveStateFlag(1);
    } else {
      globalPropertiesStore.saveStateFlag(0);
    }
  }

  // To be added
  public long getSkipSlotInMaintenance() {
    return globalPropertiesStore.getMaintenanceSkipSlots();
  }

  public AssetIssueStore getAssetIssueStore() {
    return assetIssueStore;
  }

  public void setAssetIssueStore(AssetIssueStore assetIssueStore) {
    this.assetIssueStore = assetIssueStore;
  }

  public void setBlockIndexStore(BlockIndexStore indexStore) {
    this.blockIndexStore = indexStore;
  }


  public void closeAllStore() {
    System.err.println("******** begin to close store ********");
    closeOneStore(accountStore);
    closeOneStore(blockStore);
    closeOneStore(blockIndexStore);

    closeOneStore(prodStore);
    closeOneStore(prodScheduleStore);
    closeOneStore(assetIssueStore);
    closeOneStore(globalPropertiesStore);
    closeOneStore(transactionStore);
    System.err.println("******** end to close store ********");
  }

  private void closeOneStore(Store database) {
    System.err.println("******** begin to close " + database.getName() + " ********");
    try {
      database.close();
    } catch (Exception e) {
      System.err.println("failed to close  " + database.getName() + ". " + e);
    } finally {
      System.err.println("******** end to close " + database.getName() + " ********");
    }
  }

  public boolean isTooManyPending() {
    if (getPendingTransactions().size() + PendingManager.getTmpTransactions().size()
        > MAX_TRANSACTION_PENDING) {
      return true;
    }
    return false;
  }

  public boolean isGeneratingBlock() {
    if (config.isProd()) {
      return prodController.isGeneratingBlock();
    }
    return false;
  }

  private static class ValidateSignTask implements Callable<Boolean> {

    private TransactionWrapper trx;
    private CountDownLatch countDownLatch;

    ValidateSignTask(TransactionWrapper trx, CountDownLatch countDownLatch) {
      this.trx = trx;
      this.countDownLatch = countDownLatch;
    }

    @Override
    public Boolean call() throws ValidateSignatureException {
      try {
        trx.validateSignature();
      } catch (ValidateSignatureException e) {
        throw e;
      } finally {
        countDownLatch.countDown();
      }
      return true;
    }
  }

  public synchronized void preValidateTransactionSign(BlockWrapper block)
      throws InterruptedException, ValidateSignatureException {
    logger.info("PreValidate Transaction Sign, size:" + block.getTransactions().size()
        + ",block num:" + block.getNum());
    int transSize = block.getTransactions().size();
    CountDownLatch countDownLatch = new CountDownLatch(transSize);
    List<Future<Boolean>> futures = new ArrayList<>(transSize);

    for (TransactionWrapper transaction : block.getTransactions()) {
      Future<Boolean> future = validateSignService
          .submit(new ValidateSignTask(transaction, countDownLatch));
      futures.add(future);
    }
    countDownLatch.await();

    for (Future<Boolean> future : futures) {
      try {
        future.get();
      } catch (ExecutionException e) {
        throw new ValidateSignatureException(e.getCause().getMessage());
      }
    }
  }
}
