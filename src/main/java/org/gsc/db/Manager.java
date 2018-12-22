package org.gsc.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.gsc.common.overlay.discover.node.Node;
import org.gsc.common.storage.DepositImpl;
import org.gsc.common.utils.*;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.config.args.GenesisBlock;
import org.gsc.core.Constant;
import org.gsc.core.db2.core.IGSCChainBase;
import org.gsc.core.db2.core.ISession;
import org.gsc.core.exception.*;
import org.gsc.core.witness.ProposalController;
import org.gsc.core.witness.WitnessController;
import org.gsc.core.wrapper.*;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.core.wrapper.utils.BlockUtil;
import org.gsc.db.KhaosDatabase.KhaosBlock;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.runtime.Runtime;
import org.gsc.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.joda.time.DateTime;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.gsc.config.Parameter.ChainConstant.SOLIDIFIED_THRESHOLD;
import static org.gsc.config.Parameter.NodeConstant.MAX_TRANSACTION_PENDING;
import static org.gsc.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.gsc.protos.Protocol.Transaction.Contract.ContractType.TransferContract;


@Slf4j
@Component
public class Manager {

  // db store
  @Autowired
  private AccountStore accountStore;
  @Autowired
  private TransactionStore transactionStore;
  @Autowired
  private BlockStore blockStore;
  @Autowired
  private WitnessStore witnessStore;
  @Autowired
  private AssetIssueStore assetIssueStore;
  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;
  @Autowired
  private BlockIndexStore blockIndexStore;
  @Autowired
  private AccountIdIndexStore accountIdIndexStore;
  @Autowired
  private WitnessScheduleStore witnessScheduleStore;
  @Autowired
  private RecentBlockStore recentBlockStore;
  @Autowired
  private VotesStore votesStore;
  @Autowired
  private ProposalStore proposalStore;
  @Autowired
  private ExchangeStore exchangeStore;
  @Autowired
  private TransactionHistoryStore transactionHistoryStore;
  @Autowired
  private CodeStore codeStore;
  @Autowired
  private ContractStore contractStore;
  @Autowired
  @Getter
  private StorageRowStore storageRowStore;

  // for network
  @Autowired
  private PeersStore peersStore;


  @Autowired
  private KhaosDatabase khaosDb;


  private BlockWrapper genesisBlock;
  @Getter
  @Autowired
  private RevokingDatabase revokingStore;

  @Getter
  private SessionOptional session = SessionOptional.instance();

  @Getter
  @Setter
  private boolean isSyncMode;

  @Getter
  @Setter
  private String netType;

  @Getter
  @Setter
  private WitnessController witnessController;

  @Getter
  @Setter
  private ProposalController proposalController;

  private ExecutorService validateSignService;

  private Thread repushThread;

  private boolean isRunRepushThread = true;

  @Getter
  private Cache<Sha256Hash, Boolean> transactionIdCache = CacheBuilder
      .newBuilder().maximumSize(100_000).recordStats().build();

  @Getter
  @Autowired
  private ForkController forkController;

  public WitnessStore getWitnessStore() {
    return this.witnessStore;
  }

  private void setWitnessStore(final WitnessStore witnessStore) {
    this.witnessStore = witnessStore;
  }

  public DynamicPropertiesStore getDynamicPropertiesStore() {
    return this.dynamicPropertiesStore;
  }

  public void setDynamicPropertiesStore(final DynamicPropertiesStore dynamicPropertiesStore) {
    this.dynamicPropertiesStore = dynamicPropertiesStore;
  }

  public WitnessScheduleStore getWitnessScheduleStore() {
    return this.witnessScheduleStore;
  }

  public void setWitnessScheduleStore(final WitnessScheduleStore witnessScheduleStore) {
    this.witnessScheduleStore = witnessScheduleStore;
  }

  public CodeStore getCodeStore() {
    return codeStore;
  }

  public ContractStore getContractStore() {
    return contractStore;
  }

  public VotesStore getVotesStore() {
    return this.votesStore;
  }

  public ProposalStore getProposalStore() {
    return this.proposalStore;
  }

  public ExchangeStore getExchangeStore() {
    return this.exchangeStore;
  }

  public List<TransactionWrapper> getPendingTransactions() {
    return this.pendingTransactions;
  }

  public List<TransactionWrapper> getPoppedTransactions() {
    return this.popedTransactions;
  }

  public BlockingQueue<TransactionWrapper> getRepushTransactions() {
    return repushTransactions;
  }

  // transactions cache
  private List<TransactionWrapper> pendingTransactions;

  // transactions popped
  private List<TransactionWrapper> popedTransactions =
      Collections.synchronizedList(Lists.newArrayList());

  // the capacity is equal to Integer.MAX_VALUE default
  private BlockingQueue<TransactionWrapper> repushTransactions;

  // for test only
  public List<ByteString> getWitnesses() {
    return witnessController.getActiveWitnesses();
  }

  // for test only
  public void addWitness(final ByteString address) {
    List<ByteString> witnessAddresses = witnessController.getActiveWitnesses();
    witnessAddresses.add(address);
    witnessController.setActiveWitnesses(witnessAddresses);
  }

  public BlockWrapper getHead() throws HeaderNotFound {
    List<BlockWrapper> blocks = getBlockStore().getBlockByLatestNum(1);
    if (CollectionUtils.isNotEmpty(blocks)) {
      return blocks.get(0);
    } else {
      logger.info("Header block Not Found");
      throw new HeaderNotFound("Header block Not Found");
    }
  }

  public synchronized BlockId getHeadBlockId() {
    return new BlockId(
        getDynamicPropertiesStore().getLatestBlockHeaderHash(),
        getDynamicPropertiesStore().getLatestBlockHeaderNumber());
  }

  public long getHeadBlockNum() {
    return getDynamicPropertiesStore().getLatestBlockHeaderNumber();
  }

  public long getHeadBlockTimeStamp() {
    return getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
  }

//  public PeersStore getPeersStore() {
//    return peersStore;
//  }
//
//  public void setPeersStore(PeersStore peersStore) {
//    this.peersStore = peersStore;
//  }
//
//  public Node getHomeNode() {
//    final Args args = Args.getInstance();
//    Set<Node> nodes = this.peersStore.get("home".getBytes());
//    if (nodes.size() > 0) {
//      return nodes.stream().findFirst().get();
//    } else {
//      Node node =
//          new Node(new ECKey().getNodeId(), args.getNodeExternalIp(), args.getNodeListenPort());
//      nodes.add(node);
//      this.peersStore.put("home".getBytes(), nodes);
//      return node;
//    }
//  }

  public void clearAndWriteNeighbours(Set<Node> nodes) {
    this.peersStore.put("neighbours".getBytes(), nodes);
  }

  public Set<Node> readNeighbours() {
    return this.peersStore.get("neighbours".getBytes());
  }

  /**
   * Cycle thread to repush Transactions
   */
  private Runnable repushLoop =
      () -> {
        while (isRunRepushThread) {
          try {
            TransactionWrapper tx = this.getRepushTransactions().poll(1, TimeUnit.SECONDS);
            if (tx != null) {
              this.rePush(tx);
            }
          } catch (InterruptedException ex) {
            logger.error(ex.getMessage());
            Thread.currentThread().interrupt();
          } catch (Exception ex) {
            logger.error("unknown exception happened in witness loop", ex);
          } catch (Throwable throwable) {
            logger.error("unknown throwable happened in witness loop", throwable);
          }
        }
      };

  public void stopRepushThread() {
    isRunRepushThread = false;
  }

  @PostConstruct
  public void init() {
    revokingStore.disable();
    revokingStore.check();
    this.setWitnessController(WitnessController.createInstance(this));
    this.setProposalController(ProposalController.createInstance(this));
    this.pendingTransactions = Collections.synchronizedList(Lists.newArrayList());
    this.repushTransactions = new LinkedBlockingQueue<>();

    this.initGenesis();
    try {
      this.khaosDb.start(getBlockById(getDynamicPropertiesStore().getLatestBlockHeaderHash()));
    } catch (ItemNotFoundException e) {
      logger.error(
          "Can not find Dynamic highest block from DB! \nnumber={} \nhash={}",
          getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
          getDynamicPropertiesStore().getLatestBlockHeaderHash());
      logger.error(
          "Please delete database directory({}) and restart",
          Args.getInstance().getOutputDirectory());
      System.exit(1);
    } catch (BadItemException e) {
      e.printStackTrace();
      logger.error("DB data broken!");
      logger.error(
          "Please delete database directory({}) and restart",
          Args.getInstance().getOutputDirectory());
      System.exit(1);
    }
    forkController.init(this);
    revokingStore.enable();

//    this.codeStore = CodeStore.create("code");
//    this.contractStore = ContractStore.create("contract");
//    this.storageStore = StorageStore.create("storage");

    validateSignService = Executors
        .newFixedThreadPool(Args.getInstance().getValidateSignThreadNum());

    repushThread = new Thread(repushLoop);
    repushThread.start();
  }

  public BlockId getGenesisBlockId() {
    return this.genesisBlock.getBlockId();
  }

  public BlockWrapper getGenesisBlock() {
    return genesisBlock;
  }

  /**
   * init genesis block.
   */
  public void initGenesis() {
    this.genesisBlock = BlockUtil.newGenesisBlockCapsule();
    if (this.containBlock(this.genesisBlock.getBlockId())) {
      Args.getInstance().setChainId(this.genesisBlock.getBlockId().toString());
    } else {
      if (this.hasBlocks()) {
        logger.error(
            "genesis block modify, please delete database directory({}) and restart",
            Args.getInstance().getOutputDirectory());
        System.exit(1);
      } else {
        logger.info("create genesis block");
        Args.getInstance().setChainId(this.genesisBlock.getBlockId().toString());
        // this.pushBlock(this.genesisBlock);
        blockStore.put(this.genesisBlock.getBlockId().getBytes(), this.genesisBlock);
        this.blockIndexStore.put(this.genesisBlock.getBlockId());

        logger.info("save block: " + this.genesisBlock);
        // init DynamicPropertiesStore
        this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(0);

        this.dynamicPropertiesStore.saveLatestBlockHeaderHash(
            this.genesisBlock.getBlockId().getByteString());

        this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(
            this.genesisBlock.getTimeStamp());

        this.initAccount();
        this.initWitness();
        this.witnessController.initWits();
        this.khaosDb.start(genesisBlock);
        this.updateRecentBlock(genesisBlock);
      }
    }
  }

  /**
   * save account into database.
   */
  public void initAccount() {
    final Args args = Args.getInstance();
    final GenesisBlock genesisBlockArg = args.getGenesisBlock();
    genesisBlockArg
        .getAssets()
        .forEach(
            account -> {
              account.setAccountType("Normal"); // to be set in conf
              final AccountWrapper accountWrapper =
                  new AccountWrapper(
                      account.getAccountName(),
                      ByteString.copyFrom(account.getAddress()),
                      account.getAccountType(),
                      account.getBalance());
              this.accountStore.put(account.getAddress(), accountWrapper);
              this.accountIdIndexStore.put(accountWrapper);
            });
  }

  /**
   * save witnesses into database.
   */
  private void initWitness() {
    final Args args = Args.getInstance();
    final GenesisBlock genesisBlockArg = args.getGenesisBlock();
    genesisBlockArg
        .getWitnesses()
        .forEach(
            key -> {
              byte[] keyAddress = key.getAddress();
              ByteString address = ByteString.copyFrom(keyAddress);

              final AccountWrapper accountWrapper;
              if (!this.accountStore.has(keyAddress)) {
                accountWrapper = new AccountWrapper(ByteString.EMPTY,
                    address, AccountType.AssetIssue, 0L);
              } else {
                accountWrapper = this.accountStore.getUnchecked(keyAddress);
              }
              accountWrapper.setIsWitness(true);
              this.accountStore.put(keyAddress, accountWrapper);

              final WitnessWrapper witnessCapsule =
                  new WitnessWrapper(address, key.getVoteCount(), key.getUrl());
              witnessCapsule.setIsJobs(true);
              this.witnessStore.put(keyAddress, witnessCapsule);
            });
  }

  public AccountStore getAccountStore() {
    return this.accountStore;
  }

  public void adjustBalance(byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    AccountWrapper account = getAccountStore().getUnchecked(accountAddress);
    adjustBalance(account, amount);
  }

  /**
   * judge balance.
   */
  public void adjustBalance(AccountWrapper account, long amount)
      throws BalanceInsufficientException {

    long balance = account.getBalance();
    if (amount == 0) {
      return;
    }

    if (amount < 0 && balance < -amount) {
      throw new BalanceInsufficientException(
          StringUtil.createReadableString(account.createDbKey()) + " insufficient balance");
    }
    account.setBalance(Math.addExact(balance, amount));
    this.getAccountStore().put(account.getAddress().toByteArray(), account);
  }


  public void adjustAllowance(byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    AccountWrapper account = getAccountStore().getUnchecked(accountAddress);
    long allowance = account.getAllowance();
    if (amount == 0) {
      return;
    }

    if (amount < 0 && allowance < -amount) {
      throw new BalanceInsufficientException(
          StringUtil.createReadableString(accountAddress) + " insufficient balance");
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
      byte[] blockHash = this.recentBlockStore.get(refBlockNumBytes).getData();
      if (Arrays.equals(blockHash, refBlockHash)) {
        return;
      } else {
        String str = String.format(
            "Tapos failed, different block hash, %s, %s , recent block %s, solid block %s head block %s",
            ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
            Hex.toHexString(blockHash),
            getSolidBlockId().getString(), getHeadBlockId().getString()).toString();
        logger.info(str);
        throw new TaposException(str);

      }
    } catch (ItemNotFoundException e) {
      String str = String.
          format("Tapos failed, block not found, ref block %s, %s , solid block %s head block %s",
              ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
              getSolidBlockId().getString(), getHeadBlockId().getString()).toString();
      logger.info(str);
      throw new TaposException(str);
    }
  }

  void validateCommon(TransactionWrapper transactionCapsule)
      throws TransactionExpirationException, TooBigTransactionException {
    if (transactionCapsule.getData().length > Constant.TRANSACTION_MAX_BYTE_SIZE) {
      throw new TooBigTransactionException(
          "too big transaction, the size is " + transactionCapsule.getData().length + " bytes");
    }
    long transactionExpiration = transactionCapsule.getExpiration();
    long headBlockTime = getHeadBlockTimeStamp();
    if (transactionExpiration <= headBlockTime ||
        transactionExpiration > headBlockTime + Constant.MAXIMUM_TIME_UNTIL_EXPIRATION) {
      throw new TransactionExpirationException(
          "transaction expiration, transaction expiration time is " + transactionExpiration
              + ", but headBlockTime is " + headBlockTime);
    }
  }

  void validateDup(TransactionWrapper transactionCapsule) throws DupTransactionException {
    if (getTransactionStore().getUnchecked(transactionCapsule.getTransactionId().getBytes())
        != null) {
      logger.debug(ByteArray.toHexString(transactionCapsule.getTransactionId().getBytes()));
      throw new DupTransactionException("dup trans");
    }
  }

  /**
   * push transaction into pending.
   */
  public boolean pushTransaction(final TransactionWrapper trx)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      AccountResourceInsufficientException, DupTransactionException, TaposException,
      TooBigTransactionException, TransactionExpirationException, ReceiptException,
      TransactionTraceException, ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {

    if (!trx.validateSignature()) {
      throw new ValidateSignatureException("trans sig validate failed");
    }

    //validateFreq(trx);
    synchronized (this) {
      if (!session.valid()) {
        session.setValue(revokingStore.buildSession());
      }

      try (ISession tmpSession = revokingStore.buildSession()) {
        processTransaction(trx, null);
        pendingTransactions.add(trx);
        tmpSession.merge();
      }
    }
    return true;
  }


  public void consumeBandwidth(TransactionWrapper trx, TransactionResultWrapper ret,
                               TransactionTrace trace)
      throws ContractValidateException, AccountResourceInsufficientException, TooBigTransactionResultException {
    BandwidthProcessor processor = new BandwidthProcessor(this);
    processor.consume(trx, ret, trace);
  }

  public void consumeEnergy(TransactionWrapper trx, TransactionResultWrapper ret,
                            TransactionTrace trace)
      throws ContractValidateException, AccountResourceInsufficientException {
    EnergyProcessor processor = new EnergyProcessor(this);
    processor.consume(trx, ret, trace);
  }

  @Deprecated
  private void validateFreq(TransactionWrapper trx) throws HighFreqException {
    List<org.gsc.protos.Protocol.Transaction.Contract> contracts =
        trx.getInstance().getRawData().getContractList();
    for (Transaction.Contract contract : contracts) {
      if (contract.getType() == TransferContract || contract.getType() == TransferAssetContract) {
        byte[] address = TransactionWrapper.getOwner(contract);
        AccountWrapper accountWrapper = this.getAccountStore().getUnchecked(address);
        if (accountWrapper == null) {
          throw new HighFreqException("account not exists");
        }
        long balance = accountWrapper.getBalance();
        long latestOperationTime = accountWrapper.getLatestOperationTime();
        if (latestOperationTime != 0) {
          doValidateFreq(balance, 0, latestOperationTime);
        }
        accountWrapper.setLatestOperationTime(Time.getCurrentMillis());
        this.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
      }
    }
  }

  @Deprecated
  private void doValidateFreq(long balance, int transNumber, long latestOperationTime)
      throws HighFreqException {
    long now = Time.getCurrentMillis();
    // todo: avoid ddos, design more smoothly formula later.
    if (balance < 1000000 * 1000) {
      if (now - latestOperationTime < 5 * 60 * 1000) {
        throw new HighFreqException("try later");
      }
    }
  }

  /**
   * when switch fork need erase blocks on fork branch.
   */
  public void eraseBlock() {
    session.reset();
    try {
      BlockWrapper oldHeadBlock = getBlockById(
          getDynamicPropertiesStore().getLatestBlockHeaderHash());
      logger.info("begin to erase block:" + oldHeadBlock);
      khaosDb.pop();
      revokingStore.pop();
      logger.info("end to erase block:" + oldHeadBlock);
      popedTransactions.addAll(oldHeadBlock.getTransactions());
      // todo: need add ??
      // repushTransactions.addAll(oldHeadBlock.getTransactions());
      //
    } catch (ItemNotFoundException | BadItemException e) {
      logger.warn(e.getMessage(), e);
    }
  }

  private void applyBlock(BlockWrapper block) throws ContractValidateException,
      ContractExeException, ValidateSignatureException, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException, ReceiptException,
      TaposException, ValidateScheduleException, TransactionTraceException, ReceiptCheckErrException,
      UnsupportVMException, TooBigTransactionResultException {
    processBlock(block);
    this.blockStore.put(block.getBlockId().getBytes(), block);
    this.blockIndexStore.put(block.getBlockId());
    updateFork();
  }

  private void switchFork(BlockWrapper newHead)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      ValidateScheduleException, AccountResourceInsufficientException, TaposException,
      TooBigTransactionException, TooBigTransactionResultException, DupTransactionException, TransactionExpirationException,
      NonCommonBlockException, ReceiptException, TransactionTraceException, ReceiptCheckErrException,
      UnsupportVMException {
    Pair<LinkedList<KhaosBlock>, LinkedList<KhaosBlock>> binaryTree;
    try {
      binaryTree =
          khaosDb.getBranch(
              newHead.getBlockId(), getDynamicPropertiesStore().getLatestBlockHeaderHash());
    } catch (NonCommonBlockException e) {
      logger.info(
          "there is not the most recent common ancestor, need to remove all blocks in the fork chain.");
      BlockWrapper tmp = newHead;
      while (tmp != null) {
        khaosDb.removeBlk(tmp.getBlockId());
        tmp = khaosDb.getBlock(tmp.getParentHash());
      }

      throw e;
    }
    if (CollectionUtils.isNotEmpty(binaryTree.getValue())) {
      while (!getDynamicPropertiesStore()
          .getLatestBlockHeaderHash()
          .equals(binaryTree.getValue().peekLast().getParentHash())) {
        eraseBlock();
      }
    }

    if (CollectionUtils.isNotEmpty(binaryTree.getKey())) {
      List<KhaosBlock> first = new ArrayList<>(binaryTree.getKey());
      Collections.reverse(first);
      for (KhaosBlock item : first) {
        Exception exception = null;
        // todo  process the exception carefully later
        try (ISession tmpSession = revokingStore.buildSession()) {
          applyBlock(item.getBlk());
          tmpSession.commit();
        } catch (AccountResourceInsufficientException
            | ValidateSignatureException
            | ContractValidateException
            | ContractExeException
            | TaposException
            | DupTransactionException
            | TransactionExpirationException
            | TransactionTraceException
            | ReceiptException
            | ReceiptCheckErrException
            | TooBigTransactionException
            | TooBigTransactionResultException
            | ValidateScheduleException
            | UnsupportVMException e) {
          logger.warn(e.getMessage(), e);
          exception = e;
          throw e;
        } finally {
          if (exception != null) {
            logger.warn("switch back because exception thrown while switching forks. " + exception
                    .getMessage(),
                exception);
            first.forEach(khaosBlock -> khaosDb.removeBlk(khaosBlock.getBlk().getBlockId()));
            khaosDb.setHead(binaryTree.getValue().peekFirst());

            while (!getDynamicPropertiesStore()
                .getLatestBlockHeaderHash()
                .equals(binaryTree.getValue().peekLast().getParentHash())) {
              eraseBlock();
            }

            List<KhaosBlock> second = new ArrayList<>(binaryTree.getValue());
            Collections.reverse(second);
            for (KhaosBlock khaosBlock : second) {
              // todo  process the exception carefully later
              try (ISession tmpSession = revokingStore.buildSession()) {
                applyBlock(khaosBlock.getBlk());
                tmpSession.commit();
              } catch (AccountResourceInsufficientException
                  | ValidateSignatureException
                  | ContractValidateException
                  | ContractExeException
                  | TaposException
                  | DupTransactionException
                  | TransactionExpirationException
                  | TooBigTransactionException
                  | ValidateScheduleException e) {
                logger.warn(e.getMessage(), e);
              }
            }
          }
        }
      }
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
      UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException,
      TaposException, TooBigTransactionException, TooBigTransactionResultException, DupTransactionException, TransactionExpirationException,
      BadNumberBlockException, BadBlockException, NonCommonBlockException, ReceiptException, TransactionTraceException,
      ReceiptCheckErrException, UnsupportVMException {
    try (PendingManager pm = new PendingManager(this)) {

      if (!block.generatedByMyself) {
        if (!block.validateSignature()) {
          logger.warn("The signature is not validated.");
          throw new BadBlockException("The signature is not validated");
        }

        if (!block.calcMerkleRoot().equals(block.getMerkleRoot())) {
          logger.warn(
              "The merkle root doesn't match, Calc result is "
                  + block.calcMerkleRoot()
                  + " , the headers is "
                  + block.getMerkleRoot());
          throw new BadBlockException("The merkle hash is not validated");
        }
      }

      BlockWrapper newBlock = this.khaosDb.push(block);

      // DB don't need lower block
      if (getDynamicPropertiesStore().getLatestBlockHeaderHash() == null) {
        if (newBlock.getNum() != 0) {
          return;
        }
      } else {
        if (newBlock.getNum() <= getDynamicPropertiesStore().getLatestBlockHeaderNumber()) {
          return;
        }

        // switch fork
        if (!newBlock
            .getParentHash()
            .equals(getDynamicPropertiesStore().getLatestBlockHeaderHash())) {
          logger.warn(
              "switch fork! new head num = {}, blockid = {}",
              newBlock.getNum(),
              newBlock.getBlockId());

          logger.warn(
              "******** before switchFork ******* push block: "
                  + block.getShortString()
                  + ", new block:"
                  + newBlock.getShortString()
                  + ", dynamic head num: "
                  + dynamicPropertiesStore.getLatestBlockHeaderNumber()
                  + ", dynamic head hash: "
                  + dynamicPropertiesStore.getLatestBlockHeaderHash()
                  + ", dynamic head timestamp: "
                  + dynamicPropertiesStore.getLatestBlockHeaderTimestamp()
                  + ", khaosDb head: "
                  + khaosDb.getHead()
                  + ", khaosDb miniStore size: "
                  + khaosDb.getMiniStore().size()
                  + ", khaosDb unlinkMiniStore size: "
                  + khaosDb.getMiniUnlinkedStore().size());

          switchFork(newBlock);
          logger.info("save block: " + newBlock);

          logger.warn(
              "******** after switchFork ******* push block: "
                  + block.getShortString()
                  + ", new block:"
                  + newBlock.getShortString()
                  + ", dynamic head num: "
                  + dynamicPropertiesStore.getLatestBlockHeaderNumber()
                  + ", dynamic head hash: "
                  + dynamicPropertiesStore.getLatestBlockHeaderHash()
                  + ", dynamic head timestamp: "
                  + dynamicPropertiesStore.getLatestBlockHeaderTimestamp()
                  + ", khaosDb head: "
                  + khaosDb.getHead()
                  + ", khaosDb miniStore size: "
                  + khaosDb.getMiniStore().size()
                  + ", khaosDb unlinkMiniStore size: "
                  + khaosDb.getMiniUnlinkedStore().size());

          return;
        }
        try (ISession tmpSession = revokingStore.buildSession()) {
          applyBlock(newBlock);
          tmpSession.commit();
        } catch (Throwable throwable) {
          logger.error(throwable.getMessage(), throwable);
          khaosDb.removeBlk(block.getBlockId());
          throw throwable;
        }
      }
      // save block: BlockWrapper
      logger.info("-------save block:------- \n" + newBlock + "\n-------");
    }
  }

  public void updateDynamicProperties(BlockWrapper block) {
    long slot = 1;
    if (block.getNum() != 1) {
      slot = witnessController.getSlotAtTime(block.getTimeStamp());
    }
    for (int i = 1; i < slot; ++i) {
      if (!witnessController.getScheduledWitness(i).equals(block.getWitnessAddress())) {
        WitnessWrapper w =
            this.witnessStore
                .getUnchecked(StringUtil.createDbKey(witnessController.getScheduledWitness(i)));
        w.setTotalMissed(w.getTotalMissed() + 1);
        this.witnessStore.put(w.createDbKey(), w);
        logger.info(
            "{} miss a block. totalMissed = {}", w.createReadableString(), w.getTotalMissed());
      }
      this.dynamicPropertiesStore.applyBlock(false);
    }
    this.dynamicPropertiesStore.applyBlock(true);

    if (slot <= 0) {
      logger.warn("missedBlocks [" + slot + "] is illegal");
    }

    // update head, num = 4
    logger.info("update head, num = {}", block.getNum());
    this.dynamicPropertiesStore.saveLatestBlockHeaderHash(block.getBlockId().getByteString());

    this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
    this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(block.getTimeStamp());
    revokingStore.setMaxSize((int) (dynamicPropertiesStore.getLatestBlockHeaderNumber()
        - dynamicPropertiesStore.getLatestSolidifiedBlockNum()
        + 1));
    khaosDb.setMaxSize((int)
        (dynamicPropertiesStore.getLatestBlockHeaderNumber()
            - dynamicPropertiesStore.getLatestSolidifiedBlockNum()
            + 1));
  }

  /**
   * Get the fork branch.
   */
  public LinkedList<BlockId> getBlockChainHashesOnFork(final BlockId forkBlockHash)
      throws NonCommonBlockException {
    final Pair<LinkedList<KhaosBlock>, LinkedList<KhaosBlock>> branch =
        this.khaosDb.getBranch(
            getDynamicPropertiesStore().getLatestBlockHeaderHash(), forkBlockHash);

    LinkedList<KhaosBlock> blockCapsules = branch.getValue();

    if (blockCapsules.isEmpty()) {
      logger.info("empty branch {}", forkBlockHash);
      return Lists.newLinkedList();
    }

    LinkedList<BlockId> result = blockCapsules.stream()
        .map(KhaosBlock::getBlk)
        .map(BlockWrapper::getBlockId)
        .collect(Collectors.toCollection(LinkedList::new));

    result.add(blockCapsules.peekLast().getBlk().getParentBlockId());

    return result;
  }

  /**
   * judge id.
   *
   * @param blockHash blockHash
   */
  public boolean containBlock(final Sha256Hash blockHash) {
    try {
      return this.khaosDb.containBlockInMiniStore(blockHash)
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
    byte[] headHash = getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes();
    long headNum = getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    trans.setReference(headNum, headHash);
  }

  /**
   * Get a BlockWrapper by id.
   */
  public BlockWrapper getBlockById(final Sha256Hash hash)
      throws BadItemException, ItemNotFoundException {
    return this.khaosDb.containBlock(hash)
        ? this.khaosDb.getBlock(hash)
        : blockStore.get(hash.getBytes());
  }


  /**
   * judge has blocks.
   */
  public boolean hasBlocks() {
    return blockStore.iterator().hasNext() || this.khaosDb.hasData();
  }

  /**
   * Process the transaction.
   */
  public boolean processTransaction(final TransactionWrapper trxCap, BlockWrapper blockCap)
      throws ValidateSignatureException, ContractValidateException, ContractExeException, ReceiptException,
      AccountResourceInsufficientException, TransactionExpirationException, TooBigTransactionException, TooBigTransactionResultException,
      DupTransactionException, TaposException, TransactionTraceException, ReceiptCheckErrException, UnsupportVMException {
    if (trxCap == null) {
      return false;
    }

    validateTapos(trxCap);
    validateCommon(trxCap);

    if (trxCap.getInstance().getRawData().getContractList().size() != 1) {
      throw new ContractSizeNotEqualToOneException(
          "act size should be exactly 1, this is extend feature");
    }
    forkController.hardFork(trxCap);

    validateDup(trxCap);

    if (!trxCap.validateSignature()) {
      throw new ValidateSignatureException("trans sig validate failed");
    }

    TransactionTrace trace = new TransactionTrace(trxCap, this);

    DepositImpl deposit = DepositImpl.createRoot(this);
    Runtime runtime = new Runtime(trace, blockCap, deposit, new ProgramInvokeFactoryImpl());
    if (runtime.isCallConstant()) {
      throw new UnsupportVMException("cannot call constant method ");
    }

    consumeBandwidth(trxCap, runtime.getResult().getRet(), trace);

    trace.init();

    trace.exec(runtime);

    if (Objects.nonNull(blockCap)) {
      trace.setResult(runtime);
      if (!blockCap.generatedByMyself) {
        trace.check();
      }
    }

    trace.finalization(runtime);
    if (Objects.nonNull(blockCap)) {
      if (getDynamicPropertiesStore().supportVM()) {
        trxCap.setResult(runtime);
      }
    }
    transactionStore.put(trxCap.getTransactionId().getBytes(), trxCap);

    ReceiptWrapper traceReceipt = trace.getReceipt();
    TransactionInfoWrapper transactionInfo = TransactionInfoWrapper
        .buildInstance(trxCap, blockCap, runtime, traceReceipt);

    transactionHistoryStore.put(trxCap.getTransactionId().getBytes(), transactionInfo);

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
          final WitnessWrapper witnessCapsule, final long when, final byte[] privateKey)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,

      UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException, TransactionTraceException {

    final long timestamp = this.dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    final long number = this.dynamicPropertiesStore.getLatestBlockHeaderNumber();
    final Sha256Hash preHash = this.dynamicPropertiesStore.getLatestBlockHeaderHash();

    // judge create block time
    if (when < timestamp) {
      throw new IllegalArgumentException("generate block timestamp is invalid.");
    }

    long postponedTrxCount = 0;

    final BlockWrapper blockWrapper =
        new BlockWrapper(number + 1, preHash, when, witnessCapsule.getAddress());
    blockWrapper.generatedByMyself = true;
    session.reset();
    session.setValue(revokingStore.buildSession());

    Iterator<TransactionWrapper> iterator = pendingTransactions.iterator();
    while (iterator.hasNext()) {
      TransactionWrapper trx = iterator.next();
      if (DateTime.now().getMillis() - when
          > ChainConstant.BLOCK_PRODUCED_INTERVAL * 0.5 * ChainConstant.BLOCK_PRODUCED_TIME_OUT
          / 100) {
        logger.warn("Processing transaction time exceeds the 50% producing time。");
        break;
      }
      // check the block size
      if ((blockWrapper.getInstance().getSerializedSize() + trx.getSerializedSize() + 3)
          > ChainConstant.BLOCK_SIZE) {
        postponedTrxCount++;
        continue;
      }
      // apply transaction
      try (ISession tmpSeesion = revokingStore.buildSession()) {
        processTransaction(trx, blockWrapper);
        // trx.resetResult();
        tmpSeesion.merge();
        // push into block
        blockWrapper.addTransaction(trx);
        iterator.remove();
      } catch (ContractExeException e) {
        logger.info("contract not processed during execute");
        logger.debug(e.getMessage(), e);
      } catch (ContractValidateException e) {
        logger.info("contract not processed during validate");
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
      } catch (TooBigTransactionResultException e) {
        logger.info("contract not processed during TooBigTransactionResultException");
        logger.debug(e.getMessage(), e);
      } catch (TransactionExpirationException e) {
        logger.info("contract not processed during TransactionExpirationException");
        logger.debug(e.getMessage(), e);
      } catch (AccountResourceInsufficientException e) {
        logger.info("contract not processed during AccountResourceInsufficientException");
        logger.debug(e.getMessage(), e);
      } catch (ValidateSignatureException e) {
        logger.info("contract not processed during ValidateSignatureException");
        logger.debug(e.getMessage(), e);
      } catch (ReceiptException e) {
        logger.info("receipt exception: {}", e.getMessage());
        logger.debug(e.getMessage(), e);
      } catch (ReceiptCheckErrException e) {
        logger.info("OutOfSlotTime exception: {}", e.getMessage());
        logger.debug(e.getMessage(), e);
      } catch (UnsupportVMException e) {
        logger.warn(e.getMessage(), e);
      }
    }

    session.reset();

    if (postponedTrxCount > 0) {
      logger.info("{} transactions over the block size limit", postponedTrxCount);
    }

    logger.info(
        "postponedTrxCount[" + postponedTrxCount + "],TrxLeft[" + pendingTransactions.size()
            + "]");
    blockWrapper.setMerkleRoot();
    blockWrapper.sign(privateKey);

    try {
      this.pushBlock(blockWrapper);
      return blockWrapper;
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
    } catch (BadBlockException e) {
      logger.info("block exception");
    } catch (NonCommonBlockException e) {
      logger.info("non common exception");
    } catch (ReceiptException e) {
      logger.info("receipt exception: {}", e.getMessage());
      logger.debug(e.getMessage(), e);
    } catch (ReceiptCheckErrException e) {
      logger.info("OutOfSlotTime exception: {}", e.getMessage());
      logger.debug(e.getMessage(), e);
    } catch (UnsupportVMException e) {
      logger.warn(e.getMessage(), e);
    } catch (TooBigTransactionResultException e) {
      logger.info("contract not processed during TooBigTransactionResultException");
    }

    return null;
  }

  private void setAccountStore(final AccountStore accountStore) {
    this.accountStore = accountStore;
  }

  public TransactionStore getTransactionStore() {
    return this.transactionStore;
  }

  private void setTransactionStore(final TransactionStore transactionStore) {
    this.transactionStore = transactionStore;
  }

  public TransactionHistoryStore getTransactionHistoryStore() {
    return this.transactionHistoryStore;
  }

  private void setTransactionHistoryStore(final TransactionHistoryStore transactionHistoryStore) {
    this.transactionHistoryStore = transactionHistoryStore;
  }

  public BlockStore getBlockStore() {
    return this.blockStore;
  }

  private void setBlockStore(final BlockStore blockStore) {
    this.blockStore = blockStore;
  }

  /**
   * process block.
   */
  public void processBlock(BlockWrapper block)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      AccountResourceInsufficientException, TaposException, TooBigTransactionException,
      DupTransactionException, TransactionExpirationException, ValidateScheduleException,
      ReceiptException, TransactionTraceException, ReceiptCheckErrException, UnsupportVMException, TooBigTransactionResultException {
    // todo set revoking db max size.

    // checkWitness
    if (!witnessController.validateWitnessSchedule(block)) {
      throw new ValidateScheduleException("validateWitnessSchedule error");
    }

    for (TransactionWrapper transactionCapsule : block.getTransactions()) {
      if (block.generatedByMyself) {
        transactionCapsule.setVerified(true);
      }
      processTransaction(transactionCapsule, block);
    }

    // update witness dynamicPropertiesStore.getNextMaintenanceTime() <= blockTime;
    boolean needMaint = needMaintenance(block.getTimeStamp());
    if (needMaint) {
      if (block.getNum() == 1) {

        this.dynamicPropertiesStore.updateNextMaintenanceTime(block.getTimeStamp());
      } else {
        this.processMaintenance(block);
      }
    }

    this.updateDynamicProperties(block);

    this.updateSignedWitness(block); // allowance changed 出块奖励
    this.updateLatestSolidifiedBlock();
    this.updateTransHashCache(block);
    updateMaintenanceState(needMaint);
    //witnessController.updateWitnessSchedule();

    updateRecentBlock(block);
    this.dynamicPropertiesStore.saveWitnessPayPerBlockByBlockNum(block.getNum()); //update award per block
    this.dynamicPropertiesStore.saveWitnessStandbyAllowance(block.getNum());
  }

  private void updateTransHashCache(BlockWrapper block) {
    for (TransactionWrapper transactionWrapper : block.getTransactions()) {
      this.transactionIdCache.put(transactionWrapper.getTransactionId(), true);
    }
  }

  public void updateRecentBlock(BlockWrapper block) {
    this.recentBlockStore.put(ByteArray.subArray(
        ByteArray.fromLong(block.getNum()), 6, 8),
        new BytesWrapper(ByteArray.subArray(block.getBlockId().getBytes(), 8, 16)));
  }

  /**
   * update the latest solidified block.
   */
  public void updateLatestSolidifiedBlock() {
    List<Long> numbers =
        witnessController
            .getActiveWitnesses()
            .stream()
            .map(address -> witnessController.getWitnesseByAddress(address).getLatestBlockNum())
            .sorted()
            .collect(Collectors.toList());

    long size = witnessController.getActiveWitnesses().size();
    int solidifiedPosition = (int) (size * (1 - SOLIDIFIED_THRESHOLD * 1.0 / 100));


    while (numbers.get(solidifiedPosition) == 0) solidifiedPosition++; //fix fake witness problem
    if (solidifiedPosition < 0) {
      logger.warn(
          "updateLatestSolidifiedBlock error, solidifiedPosition:{},wits.size:{}",
          solidifiedPosition,
          size);
      return;
    }
    long latestSolidifiedBlockNum = numbers.get(solidifiedPosition);
    //if current value is less than the previous value，keep the previous value.
    if (latestSolidifiedBlockNum < getDynamicPropertiesStore().getLatestSolidifiedBlockNum()) {
      logger.warn("latestSolidifiedBlockNum = 0,LatestBlockNum:{}", numbers);
      return;
    }
    getDynamicPropertiesStore().saveLatestSolidifiedBlockNum(latestSolidifiedBlockNum);
    // update solid block, num = 279
    logger.info("update solid block, num = {}", latestSolidifiedBlockNum);
  }

  public void updateFork() {
    if (forkController.shouldBeForked()) {
      return;
    }

    try {
      long latestSolidifiedBlockNum = dynamicPropertiesStore.getLatestSolidifiedBlockNum();
      BlockWrapper solidifiedBlock = getBlockByNum(latestSolidifiedBlockNum);
      forkController.update(solidifiedBlock);
    } catch (ItemNotFoundException | BadItemException e) {
      logger.error("solidified block not found");
    }
  }

  public long getSyncBeginNumber() {
    logger.info("headNumber:" + dynamicPropertiesStore.getLatestBlockHeaderNumber());
    logger.info(
        "syncBeginNumber:"
            + (dynamicPropertiesStore.getLatestBlockHeaderNumber() - revokingStore.size()));
    logger.info("solidBlockNumber:" + dynamicPropertiesStore.getLatestSolidifiedBlockNum());
    return dynamicPropertiesStore.getLatestBlockHeaderNumber() - revokingStore.size();
  }

  public BlockId getSolidBlockId() {
    try {
      long num = dynamicPropertiesStore.getLatestSolidifiedBlockNum();
      return getBlockIdByNum(num);
    } catch (Exception e) {
      return getGenesisBlockId();
    }
  }

  /**
   * Determine if the current time is maintenance time.
   */
  public boolean needMaintenance(long blockTime) {
    return this.dynamicPropertiesStore.getNextMaintenanceTime() <= blockTime;
  }

  /**
   * Perform maintenance.
   */
  private void processMaintenance(BlockWrapper block) {
    proposalController.processProposals();
    witnessController.updateWitness();
    this.dynamicPropertiesStore.updateNextMaintenanceTime(block.getTimeStamp());
    forkController.reset();
  }

  /**
   * @param block the block update signed witness. set witness who signed block the 1. the latest
   * block num 2. pay the trx to witness. 3. the latest slot num.
   */
  public void updateSignedWitness(BlockWrapper block) {
    // TODO: add verification
    WitnessWrapper witnessWrapper =
        witnessStore.getUnchecked(
            block.getInstance().getBlockHeader().getRawData().getWitnessAddress().toByteArray());
    witnessWrapper.setTotalProduced(witnessWrapper.getTotalProduced() + 1);
    witnessWrapper.setLatestBlockNum(block.getNum());
    witnessWrapper.setLatestSlotNum(witnessController.getAbSlotAtTime(block.getTimeStamp()));

    // Update memory witness status
    WitnessWrapper wit = witnessController.getWitnesseByAddress(block.getWitnessAddress());
    if (wit != null) {
      wit.setTotalProduced(witnessWrapper.getTotalProduced() + 1);
      wit.setLatestBlockNum(block.getNum());
      wit.setLatestSlotNum(witnessController.getAbSlotAtTime(block.getTimeStamp()));
    }

    this.getWitnessStore().put(witnessWrapper.getAddress().toByteArray(), witnessWrapper);

    try {
      adjustAllowance(witnessWrapper.getAddress().toByteArray(),
          getDynamicPropertiesStore().getWitnessPayPerBlock());
    } catch (BalanceInsufficientException e) {
      logger.warn(e.getMessage(), e);
    }

    logger.debug(
        "updateSignedWitness. witness address:{}, blockNum:{}, totalProduced:{}",
            witnessWrapper.createReadableString(),
        block.getNum(),
            witnessWrapper.getTotalProduced());
  }

  public void updateMaintenanceState(boolean needMaint) {
    if (needMaint) {
      getDynamicPropertiesStore().saveStateFlag(1);
    } else {
      getDynamicPropertiesStore().saveStateFlag(0);
    }
  }

  public boolean lastHeadBlockIsMaintenance() {
    return getDynamicPropertiesStore().getStateFlag() == 1;
  }

  // To be added
  public long getSkipSlotInMaintenance() {
    return getDynamicPropertiesStore().getMaintenanceSkipSlots();
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

  public AccountIdIndexStore getAccountIdIndexStore() {
    return this.accountIdIndexStore;
  }

  public void setAccountIdIndexStore(AccountIdIndexStore indexStore) {
    this.accountIdIndexStore = indexStore;
  }

  public void closeAllStore() {
    System.err.println("******** begin to close db ********");
    closeOneStore(accountStore);
    closeOneStore(blockStore);
    closeOneStore(blockIndexStore);
    closeOneStore(accountIdIndexStore);
    closeOneStore(witnessStore);
    closeOneStore(witnessScheduleStore);
    closeOneStore(assetIssueStore);
    closeOneStore(dynamicPropertiesStore);
    closeOneStore(transactionStore);
    closeOneStore(codeStore);
    closeOneStore(contractStore);
    closeOneStore(storageRowStore);
    System.err.println("******** end to close db ********");
  }

  private void closeOneStore(IGSCChainBase database) {
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
    // if (getPendingTransactions().size() + PendingManager.getTmpTransactions().size()
    if (getPendingTransactions().size() + getRepushTransactions().size()
        > MAX_TRANSACTION_PENDING) {
      return true;
    }
    return false;
  }

  public boolean isGeneratingBlock() {
    if (Args.getInstance().isWitness()) {
      return witnessController.isGeneratingBlock();
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

  public void rePush(TransactionWrapper tx) {

    try {
      if (transactionStore.get(tx.getTransactionId().getBytes()) != null) {
        return;
      }
    } catch (BadItemException e) {
      // do nothing
    }

    try {
      this.pushTransaction(tx);
    } catch (ValidateSignatureException e) {
      logger.debug(e.getMessage(), e);
    } catch (ContractValidateException e) {
      logger.debug(e.getMessage(), e);
    } catch (ContractExeException e) {
      logger.debug(e.getMessage(), e);
    } catch (AccountResourceInsufficientException e) {
      logger.debug(e.getMessage(), e);
    } catch (DupTransactionException e) {
      logger.debug("pending manager: dup trans", e);
    } catch (TaposException e) {
      logger.debug("pending manager: tapos exception", e);
    } catch (TooBigTransactionException e) {
      logger.debug("too big transaction");
    } catch (ReceiptException e) {
      logger.info("Receipt exception," + e.getMessage());
    } catch (TransactionExpirationException e) {
      logger.debug("expiration transaction");
    } catch (TransactionTraceException e) {
      logger.debug("transactionTrace transaction");
    } catch (ReceiptCheckErrException e) {
      logger.debug("outOfSlotTime transaction");
    } catch (UnsupportVMException e) {
      logger.debug(e.getMessage(), e);
    } catch (TooBigTransactionResultException e) {
      logger.debug("too big transaction result");
    }
  }
}
