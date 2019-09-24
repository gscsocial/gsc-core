/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.db;

import static org.gsc.config.Parameter.ChainConstant.CONFIRMED_THRESHOLD;
import static org.gsc.config.Parameter.NodeConstant.MAX_TRANSACTION_PENDING;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javafx.util.Pair;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.gsc.core.wrapper.*;
import org.joda.time.DateTime;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.runtime.event.EventPluginLoader;
import org.gsc.runtime.event.FilterQuery;
import org.gsc.runtime.event.wrapper.BlockLogTriggerWrapper;
import org.gsc.runtime.event.wrapper.ContractTriggerWrapper;
import org.gsc.runtime.event.wrapper.TransactionLogTriggerWrapper;
import org.gsc.runtime.event.wrapper.TriggerWrapper;
import org.gsc.runtime.event.trigger.ContractTrigger;
import org.gsc.net.node.Node;
import org.gsc.net.peer.p2p.Message;
import org.gsc.runtime.config.VMConfig;
import org.gsc.utils.ByteArray;
import org.gsc.utils.ForkController;
import org.gsc.utils.SessionOptional;
import org.gsc.utils.Sha256Hash;
import org.gsc.utils.StringUtil;
import org.gsc.core.Constant;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper.BlockId;
import org.gsc.core.wrapper.utils.BlockUtil;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.config.args.GenesisBlock;
import org.gsc.db.KhaosDatabase.KhaosBlock;
import org.gsc.db.api.AssetUpdateHelper;
import org.gsc.db.accountstate.TrieService;
import org.gsc.db.accountstate.callback.AccountStateCallBack;
import org.gsc.db.db2.core.ISession;
import org.gsc.db.db2.core.IGSCChainBase;
import org.gsc.db.db2.core.SnapshotManager;
import org.gsc.core.exception.AccountResourceInsufficientException;
import org.gsc.core.exception.BadBlockException;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.BadNumberBlockException;
import org.gsc.core.exception.BalanceInsufficientException;
import org.gsc.core.exception.ContractExeException;
import org.gsc.core.exception.ContractSizeNotEqualToOneException;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.core.exception.DupTransactionException;
import org.gsc.core.exception.HeaderNotFound;
import org.gsc.core.exception.ItemNotFoundException;
import org.gsc.core.exception.NonCommonBlockException;
import org.gsc.core.exception.ReceiptCheckErrException;
import org.gsc.core.exception.TaposException;
import org.gsc.core.exception.TooBigTransactionException;
import org.gsc.core.exception.TooBigTransactionResultException;
import org.gsc.core.exception.TransactionExpirationException;
import org.gsc.core.exception.UnLinkedBlockException;
import org.gsc.core.exception.VMIllegalException;
import org.gsc.core.exception.ValidateScheduleException;
import org.gsc.core.exception.ValidateSignatureException;
import org.gsc.net.GSCNetService;
import org.gsc.net.peer.message.BlockMessage;
import org.gsc.services.WitnessService;
import org.gsc.core.witness.ProposalController;
import org.gsc.core.witness.WitnessController;
import org.gsc.protos.Protocol.AccountType;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract;
import org.gsc.protos.Protocol.TransactionInfo;


@Slf4j(topic = "DB")
@Component
public class Manager {

    // db store
    @Autowired
    private AccountStore accountStore;
    @Autowired
    private TransactionStore transactionStore;
    @Autowired(required = false)
    private TransactionCache transactionCache;
    @Autowired
    private BlockStore blockStore;
    @Autowired
    private WitnessStore witnessStore;
    @Autowired
    private AssetIssueStore assetIssueStore;
    @Autowired
    private AssetIssueV2Store assetIssueV2Store;
    @Autowired
    private DynamicPropertiesStore dynamicPropertiesStore;
    @Autowired
    @Getter
    private BlockIndexStore blockIndexStore;
    @Autowired
    @Getter
    private TransactionRetStore transactionRetStore;
    @Autowired
    private AccountIdIndexStore accountIdIndexStore;
    @Autowired
    private AccountIndexStore accountIndexStore;
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
    private ExchangeV2Store exchangeV2Store;
    @Autowired
    private TransactionHistoryStore transactionHistoryStore;
    @Autowired
    private CodeStore codeStore;
    @Autowired
    private ContractStore contractStore;
    @Autowired
    private DelegatedResourceStore delegatedResourceStore;
    @Autowired
    private DelegatedResourceAccountIndexStore delegatedResourceAccountIndexStore;
    @Autowired
    @Getter
    private StorageRowStore storageRowStore;

    @Setter
    private GSCNetService gscNetService;

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
    private WitnessService witnessService;

    @Getter
    @Setter
    private WitnessController witnessController;

    @Getter
    @Setter
    private ProposalController proposalController;

    private ExecutorService validateSignService;

    private boolean isRunRepushThread = true;

    private boolean isRunTriggerWrapperProcessThread = true;

    private long latestConfirmedBlockNumber;

    @Getter
    @Setter
    public boolean eventPluginLoaded = false;

    private BlockingQueue<TransactionWrapper> pushTransactionQueue = new LinkedBlockingQueue<>();

    @Getter
    private Cache<Sha256Hash, Boolean> transactionIdCache = CacheBuilder
            .newBuilder().maximumSize(100_000).recordStats().build();

    @Getter
    private ForkController forkController = ForkController.instance();

    @Autowired
    private AccountStateCallBack accountStateCallBack;

    @Autowired
    private TrieService trieService;
    private Set<String> ownerAddressSet = new HashSet<>();

    public WitnessStore getWitnessStore() {
        return this.witnessStore;
    }

    public boolean needToUpdateAsset() {
        return getDynamicPropertiesStore().getTokenUpdateDone() == 0L;
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


    public DelegatedResourceStore getDelegatedResourceStore() {
        return delegatedResourceStore;
    }

    public DelegatedResourceAccountIndexStore getDelegatedResourceAccountIndexStore() {
        return delegatedResourceAccountIndexStore;
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

    public ExchangeV2Store getExchangeV2Store() {
        return this.exchangeV2Store;
    }

    public ExchangeStore getExchangeStoreFinal() {
        if (getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            return getExchangeStore();
        } else {
            return getExchangeV2Store();
        }
    }

    public void putExchangeWrapper(ExchangeWrapper exchangeWrapper) {
        if (getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            getExchangeStore().put(exchangeWrapper.createDbKey(), exchangeWrapper);
            ExchangeWrapper exchangeWrapperV2 = new ExchangeWrapper(exchangeWrapper.getData());
            exchangeWrapperV2.resetTokenWithID(this);
            getExchangeV2Store().put(exchangeWrapperV2.createDbKey(), exchangeWrapperV2);
        } else {
            getExchangeV2Store().put(exchangeWrapper.createDbKey(), exchangeWrapper);
        }
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

    private BlockingQueue<TriggerWrapper> triggerWrapperQueue;

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
                    TransactionWrapper tx = null;
                    try {
                        if (isGeneratingBlock()) {
                            TimeUnit.MILLISECONDS.sleep(10L);
                            continue;
                        }
                        tx = getRepushTransactions().peek();
                        if (tx != null) {
                            this.rePush(tx);
                        } else {
                            TimeUnit.MILLISECONDS.sleep(50L);
                        }
                    } catch (Exception ex) {
                        logger.error("unknown exception happened in repush loop", ex);
                    } catch (Throwable throwable) {
                        logger.error("unknown throwable happened in repush loop", throwable);
                    } finally {
                        if (tx != null) {
                            getRepushTransactions().remove(tx);
                        }
                    }
                }
            };

    private Runnable triggerWrapperProcessLoop =
            () -> {
                while (isRunTriggerWrapperProcessThread) {
                    try {
                        TriggerWrapper triggerWrapper = triggerWrapperQueue.poll(1, TimeUnit.SECONDS);
                        if (triggerWrapper != null) {
                            triggerWrapper.processTrigger();
                        }
                    } catch (InterruptedException ex) {
                        logger.info(ex.getMessage());
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                        logger.error("unknown exception happened in process wrapper loop", ex);
                    } catch (Throwable throwable) {
                        logger.error("unknown throwable happened in process wrapper loop", throwable);
                    }
                }
            };

    public void stopRepushThread() {
        isRunRepushThread = false;
    }

    public void stopRepushTriggerThread() {
        isRunTriggerWrapperProcessThread = false;
    }

    @PostConstruct
    public void init() {
        Message.setManager(this);
        accountStateCallBack.setManager(this);
        trieService.setManager(this);
        revokingStore.disable();
        revokingStore.check();
        this.setWitnessController(WitnessController.createInstance(this));
        this.setProposalController(ProposalController.createInstance(this));
        this.pendingTransactions = Collections.synchronizedList(Lists.newArrayList());
        this.repushTransactions = new LinkedBlockingQueue<>();
        this.triggerWrapperQueue = new LinkedBlockingQueue<>();

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

        if (Args.getInstance().isNeedToUpdateAsset() && needToUpdateAsset()) {
            new AssetUpdateHelper(this).doWork();
        }

        //for test only
        dynamicPropertiesStore.updateDynamicStoreByConfig();

        initCacheTxs();
        revokingStore.enable();
        validateSignService = Executors
                .newFixedThreadPool(Args.getInstance().getValidateSignThreadNum());
        Thread repushThread = new Thread(repushLoop);
        repushThread.start();
        // add contract event listener for subscribing
        if (Args.getInstance().isEventSubscribe()) {
            startEventSubscribing();
            Thread triggerWrapperProcessThread = new Thread(triggerWrapperProcessLoop);
            triggerWrapperProcessThread.start();
        }
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
        this.genesisBlock = BlockUtil.newGenesisBlockWrapper();
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
                            this.accountIndexStore.put(accountWrapper);
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

                            final WitnessWrapper witnessWrapper =
                                    new WitnessWrapper(address, key.getVoteCount(), key.getUrl());
                            witnessWrapper.setIsJobs(true);
                            this.witnessStore.put(keyAddress, witnessWrapper);
                        });
    }

    public void initCacheTxs() {
        logger.info("begin to init txs cache.");
        int dbVersion = Args.getInstance().getStorage().getDbVersion();
        if (dbVersion != 2) {
            return;
        }
        long start = System.currentTimeMillis();
        long headNum = dynamicPropertiesStore.getLatestBlockHeaderNumber();
        long recentBlockCount = recentBlockStore.size();
        ListeningExecutorService service = MoreExecutors
                .listeningDecorator(Executors.newFixedThreadPool(50));
        List<ListenableFuture<?>> futures = new ArrayList<>();
        AtomicLong blockCount = new AtomicLong(0);
        AtomicLong emptyBlockCount = new AtomicLong(0);
        LongStream.rangeClosed(headNum - recentBlockCount + 1, headNum).forEach(
                blockNum -> futures.add(service.submit(() -> {
                    try {
                        blockCount.incrementAndGet();
                        BlockWrapper blockWrapper = getBlockByNum(blockNum);
                        if (blockWrapper.getTransactions().isEmpty()) {
                            emptyBlockCount.incrementAndGet();
                        }
                        blockWrapper.getTransactions().stream()
                                .map(tc -> tc.getTransactionId().getBytes())
                                .map(bytes -> Maps.immutableEntry(bytes, Longs.toByteArray(blockNum)))
                                .forEach(e -> transactionCache
                                        .put(e.getKey(), new BytesWrapper(e.getValue())));
                    } catch (ItemNotFoundException | BadItemException e) {
                        logger.info("init txs cache error.");
                        throw new IllegalStateException("init txs cache error.");
                    }
                })));

        ListenableFuture<?> future = Futures.allAsList(futures);
        try {
            future.get();
            service.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.info(e.getMessage());
        }

        logger.info("end to init txs cache. trxids:{}, block count:{}, empty block count:{}, cost:{}",
                transactionCache.size(),
                blockCount.get(),
                emptyBlockCount.get(),
                System.currentTimeMillis() - start
        );
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

    void validateTapos(TransactionWrapper transactionWrapper) throws TaposException {
        byte[] refBlockHash = transactionWrapper.getInstance()
                .getRawData().getRefBlockHash().toByteArray();
        byte[] refBlockNumBytes = transactionWrapper.getInstance()
                .getRawData().getRefBlockBytes().toByteArray();
        try {
            byte[] blockHash = this.recentBlockStore.get(refBlockNumBytes).getData();
            if (!Arrays.equals(blockHash, refBlockHash)) {
                String str = String.format(
                        "Tapos failed, different block hash, %s, %s , recent block %s, solid block %s head block %s",
                        ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
                        Hex.toHexString(blockHash),
                        getConfirmedBlockId().getString(), getHeadBlockId().getString()).toString();
                logger.info(str);
                throw new TaposException(str);
            }
        } catch (ItemNotFoundException e) {
            String str = String.
                    format("Tapos failed, block not found, ref block %s, %s , solid block %s head block %s",
                            ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
                            getConfirmedBlockId().getString(), getHeadBlockId().getString()).toString();
            logger.info(str);
            throw new TaposException(str);
        }
    }

    void validateCommon(TransactionWrapper transactionWrapper)
            throws TransactionExpirationException, TooBigTransactionException {
        if (transactionWrapper.getData().length > Constant.TRANSACTION_MAX_BYTE_SIZE) {
            throw new TooBigTransactionException(
                    "too big transaction, the size is " + transactionWrapper.getData().length + " bytes");
        }
        long transactionExpiration = transactionWrapper.getExpiration();
        long headBlockTime = getHeadBlockTimeStamp();
        if (transactionExpiration <= headBlockTime ||
                transactionExpiration > headBlockTime + Constant.MAXIMUM_TIME_UNTIL_EXPIRATION) {
            throw new TransactionExpirationException(
                    "transaction expiration, transaction expiration time is " + transactionExpiration
                            + ", but headBlockTime is " + headBlockTime);
        }
    }

    void validateDup(TransactionWrapper transactionWrapper) throws DupTransactionException {
        if (containsTransaction(transactionWrapper)) {
            logger.debug(ByteArray.toHexString(transactionWrapper.getTransactionId().getBytes()));
            throw new DupTransactionException("dup trans");
        }
    }

    private boolean containsTransaction(TransactionWrapper transactionWrapper) {
        if (transactionCache != null) {
            return transactionCache.has(transactionWrapper.getTransactionId().getBytes());
        }

        return transactionStore.has(transactionWrapper.getTransactionId().getBytes());
    }

    /**
     * push transaction into pending.
     */
    public boolean pushTransaction(final TransactionWrapper trx)
            throws ValidateSignatureException, ContractValidateException, ContractExeException,
            AccountResourceInsufficientException, DupTransactionException, TaposException,
            TooBigTransactionException, TransactionExpirationException,
            ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException {

        synchronized (pushTransactionQueue) {
            pushTransactionQueue.add(trx);
        }

        try {
            if (!trx.validateSignature(this)) {
                throw new ValidateSignatureException("trans sig validate failed");
            }

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
        } finally {
            pushTransactionQueue.remove(trx);
        }
        return true;
    }

    public void consumeMultiSignFee(TransactionWrapper trx, TransactionTrace trace)
            throws AccountResourceInsufficientException {
        if (trx.getInstance().getSignatureCount() > 1) {
            long fee = getDynamicPropertiesStore().getMultiSignFee();

            List<Contract> contracts = trx.getInstance().getRawData().getContractList();
            for (Contract contract : contracts) {
                byte[] address = TransactionWrapper.getOwner(contract);
                AccountWrapper accountWrapper = getAccountStore().get(address);
                try {
                    adjustBalance(accountWrapper, -fee);
                    adjustBalance(this.getAccountStore().getBlackhole().createDbKey(), +fee);
                } catch (BalanceInsufficientException e) {
                    throw new AccountResourceInsufficientException(
                            "Account Insufficient  balance[" + fee + "] to MultiSign");
                }
            }

            trace.getReceipt().setMultiSignFee(fee);
        }
    }

    public void consumeNet(TransactionWrapper trx, TransactionTrace trace)
            throws ContractValidateException, AccountResourceInsufficientException, TooBigTransactionResultException {
        NetProcessor processor = new NetProcessor(this);
        processor.consume(trx, trace);
    }


    /**
     * when switch fork need erase blocks on fork branch.
     */
    public synchronized void eraseBlock() {
        session.reset();
        try {
            BlockWrapper oldHeadBlock = getBlockById(
                    getDynamicPropertiesStore().getLatestBlockHeaderHash());
            logger.info("begin to erase block:" + oldHeadBlock);
            khaosDb.pop();
            revokingStore.fastPop();
            logger.info("end to erase block:" + oldHeadBlock);
            popedTransactions.addAll(oldHeadBlock.getTransactions());

        } catch (ItemNotFoundException | BadItemException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public void pushVerifiedBlock(BlockWrapper block) throws ContractValidateException,
            ContractExeException, ValidateSignatureException, AccountResourceInsufficientException,
            TransactionExpirationException, TooBigTransactionException, DupTransactionException,
            TaposException, ValidateScheduleException, ReceiptCheckErrException,
            VMIllegalException, TooBigTransactionResultException, UnLinkedBlockException,
            NonCommonBlockException, BadNumberBlockException, BadBlockException {
        block.generatedByMyself = true;
        long start = System.currentTimeMillis();
        pushBlock(block);
        logger.info("push block cost:{}ms, blockNum:{}, blockHash:{}, trx count:{}",
                System.currentTimeMillis() - start,
                block.getNum(),
                block.getBlockId(),
                block.getTransactions().size());
    }

    private void applyBlock(BlockWrapper block) throws ContractValidateException,
            ContractExeException, ValidateSignatureException, AccountResourceInsufficientException,
            TransactionExpirationException, TooBigTransactionException, DupTransactionException,
            TaposException, ValidateScheduleException, ReceiptCheckErrException,
            VMIllegalException, TooBigTransactionResultException, BadBlockException {
        processBlock(block);
        this.blockStore.put(block.getBlockId().getBytes(), block);
        this.blockIndexStore.put(block.getBlockId());
        if (block.getTransactions().size() != 0) {
            this.transactionRetStore.put(ByteArray.fromLong(block.getNum()), block.getResult());
        }

        updateFork(block);
        if (System.currentTimeMillis() - block.getTimeStamp() >= 60_000) {
            revokingStore.setMaxFlushCount(SnapshotManager.DEFAULT_MAX_FLUSH_COUNT);
        } else {
            revokingStore.setMaxFlushCount(SnapshotManager.DEFAULT_MIN_FLUSH_COUNT);
        }
    }

    private void switchFork(BlockWrapper newHead)
            throws ValidateSignatureException, ContractValidateException, ContractExeException,
            ValidateScheduleException, AccountResourceInsufficientException, TaposException,
            TooBigTransactionException, TooBigTransactionResultException, DupTransactionException, TransactionExpirationException,
            NonCommonBlockException, ReceiptCheckErrException,
            VMIllegalException, BadBlockException {
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
                reorgContractTrigger();
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
                        | ReceiptCheckErrException
                        | TooBigTransactionException
                        | TooBigTransactionResultException
                        | ValidateScheduleException
                        | VMIllegalException
                        | BadBlockException e) {
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

    /**
     * save a block.
     */
    public synchronized void pushBlock(final BlockWrapper block)
            throws ValidateSignatureException, ContractValidateException, ContractExeException,
            UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException,
            TaposException, TooBigTransactionException, TooBigTransactionResultException, DupTransactionException, TransactionExpirationException,
            BadNumberBlockException, BadBlockException, NonCommonBlockException,
            ReceiptCheckErrException, VMIllegalException {
        long start = System.currentTimeMillis();
        try (PendingManager pm = new PendingManager(this)) {

            if (!block.generatedByMyself) {
                if (!block.validateSignature(this)) {
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

            if (witnessService != null) {
                witnessService.checkDupWitness(block);
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
                                    + block.toString()
                                    + ", new block:"
                                    + newBlock.toString()
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
                                    + block.toString()
                                    + ", new block:"
                                    + newBlock.toString()
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
                    // if event subscribe is enabled, post block trigger to queue
                    postBlockTrigger(newBlock);
                } catch (Throwable throwable) {
                    logger.error(throwable.getMessage(), throwable);
                    khaosDb.removeBlk(block.getBlockId());
                    throw throwable;
                }
            }
            logger.info("save block: " + newBlock);
        }
        //clear ownerAddressSet
        synchronized (pushTransactionQueue) {
            if (CollectionUtils.isNotEmpty(ownerAddressSet)) {
                Set<String> result = new HashSet<>();
                for (TransactionWrapper transactionWrapper : repushTransactions) {
                    filterOwnerAddress(transactionWrapper, result);
                }
                for (TransactionWrapper transactionWrapper : pushTransactionQueue) {
                    filterOwnerAddress(transactionWrapper, result);
                }
                ownerAddressSet.clear();
                ownerAddressSet.addAll(result);
            }
        }
        logger.info("pushBlock block number:{}, cost/txs:{}/{}",
                block.getNum(),
                System.currentTimeMillis() - start,
                block.getTransactions().size());
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
                                .getUnchecked(
                                        StringUtil.createDbKey(witnessController.getScheduledWitness(i)));
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

        logger.info("update head, num = {}", block.getNum());
        this.dynamicPropertiesStore.saveLatestBlockHeaderHash(block.getBlockId().getByteString());

        this.dynamicPropertiesStore.saveLatestBlockHeaderNumber(block.getNum());
        this.dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(block.getTimeStamp());
        revokingStore.setMaxSize((int) (dynamicPropertiesStore.getLatestBlockHeaderNumber()
                - dynamicPropertiesStore.getLatestConfirmedBlockNum()
                + 1));
        khaosDb.setMaxSize((int)
                (dynamicPropertiesStore.getLatestBlockHeaderNumber()
                        - dynamicPropertiesStore.getLatestConfirmedBlockNum()
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

        LinkedList<KhaosBlock> blockWrappers = branch.getValue();

        if (blockWrappers.isEmpty()) {
            logger.info("empty branch {}", forkBlockHash);
            return Lists.newLinkedList();
        }

        LinkedList<BlockId> result = blockWrappers.stream()
                .map(KhaosBlock::getBlk)
                .map(BlockWrapper::getBlockId)
                .collect(Collectors.toCollection(LinkedList::new));

        result.add(blockWrappers.peekLast().getBlk().getParentBlockId());

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
        } catch (ItemNotFoundException | BadItemException e) {
            return false;
        }
    }

    public boolean containBlockInMainChain(BlockId blockId) {
        try {
            return blockStore.get(blockId.getBytes()) != null;
        } catch (ItemNotFoundException | BadItemException e) {
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
        BlockWrapper block = this.khaosDb.getBlock(hash);
        if (block == null) {
            block = blockStore.get(hash.getBytes());
        }
        return block;
    }

    /**
     * judge has blocks.
     */
    public boolean hasBlocks() {
        return blockStore.iterator().hasNext() || this.khaosDb.hasData();
    }

    /**
     * Process transaction.
     */
    public TransactionInfo processTransaction(final TransactionWrapper trxCap, BlockWrapper blockCap)
            throws ValidateSignatureException, ContractValidateException, ContractExeException,
            AccountResourceInsufficientException, TransactionExpirationException, TooBigTransactionException, TooBigTransactionResultException,
            DupTransactionException, TaposException, ReceiptCheckErrException, VMIllegalException {
        if (trxCap == null) {
            return null;
        }

        validateTapos(trxCap);
        validateCommon(trxCap);

        if (trxCap.getInstance().getRawData().getContractList().size() != 1) {
            throw new ContractSizeNotEqualToOneException(
                    "act size should be exactly 1, this is extend feature");
        }

        validateDup(trxCap);

        if (!trxCap.validateSignature(this)) {
            throw new ValidateSignatureException("trans sig validate failed");
        }

        TransactionTrace trace = new TransactionTrace(trxCap, this);
        trxCap.setTrxTrace(trace);

        consumeNet(trxCap, trace);
        consumeMultiSignFee(trxCap, trace);

//        VMConfig.initVmHardFork();
        VMConfig.initAllowMultiSign(dynamicPropertiesStore.getAllowMultiSign());
        VMConfig.initAllowGvmTransferGrc10(dynamicPropertiesStore.getAllowGvmTransferGrc10());
        VMConfig.initAllowGvmConstantinople(dynamicPropertiesStore.getAllowGvmConstantinople());
        trace.init(blockCap, eventPluginLoaded);
        trace.checkIsConstant();
        trace.exec();

        if (Objects.nonNull(blockCap)) {
            trace.setResult();
            if (!blockCap.getInstance().getBlockHeader().getWitnessSignature().isEmpty()) {
                if (trace.checkNeedRetry()) {
                    String txId = Hex.toHexString(trxCap.getTransactionId().getBytes());
                    logger.info("Retry for tx id: {}", txId);
                    trace.init(blockCap, eventPluginLoaded);
                    trace.checkIsConstant();
                    trace.exec();
                    trace.setResult();
                    logger.info("Retry result for tx id: {}, tx resultCode in receipt: {}",
                            txId, trace.getReceipt().getResult());
                }
                trace.check();
            }
        }

        trace.finalization();
        if (Objects.nonNull(blockCap) && getDynamicPropertiesStore().supportVM()) {
            trxCap.setResult(trace.getRuntime());
        }
        transactionStore.put(trxCap.getTransactionId().getBytes(), trxCap);

        Optional.ofNullable(transactionCache)
                .ifPresent(t -> t.put(trxCap.getTransactionId().getBytes(),
                        new BytesWrapper(ByteArray.fromLong(trxCap.getBlockNum()))));

        TransactionInfoWrapper transactionInfo = TransactionInfoWrapper
                .buildInstance(trxCap, blockCap, trace);

        // if event subscribe is enabled, post contract triggers to queue
        postContractTrigger(trace, false);
        Contract contract = trxCap.getInstance().getRawData().getContract(0);
        if (isMultSignTransaction(trxCap.getInstance())) {
            ownerAddressSet.add(ByteArray.toHexString(TransactionWrapper.getOwner(contract)));
        }

        return transactionInfo.getInstance();
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
            final WitnessWrapper witnessWrapper, final long when, final byte[] privateKey,
            Boolean lastHeadBlockIsMaintenanceBefore, Boolean needCheckWitnessPermission)
            throws ValidateSignatureException, ContractValidateException, ContractExeException,
            UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException {

        //check that the first block after the maintenance period has just been processed
        // if (lastHeadBlockIsMaintenanceBefore != lastHeadBlockIsMaintenance()) {
        if (!witnessController.validateWitnessSchedule(witnessWrapper.getAddress(), when)) {
            logger.info("It's not my turn, "
                    + "and the first block after the maintenance period has just been processed.");

            logger.info("when:{},lastHeadBlockIsMaintenanceBefore:{},lastHeadBlockIsMaintenanceAfter:{}",
                    when, lastHeadBlockIsMaintenanceBefore, lastHeadBlockIsMaintenance());

            return null;
        }
        // }

        final long timestamp = this.dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
        final long number = this.dynamicPropertiesStore.getLatestBlockHeaderNumber();
        final Sha256Hash preHash = this.dynamicPropertiesStore.getLatestBlockHeaderHash();

        // judge create block time
        if (when < timestamp) {
            throw new IllegalArgumentException("generate block timestamp is invalid.");
        }

        long postponedTrxCount = 0;

        ByteString extraData = ByteString.copyFrom(Args.getInstance().getBlockExtraData().getBytes());
        final BlockWrapper blockWrapper =
                new BlockWrapper(number + 1, preHash, when, extraData, witnessWrapper.getAddress());
        blockWrapper.generatedByMyself = true;
        session.reset();
        session.setValue(revokingStore.buildSession());
        //
        accountStateCallBack.preExecute(blockWrapper);

        if (needCheckWitnessPermission && !witnessService.
                validateWitnessPermission(witnessWrapper.getAddress())) {
            logger.warn("Witness permission is wrong");
            return null;
        }
        TransactionRetWrapper transactionRetWrapper =
                new TransactionRetWrapper(blockWrapper);

        Set<String> accountSet = new HashSet<>();
        Iterator<TransactionWrapper> iterator = pendingTransactions.iterator();
        while (iterator.hasNext() || repushTransactions.size() > 0) {
            boolean fromPending = false;
            TransactionWrapper trx;
            if (iterator.hasNext()) {
                fromPending = true;
                trx = (TransactionWrapper) iterator.next();
            } else {
                trx = repushTransactions.poll();
            }

            if (DateTime.now().getMillis() - when
                    > ChainConstant.BLOCK_PRODUCED_INTERVAL * 0.5
                    * Args.getInstance().getBlockProducedTimeOut()
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

            //
            Contract contract = trx.getInstance().getRawData().getContract(0);
            byte[] owner = TransactionWrapper.getOwner(contract);
            String ownerAddress = ByteArray.toHexString(owner);
            if (accountSet.contains(ownerAddress)) {
                continue;
            } else {
                if (isMultSignTransaction(trx.getInstance())) {
                    accountSet.add(ownerAddress);
                }
            }
            if (ownerAddressSet.contains(ownerAddress)) {
                trx.setVerified(false);
            }
            // apply transaction
            try (ISession tmpSeesion = revokingStore.buildSession()) {
                accountStateCallBack.preExeTrans();
                TransactionInfo result = processTransaction(trx, blockWrapper);
                accountStateCallBack.exeTransFinish();
                tmpSeesion.merge();
                // push into block
                blockWrapper.addTransaction(trx);

                if (Objects.nonNull(result)) {
                    transactionRetWrapper.addTransactionInfo(result);
                }
                if (fromPending) {
                    iterator.remove();
                }
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
            } catch (ReceiptCheckErrException e) {
                logger.info("OutOfSlotTime exception: {}", e.getMessage());
                logger.debug(e.getMessage(), e);
            } catch (VMIllegalException e) {
                logger.warn(e.getMessage(), e);
            }
        } // end of while

        accountStateCallBack.executeGenerateFinish();

        session.reset();
        if (postponedTrxCount > 0) {
            logger.info("{} transactions over the block size limit", postponedTrxCount);
        }

        logger.info(
                "postponedTrxCount[" + postponedTrxCount + "],TrxLeft[" + pendingTransactions.size()
                        + "],repushTrxCount[" + repushTransactions.size() + "]");

        blockWrapper.setMerkleRoot();
        blockWrapper.sign(privateKey);
        blockWrapper.setResult(transactionRetWrapper);

        if (gscNetService != null) {
            gscNetService.fastForward(new BlockMessage(blockWrapper));
        }

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
        } catch (ReceiptCheckErrException e) {
            logger.info("OutOfSlotTime exception: {}", e.getMessage());
            logger.debug(e.getMessage(), e);
        } catch (VMIllegalException e) {
            logger.warn(e.getMessage(), e);
        } catch (TooBigTransactionResultException e) {
            logger.info("contract not processed during TooBigTransactionResultException");
        }

        return null;
    }

    private void filterOwnerAddress(TransactionWrapper transactionWrapper, Set<String> result) {
        Contract contract = transactionWrapper.getInstance().getRawData().getContract(0);
        byte[] owner = TransactionWrapper.getOwner(contract);
        String ownerAddress = ByteArray.toHexString(owner);
        if (ownerAddressSet.contains(ownerAddress)) {
            result.add(ownerAddress);
        }
    }

    private boolean isMultSignTransaction(Transaction transaction) {
        Contract contract = transaction.getRawData().getContract(0);
        switch (contract.getType()) {
            case AccountPermissionUpdateContract: {
                return true;
            }
            default:
        }
        return false;
    }

    public TransactionStore getTransactionStore() {
        return this.transactionStore;
    }

    public TransactionHistoryStore getTransactionHistoryStore() {
        return this.transactionHistoryStore;
    }

    public BlockStore getBlockStore() {
        return this.blockStore;
    }


    /**
     * process block.
     */
    public void processBlock(BlockWrapper block)
            throws ValidateSignatureException, ContractValidateException, ContractExeException,
            AccountResourceInsufficientException, TaposException, TooBigTransactionException,
            DupTransactionException, TransactionExpirationException, ValidateScheduleException,
            ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException, BadBlockException {
        // todo set revoking db max size.

        // checkWitness
        if (!witnessController.validateWitnessSchedule(block)) {
            throw new ValidateScheduleException("validateWitnessSchedule error");
        }
        //reset BlockCpuUsage
        this.dynamicPropertiesStore.saveBlockCpuUsage(0);
        //parallel check sign
        if (!block.generatedByMyself) {
            try {
                preValidateTransactionSign(block);
            } catch (InterruptedException e) {
                logger.error("parallel check sign interrupted exception! block info: {}", block, e);
                Thread.currentThread().interrupt();
            }
        }

        TransactionRetWrapper transactionRetWrapper =
                new TransactionRetWrapper(block);

        try {
            accountStateCallBack.preExecute(block);
            for (TransactionWrapper transactionWrapper : block.getTransactions()) {
                transactionWrapper.setBlockNum(block.getNum());
                if (block.generatedByMyself) {
                    transactionWrapper.setVerified(true);
                }
                accountStateCallBack.preExeTrans();
                TransactionInfo result = processTransaction(transactionWrapper, block);
                accountStateCallBack.exeTransFinish();
                if (Objects.nonNull(result)) {
                    transactionRetWrapper.addTransactionInfo(result);
                }
            }
            accountStateCallBack.executePushFinish();
        } finally {
            accountStateCallBack.exceptionFinish();
        }

        block.setResult(transactionRetWrapper);
        boolean needMaint = needMaintenance(block.getTimeStamp());
        if (needMaint) {
            if (block.getNum() == 1) {
                this.dynamicPropertiesStore.updateNextMaintenanceTime(block.getTimeStamp());
            } else {
                this.processMaintenance(block);
            }
        }
        if (getDynamicPropertiesStore().getAllowAdaptiveCpu() == 1) {
            CpuProcessor cpuProcessor = new CpuProcessor(this);
            cpuProcessor.updateTotalCpuAverageUsage();
            cpuProcessor.updateAdaptiveTotalCpuLimit();
        }
        updateSignedWitness(block);
        updateLatestConfirmedBlock();
        updateTransHashCache(block);
        updateMaintenanceState(needMaint);
        updateRecentBlock(block);
        updateDynamicProperties(block);
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
     * update the latest confirmed block.
     */
    public void updateLatestConfirmedBlock() {
        List<Long> numbers =
                witnessController
                        .getActiveWitnesses()
                        .stream()
                        .map(address -> witnessController.getWitnesseByAddress(address)
                                .getLatestBlockNum())
                        .sorted()
                        .collect(Collectors.toList());

        long size = witnessController.getActiveWitnesses().size();
        int confirmedPosition = (int) (size * (1 - CONFIRMED_THRESHOLD * 1.0 / 100));
        if (confirmedPosition < 0) {
            logger.warn(
                    "updateLatestConfirmedBlock error, confirmedPosition:{},wits.size:{}",
                    confirmedPosition,
                    size);
            return;
        }
        long latestConfirmedBlockNum = numbers.get(confirmedPosition);
        //if current value is less than the previous value，keep the previous value.
        if (latestConfirmedBlockNum < getDynamicPropertiesStore().getLatestConfirmedBlockNum()) {
            logger.warn("latestConfirmedBlockNum = 0,LatestBlockNum:{}", numbers);
            return;
        }

        getDynamicPropertiesStore().saveLatestConfirmedBlockNum(latestConfirmedBlockNum);
        this.latestConfirmedBlockNumber = latestConfirmedBlockNum;
        logger.info("update confirmed block, num = {}", latestConfirmedBlockNum);
    }

    public void updateFork(BlockWrapper block) {
        forkController.update(block);
    }

    public long getSyncBeginNumber() {
        logger.info("headNumber:" + dynamicPropertiesStore.getLatestBlockHeaderNumber());
        logger.info(
                "syncBeginNumber:"
                        + (dynamicPropertiesStore.getLatestBlockHeaderNumber() - revokingStore.size()));
        logger.info("confirmedBlockNumber:" + dynamicPropertiesStore.getLatestConfirmedBlockNum());
        return dynamicPropertiesStore.getLatestBlockHeaderNumber() - revokingStore.size();
    }

    public BlockId getConfirmedBlockId() {
        try {
            long num = dynamicPropertiesStore.getLatestConfirmedBlockNum();
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
     *              block num 2. pay the gsc to witness. 3. the latest slot num.
     */
    public void updateSignedWitness(BlockWrapper block) {
        // TODO: add verification
        WitnessWrapper witnessWrapper =
                witnessStore.getUnchecked(
                        block.getInstance().getBlockHeader().getRawData().getWitnessAddress()
                                .toByteArray());
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

    public AssetIssueV2Store getAssetIssueV2Store() {
        return assetIssueV2Store;
    }

    public AssetIssueStore getAssetIssueStoreFinal() {
        if (getDynamicPropertiesStore().getAllowSameTokenName() == 0) {
            return getAssetIssueStore();
        } else {
            return getAssetIssueV2Store();
        }
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

    public AccountIndexStore getAccountIndexStore() {
        return this.accountIndexStore;
    }

    public void setAccountIndexStore(AccountIndexStore indexStore) {
        this.accountIndexStore = indexStore;
    }

    public void closeAllStore() {
        logger.info("******** begin to close db ********");
        closeOneStore(accountStore);
        closeOneStore(blockStore);
        closeOneStore(blockIndexStore);
        closeOneStore(accountIdIndexStore);
        closeOneStore(accountIndexStore);
        closeOneStore(witnessStore);
        closeOneStore(witnessScheduleStore);
        closeOneStore(assetIssueStore);
        closeOneStore(dynamicPropertiesStore);
        closeOneStore(transactionStore);
        closeOneStore(codeStore);
        closeOneStore(contractStore);
        closeOneStore(storageRowStore);
        closeOneStore(exchangeStore);
        closeOneStore(peersStore);
        closeOneStore(proposalStore);
        closeOneStore(recentBlockStore);
        closeOneStore(transactionHistoryStore);
        closeOneStore(votesStore);
        closeOneStore(delegatedResourceStore);
        closeOneStore(delegatedResourceAccountIndexStore);
        closeOneStore(assetIssueV2Store);
        closeOneStore(exchangeV2Store);
        closeOneStore(transactionRetStore);
        logger.info("******** end to close db ********");
    }

    public void closeOneStore(IGSCChainBase database) {
        logger.info("******** begin to close " + database.getName() + " ********");
        try {
            database.close();
        } catch (Exception e) {
            logger.info("failed to close  " + database.getName() + ". " + e);
        } finally {
            logger.info("******** end to close " + database.getName() + " ********");
        }
    }

    public boolean isTooManyPending() {
        return getPendingTransactions().size() + getRepushTransactions().size()
                > MAX_TRANSACTION_PENDING;
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
        private Manager manager;

        ValidateSignTask(TransactionWrapper trx, CountDownLatch countDownLatch,
                         Manager manager) {
            this.trx = trx;
            this.countDownLatch = countDownLatch;
            this.manager = manager;
        }

        @Override
        public Boolean call() throws ValidateSignatureException {
            try {
                trx.validateSignature(manager);
            } catch (ValidateSignatureException e) {
                throw e;
            } finally {
                countDownLatch.countDown();
            }
            return true;
        }
    }

    public void preValidateTransactionSign(BlockWrapper block)
            throws InterruptedException, ValidateSignatureException {
        logger.info("PreValidate Transaction Sign, size:" + block.getTransactions().size()
                + ",block num:" + block.getNum());
        int transSize = block.getTransactions().size();
        if (transSize <= 0) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(transSize);
        List<Future<Boolean>> futures = new ArrayList<>(transSize);

        for (TransactionWrapper transaction : block.getTransactions()) {
            Future<Boolean> future = validateSignService
                    .submit(new ValidateSignTask(transaction, countDownLatch, this));
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
        if (containsTransaction(tx)) {
            return;
        }

        try {
            this.pushTransaction(tx);
        } catch (ValidateSignatureException | ContractValidateException | ContractExeException
                | AccountResourceInsufficientException | VMIllegalException e) {
            logger.debug(e.getMessage(), e);
        } catch (DupTransactionException e) {
            logger.debug("pending manager: dup trans", e);
        } catch (TaposException e) {
            logger.debug("pending manager: tapos exception", e);
        } catch (TooBigTransactionException e) {
            logger.debug("too big transaction");
        } catch (TransactionExpirationException e) {
            logger.debug("expiration transaction");
        } catch (ReceiptCheckErrException e) {
            logger.debug("outOfSlotTime transaction");
        } catch (TooBigTransactionResultException e) {
            logger.debug("too big transaction result");
        }
    }

    public void setMode(boolean mode) {
        revokingStore.setMode(mode);
    }

    private void startEventSubscribing() {

        try {
            eventPluginLoaded = EventPluginLoader.getInstance()
                    .start(Args.getInstance().getEventPluginConfig());

            if (!eventPluginLoaded) {
                logger.error("failed to load eventPlugin");
            }

            FilterQuery eventFilter = Args.getInstance().getEventFilter();
            if (!Objects.isNull(eventFilter)) {
                EventPluginLoader.getInstance().setFilterQuery(eventFilter);
            }

        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    private void postBlockTrigger(final BlockWrapper newBlock) {
        if (eventPluginLoaded && EventPluginLoader.getInstance().isBlockLogTriggerEnable()) {
            BlockLogTriggerWrapper blockLogTriggerWrapper = new BlockLogTriggerWrapper(newBlock);
            blockLogTriggerWrapper.setLatestConfirmedBlockNumber(latestConfirmedBlockNumber);
            boolean result = triggerWrapperQueue.offer(blockLogTriggerWrapper);
            if (!result) {
                logger.info("too many trigger, lost block trigger: {}", newBlock.getBlockId());
            }
        }

        for (TransactionWrapper e : newBlock.getTransactions()) {
            postTransactionTrigger(e, newBlock);
        }
    }

    private void postTransactionTrigger(final TransactionWrapper trxCap,
                                        final BlockWrapper blockCap) {
        if (eventPluginLoaded && EventPluginLoader.getInstance().isTransactionLogTriggerEnable()) {
            TransactionLogTriggerWrapper trx = new TransactionLogTriggerWrapper(trxCap, blockCap);
            trx.setLatestConfirmedBlockNumber(latestConfirmedBlockNumber);
            boolean result = triggerWrapperQueue.offer(trx);
            if (!result) {
                logger.info("too many trigger, lost transaction trigger: {}", trxCap.getTransactionId());
            }
        }
    }

    private void reorgContractTrigger() {
        if (eventPluginLoaded &&
                (EventPluginLoader.getInstance().isContractEventTriggerEnable()
                        || EventPluginLoader.getInstance().isContractLogTriggerEnable())) {
            logger.info("switchfork occured, post reorgContractTrigger");
            try {
                BlockWrapper oldHeadBlock = getBlockById(
                        getDynamicPropertiesStore().getLatestBlockHeaderHash());
                for (TransactionWrapper trx : oldHeadBlock.getTransactions()) {
                    postContractTrigger(trx.getTrxTrace(), true);
                }
            } catch (BadItemException | ItemNotFoundException e) {
                logger.error("block header hash not exists or bad: {}",
                        getDynamicPropertiesStore().getLatestBlockHeaderHash());
            }
        }
    }

    private void postContractTrigger(final TransactionTrace trace, boolean remove) {
        if (eventPluginLoaded &&
                (EventPluginLoader.getInstance().isContractEventTriggerEnable()
                        || EventPluginLoader.getInstance().isContractLogTriggerEnable())) {
            // be careful, trace.getRuntimeResult().getTriggerList() should never return null
            for (ContractTrigger trigger : trace.getRuntimeResult().getTriggerList()) {
                ContractTriggerWrapper contractEventTriggerWrapper = new ContractTriggerWrapper(trigger);
                contractEventTriggerWrapper.getContractTrigger().setRemoved(remove);
                contractEventTriggerWrapper.setLatestConfirmedBlockNumber(latestConfirmedBlockNumber);
                if (!triggerWrapperQueue.offer(contractEventTriggerWrapper)) {
                    logger.info("too many tigger, lost contract log trigger: {}", trigger.getTransactionId());
                }
            }
        }
    }
}
