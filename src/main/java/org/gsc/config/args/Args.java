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
package org.gsc.config.args;

import static java.lang.Math.max;
import static java.lang.System.exit;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyServerBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;
import org.gsc.crypto.ECKey;
import org.gsc.runtime.event.EventPluginConfig;
import org.gsc.runtime.event.FilterQuery;
import org.gsc.runtime.event.TriggerConfig;
import org.gsc.net.node.Node;
import org.gsc.db.dbsource.RocksDbSettings;
import org.gsc.utils.ByteArray;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.config.Configuration;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.Parameter.NetConstants;
import org.gsc.config.Parameter.NodeConstant;
import org.gsc.db.AccountStore;
import org.gsc.db.backup.DbBackupConfig;
import org.gsc.keystore.CipherException;
import org.gsc.keystore.Credentials;
import org.gsc.keystore.WalletUtils;
import org.gsc.Version;

@Slf4j(topic = "app")
@NoArgsConstructor
@Component
public class Args {

    private static final Args INSTANCE = new Args();

    @Parameter(names = {"-c", "--config"}, description = "Config File")
    private String shellConfFileName = "";

    @Parameter(names = {"-d", "--db-directory"}, description = "Directory")
    private String outputDirectory = "gsc-db";

    @Getter
    @Parameter(names = {"--log-config"})
    private String logbackPath = "";

    @Getter
    @Parameter(names = {"-h", "--help"}, help = true, description = "HELP message")
    private boolean help = false;

    @Getter
    @Setter
    @Parameter(names = {"-w", "--witness"})
    private boolean witness = false;

    @Getter
    @Setter
    @Parameter(names = {"--support-constant"})
    private boolean supportConstant = false;

    @Getter
    @Setter
    @Parameter(names = {"--debug"})
    private boolean debug = false;

    @Getter
    @Setter
    @Parameter(names = {"--min-time-ratio"})
    private double minTimeRatio = 0.0;

    @Getter
    @Setter
    @Parameter(names = {"--max-time-ratio"})
    private double maxTimeRatio = calcMaxTimeRatio();

    @Getter
    @Setter
    @Parameter(names = {"--long-running-time"})
    private int longRunningTime = 10;

    @Getter
    @Setter
    @Parameter(names = {"--max-connect-number"})
    private int maxHttpConnectNumber = 50;

    @Getter
    @Parameter(description = "--boot-nodes")
    private List<String> bootNodes = new ArrayList<>();

    @Parameter(names = {"-p", "--private-key"}, description = "private-key")
    private String privateKey = "";

    @Parameter(names = {"--witness-address"}, description = "witness-address")
    private String witnessAddress = "";

    @Parameter(names = {"--password"}, description = "password")
    private String password;

    @Parameter(names = {"--storage-db-directory"}, description = "Storage db directory")
    private String storageDbDirectory = "";

    @Parameter(names = {"--storage-db-version"}, description = "Storage db version.(1 or 2)")
    private String storageDbVersion = "";

    @Parameter(names = {
            "--storage-db-engine"}, description = "Storage db engine.(leveldb or rocksdb)")
    private String storageDbEngine = "";

    @Parameter(names = {
            "--storage-db-synchronous"}, description = "Storage db is synchronous or not.(true or false)")
    private String storageDbSynchronous = "";

    @Parameter(names = {
            "--contract-parse-enable"}, description = "enable contract parses in java-gsc or not.(true or false)")
    private String contractParseEnable = "";

    @Parameter(names = {"--storage-index-directory"}, description = "Storage index directory")
    private String storageIndexDirectory = "";

    @Parameter(names = {"--storage-index-switch"}, description = "Storage index switch.(on or off)")
    private String storageIndexSwitch = "";

    @Parameter(names = {
            "--storage-transactionHistory-switch"}, description = "Storage transaction history switch.(on or off)")
    private String storageTransactionHistoreSwitch = "";

    @Getter
    @Parameter(names = {"--fast-forward"})
    private boolean fastForward = false;

    @Getter
    private Storage storage;

    @Getter
    private Overlay overlay;

    @Getter
    private GenesisBlock genesisBlock;

    @Getter
    private String blockExtraData;

    @Getter
    @Setter
    private String chainId;

    @Getter
    @Setter
    private LocalWitnesses localWitnesses = new LocalWitnesses();

    @Getter
    @Setter
    private boolean syncEnabled;

    @Getter
    @Setter
    private boolean nodeDiscoveryEnable;

    @Getter
    @Setter
    private boolean nodeDiscoveryPersist;

    @Getter
    @Setter
    private int nodeConnectionTimeout;

    @Getter
    @Setter
    private List<Node> activeNodes;

    @Getter
    @Setter
    private List<Node> passiveNodes;

    @Getter
    @Setter
    private List<Node> fastForwardNodes;

    @Getter
    @Setter
    private int nodeChannelReadTimeout;

    @Getter
    @Setter
    private int nodeMaxActiveNodes;

    @Getter
    @Setter
    private int nodeMaxActiveNodesWithSameIp;

    @Getter
    @Setter
    private int minParticipationRate;

    @Getter
    @Setter
    private int nodeListenPort;

    @Getter
    @Setter
    private String nodeDiscoveryBindIp;

    @Getter
    @Setter
    private String nodeExternalIp;

    @Getter
    @Setter
    private boolean nodeDiscoveryPublicHomeNode;

    @Getter
    @Setter
    private long nodeP2pPingInterval;

    @Getter
    @Setter
    @Parameter(names = {"--save-internaltx"})
    private boolean saveInternalTx;

    @Getter
    @Setter
    private int nodeP2pVersion;

    @Getter
    @Setter
    private String p2pNodeId;

    //If you are running a confirmed node for java gsc, this flag is set to true
    @Getter
    @Setter
    private boolean confirmedNode = false;

    @Getter
    @Setter
    private int rpcPort;

    @Getter
    @Setter
    private int rpcOnConfirmedPort;

    @Getter
    @Setter
    private int fullNodeHttpPort;

    @Getter
    @Setter
    private int confirmedHttpPort;

    @Getter
    @Setter
    @Parameter(names = {"--rpc-thread"}, description = "Num of gRPC thread")
    private int rpcThreadNum;

    @Getter
    @Setter
    @Parameter(names = {"--confirmed-thread"}, description = "Num of confirmed thread")
    private int confirmedThreadNum;

    @Getter
    @Setter
    private int maxConcurrentCallsPerConnection;

    @Getter
    @Setter
    private int flowControlWindow;

    @Getter
    @Setter
    private long maxConnectionIdleInMillis;

    @Getter
    @Setter
    private int blockProducedTimeOut;

    @Getter
    @Setter
    private long netMaxTrxPerSecond;

    @Getter
    @Setter
    private long maxConnectionAgeInMillis;

    @Getter
    @Setter
    private int maxMessageSize;

    @Getter
    @Setter
    private int maxHeaderListSize;

    @Getter
    @Setter
    @Parameter(names = {"--validate-sign-thread"}, description = "Num of validate thread")
    private int validateSignThreadNum;

    @Getter
    @Setter
    private long maintenanceTimeInterval; // (ms)

    @Getter
    @Setter
    private long proposalExpireTime; // (ms)

    @Getter
    @Setter
    private int checkFrozenTime; // for test only

    @Getter
    @Setter
    private long allowCreationOfContracts; //committee parameter

    @Getter
    @Setter
    private long allowAdaptiveCpu; //committee parameter

    @Getter
    @Setter
    private long allowDelegateResource; //committee parameter

    @Getter
    @Setter
    private long allowSameTokenName; //committee parameter

    @Getter
    @Setter
    private long allowGvmTransferGrc10; //committee parameter

    @Getter
    @Setter
    private long allowGvmConstantinople; //committee parameter

    @Getter
    @Setter
    private long allowUpdateAccountName; //committee parameter

    @Getter
    @Setter
    private int tcpNettyWorkThreadNum;

    @Getter
    @Setter
    private int udpNettyWorkThreadNum;

    @Getter
    @Setter
    @Parameter(names = {"--trust-node"}, description = "Trust node addr")
    private String trustNodeAddr;

    @Getter
    @Setter
    private boolean walletExtensionApi;

    @Getter
    @Setter
    private int backupPriority;

    @Getter
    @Setter
    private int backupPort;

    @Getter
    @Setter
    private List<String> backupMembers;

    @Getter
    @Setter
    private double connectFactor;

    @Getter
    @Setter
    private double activeConnectFactor;

    @Getter
    @Setter
    private double disconnectNumberFactor;

    @Getter
    @Setter
    private double maxConnectNumberFactor;

    @Getter
    @Setter
    private long receiveTcpMinDataLength;

    @Getter
    @Setter
    private boolean isOpenFullTcpDisconnect;

    @Getter
    @Setter
    private int allowMultiSign;

    @Getter
    @Setter
    private boolean vmTrace;

    @Getter
    @Setter
    private boolean needToUpdateAsset;

    @Getter
    @Setter
    private String trxReferenceBlock;

    @Getter
    @Setter
    private int minEffectiveConnection;

//    @Getter
//    @Setter
//    private long blockNumForEneryLimit;

    @Getter
    @Setter
    @Parameter(names = {"--es"})
    private boolean eventSubscribe = false;

    @Getter
    private EventPluginConfig eventPluginConfig;

    @Getter
    private FilterQuery eventFilter;

    @Getter
    @Setter
    private long trxExpirationTimeInMilliseconds; // (ms)

    @Getter
    private DbBackupConfig dbBackupConfig;

    @Getter
    private RocksDbSettings rocksDBCustomSettings;

    @Parameter(names = {"-v", "--version"}, description = "output code version", help = true)
    private boolean version;

    @Getter
    @Setter
    private long allowProtoFilterNum;

    @Getter
    @Setter
    private long allowAccountStateRoot;

    @Getter
    @Setter
    private int validContractProtoThreadNum;

    public static void clearParam() {
        INSTANCE.outputDirectory = "gsc-db";
        INSTANCE.help = false;
        INSTANCE.witness = false;
        INSTANCE.bootNodes = new ArrayList<>();
        INSTANCE.privateKey = "";
        INSTANCE.witnessAddress = "";
        INSTANCE.storageDbDirectory = "";
        INSTANCE.storageIndexDirectory = "";
        INSTANCE.storageIndexSwitch = "";

        // FIXME: INSTANCE.storage maybe null ?
        if (INSTANCE.storage != null) {
            // WARNING: WILL DELETE DB STORAGE PATHS
            INSTANCE.storage.deleteAllStoragePaths();
            INSTANCE.storage = null;
        }

        INSTANCE.overlay = null;
        INSTANCE.genesisBlock = null;
        INSTANCE.chainId = null;
        INSTANCE.localWitnesses = null;
        INSTANCE.syncEnabled = false;
        INSTANCE.nodeDiscoveryEnable = false;
        INSTANCE.nodeDiscoveryPersist = false;
        INSTANCE.nodeConnectionTimeout = 0;
        INSTANCE.activeNodes = Collections.emptyList();
        INSTANCE.passiveNodes = Collections.emptyList();
        INSTANCE.fastForwardNodes = Collections.emptyList();
        INSTANCE.nodeChannelReadTimeout = 0;
        INSTANCE.nodeMaxActiveNodes = 30;
        INSTANCE.nodeMaxActiveNodesWithSameIp = 2;
        INSTANCE.minParticipationRate = 0;
        INSTANCE.nodeListenPort = 0;
        INSTANCE.nodeDiscoveryBindIp = "";
        INSTANCE.nodeExternalIp = "";
        INSTANCE.nodeDiscoveryPublicHomeNode = false;
        INSTANCE.nodeP2pPingInterval = 0L;
        INSTANCE.nodeP2pVersion = 0;
        INSTANCE.rpcPort = 0;
        INSTANCE.rpcOnConfirmedPort = 0;
        INSTANCE.fullNodeHttpPort = 0;
        INSTANCE.confirmedHttpPort = 0;
        INSTANCE.maintenanceTimeInterval = 0;
        INSTANCE.proposalExpireTime = 0;
        INSTANCE.checkFrozenTime = 1;
        INSTANCE.allowCreationOfContracts = 0;
        INSTANCE.allowAdaptiveCpu = 0;
        INSTANCE.allowGvmTransferGrc10 = 0;
        INSTANCE.allowGvmConstantinople = 0;
        INSTANCE.allowDelegateResource = 0;
        INSTANCE.allowSameTokenName = 1;
        INSTANCE.allowUpdateAccountName = 1;
        INSTANCE.tcpNettyWorkThreadNum = 0;
        INSTANCE.udpNettyWorkThreadNum = 0;
        INSTANCE.p2pNodeId = "";
        INSTANCE.confirmedNode = false;
        INSTANCE.trustNodeAddr = "";
        INSTANCE.walletExtensionApi = false;
        INSTANCE.connectFactor = 0.3;
        INSTANCE.activeConnectFactor = 0.1;
        INSTANCE.disconnectNumberFactor = 0.4;
        INSTANCE.maxConnectNumberFactor = 0.8;
        INSTANCE.receiveTcpMinDataLength = 2048;
        INSTANCE.isOpenFullTcpDisconnect = false;
        INSTANCE.supportConstant = false;
        INSTANCE.debug = false;
        INSTANCE.minTimeRatio = 0.0;
        INSTANCE.maxTimeRatio = 5.0;
        INSTANCE.longRunningTime = 10;
        INSTANCE.maxHttpConnectNumber = 50;
        INSTANCE.allowMultiSign = 0;
        INSTANCE.trxExpirationTimeInMilliseconds = 0;
        INSTANCE.allowProtoFilterNum = 0;
        INSTANCE.allowAccountStateRoot = 0;
        INSTANCE.validContractProtoThreadNum = 1;
    }

    /**
     * set parameters.
     */
    public static void setParam(final String[] args, final String confFileName) {
        JCommander.newBuilder().addObject(INSTANCE).build().parse(args);
        if (INSTANCE.version) {
            JCommander.getConsole()
                    .println(Version.getVersion() + "\n" + Version.versionName + "\n" + Version.versionCode);
            exit(0);
        }

        Config config = Configuration.getByFileName(INSTANCE.shellConfFileName, confFileName);

        Wallet.setAddressPreFixByte(Constant.ADD_PRE_FIX_BYTE);
        Wallet.setAddressPreFixString(Constant.ADD_PRE_FIX_STRING);

        if (config.hasPath("witness.mining")){
            INSTANCE.witness = config.getBoolean("witness.mining");
        }

        if (StringUtils.isNoneBlank(INSTANCE.privateKey)) {
            INSTANCE.setLocalWitnesses(new LocalWitnesses(INSTANCE.privateKey));
            if (StringUtils.isNoneBlank(INSTANCE.witnessAddress)) {
                byte[] bytes = Wallet.decodeFromBase58Check(INSTANCE.witnessAddress);
                if (bytes != null) {
                    INSTANCE.localWitnesses.setWitnessAccountAddress(bytes);
                    logger.debug("Got witness address from cmd");
                } else {
                    INSTANCE.witnessAddress = "";
                    logger.warn("The witness address format is incorrect, ignored");
                }
            }
            INSTANCE.localWitnesses.initWitnessAccountAddress();
            logger.debug("Got privateKey from cmd");
        } else if (config.hasPath("witness.privateKey")) {
            INSTANCE.localWitnesses = new LocalWitnesses();
            List<String> localwitness = config.getStringList("witness.privateKey");
            if (localwitness.size() > 1) {
                logger.warn("witness size must be one, get the first one");
                localwitness = localwitness.subList(0, 1);
            }
            INSTANCE.localWitnesses.setPrivateKeys(localwitness);

            if (config.hasPath("witness.address")) {
                byte[] bytes = Wallet.decodeFromBase58Check(config.getString("witness.address"));
                if (bytes != null) {
                    INSTANCE.localWitnesses.setWitnessAccountAddress(bytes);
                    logger.debug("Got witness address from config.conf");
                } else {
                    logger.warn("The witness address format is incorrect, ignored");
                }
            }
            INSTANCE.localWitnesses.initWitnessAccountAddress();

            logger.debug("Got privateKey from config.conf");
        } else if (config.hasPath("witness.keystore")) {
            INSTANCE.localWitnesses = new LocalWitnesses();
            List<String> privateKeys = new ArrayList<String>();
            if (INSTANCE.isWitness()) {
                List<String> localwitness = config.getStringList("witness.keystore");
                if (localwitness.size() > 0) {
                    String fileName = System.getProperty("user.dir") + "/" + localwitness.get(0);
                    String password;
                    if (StringUtils.isEmpty(INSTANCE.password)) {
                        System.out.println("Please input your password.");
                        password = WalletUtils.inputPassword();
                    } else {
                        password = INSTANCE.password;
                        INSTANCE.password = null;
                    }

                    try {
                        Credentials credentials = WalletUtils
                                .loadCredentials(password, new File(fileName));
                        ECKey ecKeyPair = credentials.getEcKeyPair();
                        String prikey = ByteArray.toHexString(ecKeyPair.getPrivKeyBytes());
                        privateKeys.add(prikey);
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                        logger.error("Witness node start faild!");
                        exit(-1);
                    } catch (CipherException e) {
                        logger.error(e.getMessage());
                        logger.error("Witness node start faild!");
                        exit(-1);
                    }
                }
            }
            INSTANCE.localWitnesses.setPrivateKeys(privateKeys);
            logger.debug("Got privateKey from keystore");
        }

        if (INSTANCE.isWitness() && CollectionUtils.isEmpty(INSTANCE.localWitnesses.getPrivateKeys())) {
            logger.warn("This is a witness node,but witnesses is null");
        }

        if (config.hasPath("vm.supportConstant")) {
            INSTANCE.supportConstant = config.getBoolean("vm.supportConstant");
        }

        if (config.hasPath("vm.minTimeRatio")) {
            INSTANCE.minTimeRatio = config.getDouble("vm.minTimeRatio");
        }

        if (config.hasPath("vm.maxTimeRatio")) {
            INSTANCE.maxTimeRatio = config.getDouble("vm.maxTimeRatio");
        }

        if (config.hasPath("vm.longRunningTime")) {
            INSTANCE.longRunningTime = config.getInt("vm.longRunningTime");
        }

        INSTANCE.storage = new Storage();
        INSTANCE.storage.setDbVersion(Optional.ofNullable(INSTANCE.storageDbVersion)
                .filter(StringUtils::isNotEmpty)
                .map(Integer::valueOf)
                .orElse(Storage.getDbVersionFromConfig(config)));

        INSTANCE.storage.setDbEngine(Optional.ofNullable(INSTANCE.storageDbEngine)
                .filter(StringUtils::isNotEmpty)
                .orElse(Storage.getDbEngineFromConfig(config)));

        if ("ROCKSDB".equals(INSTANCE.storage.getDbEngine().toUpperCase())
                && INSTANCE.storage.getDbVersion() == 1) {
            throw new RuntimeException("db.version = 1 is not supported by ROCKSDB engine.");
        }

        INSTANCE.storage.setDbSync(Optional.ofNullable(INSTANCE.storageDbSynchronous)
                .filter(StringUtils::isNotEmpty)
                .map(Boolean::valueOf)
                .orElse(Storage.getDbVersionSyncFromConfig(config)));

        INSTANCE.storage.setContractParseSwitch(Optional.ofNullable(INSTANCE.contractParseEnable)
                .filter(StringUtils::isNotEmpty)
                .map(Boolean::valueOf)
                .orElse(Storage.getContractParseSwitchFromConfig(config)));

        INSTANCE.storage.setDbDirectory(Optional.ofNullable(INSTANCE.storageDbDirectory)
                .filter(StringUtils::isNotEmpty)
                .orElse(Storage.getDbDirectoryFromConfig(config)));

        INSTANCE.storage.setIndexDirectory(Optional.ofNullable(INSTANCE.storageIndexDirectory)
                .filter(StringUtils::isNotEmpty)
                .orElse(Storage.getIndexDirectoryFromConfig(config)));

        INSTANCE.storage.setIndexSwitch(Optional.ofNullable(INSTANCE.storageIndexSwitch)
                .filter(StringUtils::isNotEmpty)
                .orElse(Storage.getIndexSwitchFromConfig(config)));

        INSTANCE.storage
                .setTransactionHistoreSwitch(Optional.ofNullable(INSTANCE.storageTransactionHistoreSwitch)
                        .filter(StringUtils::isNotEmpty)
                        .orElse(Storage.getTransactionHistoreSwitchFromConfig(config)));

        INSTANCE.storage.setPropertyMapFromConfig(config);

        INSTANCE.bootNodes = Optional.ofNullable(INSTANCE.bootNodes)
                .filter(bootNode -> 0 != bootNode.size())
                .orElse(config.getStringList("node.discovery.boot"));

        if (config.hasPath("genesis.block")) {
            INSTANCE.genesisBlock = new GenesisBlock();

            INSTANCE.genesisBlock.setTimestamp(config.getString("genesis.block.timestamp"));
            INSTANCE.genesisBlock.setParentHash(config.getString("genesis.block.parentHash"));
            INSTANCE.genesisBlock.setExtraData(config.getString("genesis.block.extraData"));

            if (config.hasPath("genesis.block.assets")) {
                INSTANCE.genesisBlock.setAssets(getAccountsFromConfig(config));
                AccountStore.setAccount(config);
            }
            if (config.hasPath("genesis.block.witnesses")) {
                INSTANCE.genesisBlock.setWitnesses(getWitnessesFromConfig(config));
            }
        } else {
            INSTANCE.genesisBlock = GenesisBlock.getDefault();
        }

        INSTANCE.syncEnabled =
                config.hasPath("sync.enabled") && config.getBoolean("sync.enabled");

        INSTANCE.nodeDiscoveryEnable =
                config.hasPath("node.discovery.enable") && config.getBoolean("node.discovery.enable");

        INSTANCE.nodeDiscoveryPersist =
                config.hasPath("node.discovery.persist") && config.getBoolean("node.discovery.persist");

        INSTANCE.nodeConnectionTimeout =
                config.hasPath("node.connection.timeout") ? config.getInt("node.connection.timeout") * 1000
                        : 0;

        INSTANCE.nodeChannelReadTimeout =
                config.hasPath("node.channel.read.timeout") ? config.getInt("node.channel.read.timeout")
                        : 0;

        INSTANCE.nodeMaxActiveNodes =
                config.hasPath("node.maxActiveNodes") ? config.getInt("node.maxActiveNodes") : 30;

        INSTANCE.nodeMaxActiveNodesWithSameIp =
                config.hasPath("node.maxActiveNodesWithSameIp") ? config
                        .getInt("node.maxActiveNodesWithSameIp") : 2;

        INSTANCE.minParticipationRate =
                config.hasPath("node.minParticipationRate") ? config.getInt("node.minParticipationRate")
                        : 0;

        INSTANCE.nodeListenPort =
                config.hasPath("node.listen.port") ? config.getInt("node.listen.port") : 0;

        bindIp(config);
        externalIp(config);

        INSTANCE.nodeDiscoveryPublicHomeNode =
                config.hasPath("node.discovery.public.home.node") && config
                        .getBoolean("node.discovery.public.home.node");

        INSTANCE.nodeP2pPingInterval =
                config.hasPath("node.p2p.pingInterval") ? config.getLong("node.p2p.pingInterval") : 0;

        INSTANCE.nodeP2pVersion =
                config.hasPath("node.p2p.version") ? config.getInt("node.p2p.version") : 0;

        INSTANCE.rpcPort =
                config.hasPath("node.rpc.port") ? config.getInt("node.rpc.port") : 5021;

        INSTANCE.rpcOnConfirmedPort =
                config.hasPath("node.rpc.confirmedPort") ? config.getInt("node.rpc.confirmedPort") : 5023;

        INSTANCE.fullNodeHttpPort =
                config.hasPath("node.http.fullNodePort") ? config.getInt("node.http.fullNodePort") : 8090;

        INSTANCE.confirmedHttpPort =
                config.hasPath("node.http.confirmedPort") ? config.getInt("node.http.confirmedPort") : 8091;

        INSTANCE.rpcThreadNum =
                config.hasPath("node.rpc.threads") ? config.getInt("node.rpc.threads")
                        : Runtime.getRuntime().availableProcessors() / 2;

        INSTANCE.confirmedThreadNum =
                config.hasPath("node.rpc.confirmedThreads") ? config.getInt("node.rpc.confirmedThreads")
                        : Runtime.getRuntime().availableProcessors();

        INSTANCE.maxConcurrentCallsPerConnection =
                config.hasPath("node.rpc.maxConcurrentCallsPerConnection") ?
                        config.getInt("node.rpc.maxConcurrentCallsPerConnection") : Integer.MAX_VALUE;

        INSTANCE.flowControlWindow = config.hasPath("node.rpc.flowControlWindow") ?
                config.getInt("node.rpc.flowControlWindow")
                : NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW;

        INSTANCE.maxConnectionIdleInMillis = config.hasPath("node.rpc.maxConnectionIdleInMillis") ?
                config.getLong("node.rpc.maxConnectionIdleInMillis") : Long.MAX_VALUE;

        INSTANCE.blockProducedTimeOut = config.hasPath("node.blockProducedTimeOut") ?
                config.getInt("node.blockProducedTimeOut") : ChainConstant.BLOCK_PRODUCED_TIME_OUT;

        INSTANCE.maxHttpConnectNumber = config.hasPath("node.maxHttpConnectNumber") ?
                config.getInt("node.maxHttpConnectNumber") : NodeConstant.MAX_HTTP_CONNECT_NUMBER;

        if (INSTANCE.blockProducedTimeOut < 30) {
            INSTANCE.blockProducedTimeOut = 30;
        }
        if (INSTANCE.blockProducedTimeOut > 100) {
            INSTANCE.blockProducedTimeOut = 100;
        }

        INSTANCE.netMaxTrxPerSecond = config.hasPath("node.netMaxTrxPerSecond") ?
                config.getInt("node.netMaxTrxPerSecond") : NetConstants.NET_MAX_TRX_PER_SECOND;

        INSTANCE.maxConnectionAgeInMillis = config.hasPath("node.rpc.maxConnectionAgeInMillis") ?
                config.getLong("node.rpc.maxConnectionAgeInMillis") : Long.MAX_VALUE;

        INSTANCE.maxMessageSize = config.hasPath("node.rpc.maxMessageSize") ?
                config.getInt("node.rpc.maxMessageSize") : GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

        INSTANCE.maxHeaderListSize = config.hasPath("node.rpc.maxHeaderListSize") ?
                config.getInt("node.rpc.maxHeaderListSize") : GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;

        INSTANCE.blockExtraData =
                config.hasPath("witness.extraData") ? config
                        .getString("witness.extraData") : "";

        INSTANCE.maintenanceTimeInterval =
                config.hasPath("witness.maintenanceTimeInterval") ? config
                        .getInt("witness.maintenanceTimeInterval") : 21600000L;

        INSTANCE.proposalExpireTime =
                config.hasPath("witness.proposalExpireTime") ? config
                        .getInt("witness.proposalExpireTime") : 259200000L;

        INSTANCE.checkFrozenTime =
                config.hasPath("witness.checkFrozenTime") ? config
                        .getInt("witness.checkFrozenTime") : 1;

        INSTANCE.trxReferenceBlock = config.hasPath("witness.trx.referenceBlock") ?
                config.getString("witness.trx.referenceBlock") : "latest";

        INSTANCE.trxExpirationTimeInMilliseconds =
                config.hasPath("witness.trx.expirationTimeInMilliseconds")
                        && config.getLong("witness.trx.expirationTimeInMilliseconds") > 0 ?
                        config.getLong("witness.trx.expirationTimeInMilliseconds")
                        : Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;

        INSTANCE.allowCreationOfContracts =
                config.hasPath("committee.allowCreationOfContracts") ? config
                        .getInt("committee.allowCreationOfContracts") : 0;

        INSTANCE.allowMultiSign =
                config.hasPath("committee.allowMultiSign") ? config
                        .getInt("committee.allowMultiSign") : 0;

        INSTANCE.allowAdaptiveCpu =
                config.hasPath("committee.allowAdaptiveCpu") ? config
                        .getInt("committee.allowAdaptiveCpu") : 0;

        INSTANCE.allowDelegateResource =
                config.hasPath("committee.allowDelegateResource") ? config
                        .getInt("committee.allowDelegateResource") : 0;

        INSTANCE.allowSameTokenName =
                config.hasPath("committee.allowSameTokenName") ? config
                        .getInt("committee.allowSameTokenName") : 0;

        INSTANCE.allowGvmTransferGrc10 =
                config.hasPath("committee.allowGvmTransferGrc10") ? config
                        .getInt("committee.allowGvmTransferGrc10") : 0;

        INSTANCE.allowGvmConstantinople =
                config.hasPath("committee.allowGvmConstantinople") ? config
                        .getInt("committee.allowGvmConstantinople") : 0;

        INSTANCE.allowUpdateAccountName =
                config.hasPath("committee.allowUpdateAccountName") ? config
                        .getInt("committee.allowUpdateAccountName") : 0;

        INSTANCE.tcpNettyWorkThreadNum = config.hasPath("node.tcpNettyWorkThreadNum") ? config
                .getInt("node.tcpNettyWorkThreadNum") : 0;

        INSTANCE.udpNettyWorkThreadNum = config.hasPath("node.udpNettyWorkThreadNum") ? config
                .getInt("node.udpNettyWorkThreadNum") : 1;

        if (StringUtils.isEmpty(INSTANCE.trustNodeAddr)) {
            INSTANCE.trustNodeAddr =
                    config.hasPath("node.trustNode") ? config.getString("node.trustNode") : null;
        }

        INSTANCE.validateSignThreadNum = config.hasPath("node.validateSignThreadNum") ? config
                .getInt("node.validateSignThreadNum") : Runtime.getRuntime().availableProcessors() / 2;

        INSTANCE.walletExtensionApi =
                config.hasPath("node.walletExtensionApi") && config.getBoolean("node.walletExtensionApi");

        INSTANCE.connectFactor =
                config.hasPath("node.connectFactor") ? config.getDouble("node.connectFactor") : 0.3;

        INSTANCE.activeConnectFactor = config.hasPath("node.activeConnectFactor") ?
                config.getDouble("node.activeConnectFactor") : 0.1;

        INSTANCE.disconnectNumberFactor = config.hasPath("node.disconnectNumberFactor") ?
                config.getDouble("node.disconnectNumberFactor") : 0.4;
        INSTANCE.maxConnectNumberFactor = config.hasPath("node.maxConnectNumberFactor") ?
                config.getDouble("node.maxConnectNumberFactor") : 0.8;
        INSTANCE.receiveTcpMinDataLength = config.hasPath("node.receiveTcpMinDataLength") ?
                config.getLong("node.receiveTcpMinDataLength") : 2048;
        INSTANCE.isOpenFullTcpDisconnect = config.hasPath("node.isOpenFullTcpDisconnect") && config
                .getBoolean("node.isOpenFullTcpDisconnect");
        INSTANCE.needToUpdateAsset =
                config.hasPath("storage.needToUpdateAsset") && config.getBoolean("storage.needToUpdateAsset");

        INSTANCE.minEffectiveConnection = config.hasPath("node.rpc.minEffectiveConnection") ?
                config.getInt("node.rpc.minEffectiveConnection") : 1;

//        INSTANCE.blockNumForEneryLimit = config.hasPath("enery.limit.block.num") ?
//                config.getInt("enery.limit.block.num") : 4727890L;

        INSTANCE.vmTrace =
                config.hasPath("vm.vmTrace") && config.getBoolean("vm.vmTrace");

        INSTANCE.saveInternalTx =
                config.hasPath("vm.saveInternalTx") && config.getBoolean("vm.saveInternalTx");

        INSTANCE.eventPluginConfig =
                config.hasPath("event.subscribe") ?
                        getEventPluginConfig(config) : null;

        INSTANCE.eventFilter =
                config.hasPath("event.subscribe.filter") ? getEventFilter(config) : null;

        INSTANCE.allowProtoFilterNum =
                config.hasPath("committee.allowProtoFilterNum") ? config
                        .getInt("committee.allowProtoFilterNum") : 0;

        INSTANCE.allowAccountStateRoot =
                config.hasPath("committee.allowAccountStateRoot") ? config
                        .getInt("committee.allowAccountStateRoot") : 0;

        INSTANCE.validContractProtoThreadNum =
                config.hasPath("node.validContractProto.threads") ? config
                        .getInt("node.validContractProto.threads")
                        : Runtime.getRuntime().availableProcessors();

        INSTANCE.activeNodes = getNodes(config, "node.active");

        INSTANCE.passiveNodes = getNodes(config, "node.passive");

        INSTANCE.fastForwardNodes = getNodes(config, "node.fastForward");

        initBackupProperty(config);
        if ("ROCKSDB".equals(Args.getInstance().getStorage().getDbEngine().toUpperCase())) {
            initRocksDbBackupProperty(config);
            initRocksDbSettings(config);
        }

        logConfig();
    }

    private static List<Witness> getWitnessesFromConfig(final com.typesafe.config.Config config) {
        return config.getObjectList("genesis.block.witnesses").stream()
                .map(Args::createWitness)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Witness createWitness(final ConfigObject witnessAccount) {
        final Witness witness = new Witness();
        witness.setAddress(
                Wallet.decodeFromBase58Check(witnessAccount.get("address").unwrapped().toString()));
        witness.setUrl(witnessAccount.get("url").unwrapped().toString());
        witness.setVoteCount(witnessAccount.toConfig().getLong("voteCount"));
        return witness;
    }

    private static List<Account> getAccountsFromConfig(final com.typesafe.config.Config config) {
        return config.getObjectList("genesis.block.assets").stream()
                .map(Args::createAccount)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Account createAccount(final ConfigObject asset) {
        final Account account = new Account();
        account.setAccountName(asset.get("accountName").unwrapped().toString());
        account.setAccountType(asset.get("accountType").unwrapped().toString());
        account.setAddress(Wallet.decodeFromBase58Check(asset.get("address").unwrapped().toString()));
        account.setBalance(asset.get("balance").unwrapped().toString());
        return account;
    }

    public static Args getInstance() {
        return INSTANCE;
    }

    /**
     * Get storage path by name of database
     *
     * @param dbName name of database
     * @return path of that database
     */
    public String getOutputDirectoryByDbName(String dbName) {
        String path = storage.getPathByDbName(dbName);
        if (!StringUtils.isBlank(path)) {
            return path;
        }
        return getOutputDirectory();
    }

    /**
     * get db directory.
     */
    public String getOutputDirectory() {
        if (!this.outputDirectory.equals("") && !this.outputDirectory.endsWith(File.separator)) {
            return this.outputDirectory + File.separator;
        }
        return this.outputDirectory;
    }

    private static List<Node> getNodes(final com.typesafe.config.Config config, String path) {
        if (!config.hasPath(path)) {
            return Collections.emptyList();
        }
        List<Node> ret = new ArrayList<>();
        List<String> list = config.getStringList(path);
        for (String configString : list) {
            Node n = Node.instanceOf(configString);
            if (!(INSTANCE.nodeDiscoveryBindIp.equals(n.getHost()) ||
                    INSTANCE.nodeExternalIp.equals(n.getHost()) ||
                    "127.0.0.1".equals(n.getHost())) ||
                    INSTANCE.nodeListenPort != n.getPort()) {
                ret.add(n);
            }
        }
        return ret;
    }

//    private static void privateKey(final com.typesafe.config.Config config) {
//        if (config.hasPath("private.key")) {
//
//            INSTANCE.privateKey = config.getString("private.key");
//            if (INSTANCE.privateKey.length() != ChainConstant.PRIVATE_KEY_LENGTH) {
//                throw new RuntimeException(
//                        "The peer.privateKey needs to be Hex encoded and 32 byte length");
//            }
//        } else {
//            INSTANCE.privateKey = getGeneratedNodePrivateKey();
//        }
//    }

    private static EventPluginConfig getEventPluginConfig(final com.typesafe.config.Config config) {
        EventPluginConfig eventPluginConfig = new EventPluginConfig();

        boolean useNativeQueue = false;
        int bindPort = 0;
        int sendQueueLength = 0;
        if (config.hasPath("event.subscribe.native.useNativeQueue")) {
            useNativeQueue = config.getBoolean("event.subscribe.native.useNativeQueue");

            if (config.hasPath("event.subscribe.native.bindport")) {
                bindPort = config.getInt("event.subscribe.native.bindport");
            }

            if (config.hasPath("event.subscribe.native.sendqueuelength")) {
                sendQueueLength = config.getInt("event.subscribe.native.sendqueuelength");
            }

            eventPluginConfig.setUseNativeQueue(useNativeQueue);
            eventPluginConfig.setBindPort(bindPort);
            eventPluginConfig.setSendQueueLength(sendQueueLength);
        }

        // use event plugin
        if (!useNativeQueue) {
            if (config.hasPath("event.subscribe.path")) {
                String pluginPath = config.getString("event.subscribe.path");
                if (StringUtils.isNotEmpty(pluginPath)) {
                    eventPluginConfig.setPluginPath(pluginPath.trim());
                }
            }

            if (config.hasPath("event.subscribe.server")) {
                String serverAddress = config.getString("event.subscribe.server");
                if (StringUtils.isNotEmpty(serverAddress)) {
                    eventPluginConfig.setServerAddress(serverAddress.trim());
                }
            }

            if (config.hasPath("event.subscribe.dbconfig")) {
                String dbConfig = config.getString("event.subscribe.dbconfig");
                if (StringUtils.isNotEmpty(dbConfig)) {
                    eventPluginConfig.setDbConfig(dbConfig.trim());
                }
            }
        }

        if (config.hasPath("event.subscribe.topics")) {
            List<TriggerConfig> triggerConfigList = config.getObjectList("event.subscribe.topics")
                    .stream()
                    .map(Args::createTriggerConfig)
                    .collect(Collectors.toCollection(ArrayList::new));

            eventPluginConfig.setTriggerConfigList(triggerConfigList);
        }

        return eventPluginConfig;
    }

    private static TriggerConfig createTriggerConfig(ConfigObject triggerObject) {
        if (Objects.isNull(triggerObject)) {
            return null;
        }

        TriggerConfig triggerConfig = new TriggerConfig();

        String triggerName = triggerObject.get("triggerName").unwrapped().toString();
        triggerConfig.setTriggerName(triggerName);

        String enabled = triggerObject.get("enable").unwrapped().toString();
        triggerConfig.setEnabled("true".equalsIgnoreCase(enabled) ? true : false);

        String topic = triggerObject.get("topic").unwrapped().toString();
        triggerConfig.setTopic(topic);

        return triggerConfig;
    }

    private static FilterQuery getEventFilter(final com.typesafe.config.Config config) {
        FilterQuery filter = new FilterQuery();
        long fromBlockLong = 0, toBlockLong = 0;

        String fromBlock = config.getString("event.subscribe.filter.fromblock").trim();
        try {
            fromBlockLong = FilterQuery.parseFromBlockNumber(fromBlock);
        } catch (Exception e) {
            logger.error("{}", e);
            return null;
        }
        filter.setFromBlock(fromBlockLong);

        String toBlock = config.getString("event.subscribe.filter.toblock").trim();
        try {
            toBlockLong = FilterQuery.parseToBlockNumber(toBlock);
        } catch (Exception e) {
            logger.error("{}", e);
            return null;
        }
        filter.setToBlock(toBlockLong);

        List<String> addressList = config.getStringList("event.subscribe.filter.contractAddress");
        addressList = addressList.stream().filter(address -> StringUtils.isNotEmpty(address)).collect(
                Collectors.toList());
        filter.setContractAddressList(addressList);

        List<String> topicList = config.getStringList("event.subscribe.filter.contractTopic");
        topicList = topicList.stream().filter(top -> StringUtils.isNotEmpty(top)).collect(
                Collectors.toList());
        filter.setContractTopicList(topicList);

        return filter;
    }


    private static String getGeneratedNodePrivateKey() {
        String nodeId;
        try {
            File file = new File(
                    INSTANCE.outputDirectory + File.separator + INSTANCE.storage.getDbDirectory(),
                    "nodeId.properties");
            Properties props = new Properties();
            if (file.canRead()) {
                try (Reader r = new FileReader(file)) {
                    props.load(r);
                }
            } else {
                ECKey key = new ECKey();
                props.setProperty("nodeIdPrivateKey", Hex.toHexString(key.getPrivKeyBytes()));
                props.setProperty("nodeId", Hex.toHexString(key.getNodeId()));
                file.getParentFile().mkdirs();
                try (Writer w = new FileWriter(file)) {
                    props.store(w,
                            "Generated NodeID. To use your own nodeId please refer to 'peer.privateKey' config option.");
                }
                logger.info("New nodeID generated: " + props.getProperty("nodeId"));
                logger.info("Generated nodeID and its private key stored in " + file);
            }
            nodeId = props.getProperty("nodeIdPrivateKey");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return nodeId;
    }

    private static void bindIp(final com.typesafe.config.Config config) {
        if (!config.hasPath("node.discovery.bind.ip") || config.getString("node.discovery.bind.ip")
                .trim().isEmpty()) {
            if (INSTANCE.nodeDiscoveryBindIp == null) {
                logger.info("Bind address wasn't set, Punching to identify it...");
                try (Socket s = new Socket("www.baidu.com", 80)) {
                    INSTANCE.nodeDiscoveryBindIp = s.getLocalAddress().getHostAddress();
                    logger.info("UDP local bound to: {}", INSTANCE.nodeDiscoveryBindIp);
                } catch (IOException e) {
                    logger.warn("Can't get bind IP. Fall back to 0.0.0.0: " + e);
                    INSTANCE.nodeDiscoveryBindIp = "0.0.0.0";
                }
            }
        } else {
            INSTANCE.nodeDiscoveryBindIp = config.getString("node.discovery.bind.ip").trim();
        }
    }

    private static void externalIp(final com.typesafe.config.Config config) {
        if (!config.hasPath("node.discovery.external.ip") || config
                .getString("node.discovery.external.ip").trim().isEmpty()) {
            if (INSTANCE.nodeExternalIp == null) {
                logger.info("External IP wasn't set, using checkip.amazonaws.com to identify it...");
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(
                            new URL("http://checkip.amazonaws.com").openStream()));
                    INSTANCE.nodeExternalIp = in.readLine();
                    if (INSTANCE.nodeExternalIp == null || INSTANCE.nodeExternalIp.trim().isEmpty()) {
                        throw new IOException("Invalid address: '" + INSTANCE.nodeExternalIp + "'");
                    }
                    try {
                        InetAddress.getByName(INSTANCE.nodeExternalIp);
                    } catch (Exception e) {
                        throw new IOException("Invalid address: '" + INSTANCE.nodeExternalIp + "'");
                    }
                    logger.info("External address identified: {}", INSTANCE.nodeExternalIp);
                } catch (IOException e) {
                    INSTANCE.nodeExternalIp = INSTANCE.nodeDiscoveryBindIp;
                    logger.warn(
                            "Can't get external IP. Fall back to peer.bind.ip: " + INSTANCE.nodeExternalIp + " :"
                                    + e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            //ignore
                        }
                    }

                }
            }
        } else {
            INSTANCE.nodeExternalIp = config.getString("node.discovery.external.ip").trim();
        }
    }

    public ECKey getMyKey() {
        if (StringUtils.isEmpty(INSTANCE.p2pNodeId)) {
            INSTANCE.p2pNodeId = getGeneratedNodePrivateKey();
        }

        return ECKey.fromPrivate(Hex.decode(INSTANCE.p2pNodeId));
    }

    private static double calcMaxTimeRatio() {
        //return max(2.0, min(5.0, 5 * 4.0 / max(Runtime.getRuntime().availableProcessors(), 1)));
        return 5.0;
    }


    private static void initRocksDbSettings(Config config) {
        String prefix = "storage.dbSettings.";
        int levelNumber = config.hasPath(prefix + "levelNumber")
                ? config.getInt(prefix + "levelNumber") : 7;
        int compactThreads = config.hasPath(prefix + "compactThreads")
                ? config.getInt(prefix + "compactThreads")
                : max(Runtime.getRuntime().availableProcessors(), 1);
        int blocksize = config.hasPath(prefix + "blocksize")
                ? config.getInt(prefix + "blocksize") : 16;
        long maxBytesForLevelBase = config.hasPath(prefix + "maxBytesForLevelBase")
                ? config.getInt(prefix + "maxBytesForLevelBase") : 256;
        double maxBytesForLevelMultiplier = config.hasPath(prefix + "maxBytesForLevelMultiplier")
                ? config.getDouble(prefix + "maxBytesForLevelMultiplier") : 10;
        int level0FileNumCompactionTrigger =
                config.hasPath(prefix + "level0FileNumCompactionTrigger") ? config
                        .getInt(prefix + "level0FileNumCompactionTrigger") : 2;
        long targetFileSizeBase = config.hasPath(prefix + "targetFileSizeBase") ? config
                .getLong(prefix + "targetFileSizeBase") : 64;
        int targetFileSizeMultiplier = config.hasPath(prefix + "targetFileSizeMultiplier") ? config
                .getInt(prefix + "targetFileSizeMultiplier") : 1;

        INSTANCE.rocksDBCustomSettings = RocksDbSettings
                .initCustomSettings(levelNumber, compactThreads, blocksize, maxBytesForLevelBase,
                        maxBytesForLevelMultiplier, level0FileNumCompactionTrigger,
                        targetFileSizeBase, targetFileSizeMultiplier);
        RocksDbSettings.loggingSettings();
    }

    private static void initRocksDbBackupProperty(Config config) {
        boolean enable =
                config.hasPath("storage.backup.enable") && config.getBoolean("storage.backup.enable");
        String propPath = config.hasPath("storage.backup.propPath")
                ? config.getString("storage.backup.propPath") : "prop.properties";
        String bak1path = config.hasPath("storage.backup.bak1path")
                ? config.getString("storage.backup.bak1path") : "bak1/database/";
        String bak2path = config.hasPath("storage.backup.bak2path")
                ? config.getString("storage.backup.bak2path") : "bak2/database/";
        int frequency = config.hasPath("storage.backup.frequency")
                ? config.getInt("storage.backup.frequency") : 10000;
        INSTANCE.dbBackupConfig = DbBackupConfig.getInstance()
                .initArgs(enable, propPath, bak1path, bak2path, frequency);
    }

    private static void initBackupProperty(Config config) {
        INSTANCE.backupPriority = config.hasPath("node.backup.priority")
                ? config.getInt("node.backup.priority") : 0;
        INSTANCE.backupPort = config.hasPath("node.backup.port")
                ? config.getInt("node.backup.port") : 5555;
        INSTANCE.backupMembers = config.hasPath("node.backup.members")
                ? config.getStringList("node.backup.members") : new ArrayList<>();
    }

    private static void logConfig() {
        Args args = getInstance();
        logger.info("\n");
        logger.info("************************ Net config ************************");
        logger.info("P2P version: {}", args.getNodeP2pVersion());
        logger.info("Bind IP: {}", args.getNodeDiscoveryBindIp());
        logger.info("External IP: {}", args.getNodeExternalIp());
        logger.info("Listen port: {}", args.getNodeListenPort());
        logger.info("Discover enable: {}", args.isNodeDiscoveryEnable());
        logger.info("Active node size: {}", args.getActiveNodes().size());
        logger.info("Passive node size: {}", args.getPassiveNodes().size());
        logger.info("FastForward node size: {}", args.getFastForwardNodes().size());
        logger.info("Boot node size: {}", args.getBootNodes().size());
        logger.info("Max connection: {}", args.getNodeMaxActiveNodes());
        logger.info("Max connection with same IP: {}", args.getNodeMaxActiveNodesWithSameIp());
        logger.info("Confirmed threads: {}", args.getConfirmedThreadNum());
        logger.info("************************ Backup config ************************");
        logger.info("Backup listen port: {}", args.getBackupPort());
        logger.info("Backup member size: {}", args.getBackupMembers().size());
        logger.info("Backup priority: {}", args.getBackupPriority());
        logger.info("************************ Code version *************************");
        logger.info("Code version : {}", Version.getVersion());
        logger.info("Version name: {}", Version.versionName);
        logger.info("Version code: {}", Version.versionCode);
        logger.info("************************ DB config *************************");
        logger.info("DB version : {}", args.getStorage().getDbVersion());
        logger.info("DB engine : {}", args.getStorage().getDbEngine());
        logger.info("***************************************************************");
        logger.info("\n");
    }
}
