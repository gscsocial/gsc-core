package org.gsc.config;

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
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gsc.common.utils.AddressUtil;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Account;
import org.gsc.config.args.GenesisBlock;
import org.gsc.config.args.LocalWitnesses;
import org.gsc.config.args.Overlay;
import org.gsc.config.args.SeedNode;
import org.gsc.config.args.Witness;
import org.gsc.crypto.ECKey;
import org.gsc.db.AccountStore;
import org.gsc.keystore.CipherException;
import org.gsc.keystore.Credentials;
import org.gsc.keystore.WalletUtils;
import org.gsc.net.discover.Node;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Data
public class Args {

  private int rpcPort;

  private int nodeP2pVersion;
  
  private boolean nodeDiscoveryEnable;

  private int nodeListenPort;

  private int nodeConnectionTimeout;
  
  private int nodeMaxActiveNodes;
  
  private int minParticipationRate;

  private boolean needSyncCheck;

  private String storageDir;

  private long maintenanceTimeInterval;

  private long genesisBlockTimestamp;

  private boolean prod;

  @com.beust.jcommander.Parameter(names = {"-c", "--config"}, description = "Config File")
  private String shellConfFileName = "";

  @com.beust.jcommander.Parameter(names = {"-d", "--output-directory"}, description = "Directory")
  private String outputDirectory = "output-directory";

  @com.beust.jcommander.Parameter(names = {"-h", "--help"}, help = true, description = "HELP message")
  private boolean help = false;

  @Setter
  @com.beust.jcommander.Parameter(names = {"-w", "--witness"})
  private boolean witness = false;

  @com.beust.jcommander.Parameter(description = "--seed-nodes")
  private List<String> seedNodes = new ArrayList<>();

  @com.beust.jcommander.Parameter(names = {"-p", "--private-key"}, description = "private-key")
  private String privateKey = "";

  @com.beust.jcommander.Parameter(names = {"--password"}, description = "password")
  private String password;

  @com.beust.jcommander.Parameter(names = {"--storage-db-directory"}, description = "Storage db directory")
  private String storageDbDirectory = "";

  @com.beust.jcommander.Parameter(names = {"--storage-index-directory"}, description = "Storage index directory")
  private String storageIndexDirectory = "";

  private org.gsc.config.args.Storage storage;

  private Overlay overlay;

  private SeedNode seedNode;

  private GenesisBlock genesisBlock;

  private String chainId;

  private LocalWitnesses localWitnesses = new LocalWitnesses();

  private boolean nodeDiscoveryPersist;

  private List<Node> activeNodes;

  private List<Node> trustNodes;

  private int nodeChannelReadTimeout;

  private int nodeMaxActiveNodesWithSameIp;

  private String nodeDiscoveryBindIp;

  private String nodeExternalIp;

  private boolean nodeDiscoveryPublicHomeNode;

  private long nodeP2pPingInterval;

  private String p2pNodeId;

  private boolean solidityNode = false;

  @com.beust.jcommander.Parameter(names = {"--rpc-thread"}, description = "Num of gRPC thread")
  private int rpcThreadNum;

  private int maxConcurrentCallsPerConnection;

  private int flowControlWindow;

  private long maxConnectionIdleInMillis;

  private long maxConnectionAgeInMillis;

  private int maxMessageSize;

  private int maxHeaderListSize;

  @com.beust.jcommander.Parameter(names = {"--validate-sign-thread"}, description = "Num of validate thread")
  private int validateSignThreadNum;

  private int tcpNettyWorkThreadNum;

  private int udpNettyWorkThreadNum;

  @Parameter(names = {"--trust-node"}, description = "Trust node addr")
  private String trustNodeAddr;

  private boolean walletExtensionApi;

  private int backupPriority;

  private int backupPort;

  private List<String> backupMembers;

  public void clearParam() {
    this.outputDirectory = "output-directory";
    this.help = false;
    this.witness = false;
    this.seedNodes = new ArrayList<>();
    this.privateKey = "";
    this.storageDbDirectory = "";
    this.storageIndexDirectory = "";

    // FIXME: this.storage maybe null ?
    if (this.storage != null) {
      // WARNING: WILL DELETE DB STORAGE PATHS
      this.storage.deleteAllStoragePaths();
      this.storage = null;
    }

    this.overlay = null;
    this.seedNode = null;
    this.genesisBlock = null;
    this.chainId = null;
    this.localWitnesses = null;
    this.needSyncCheck = false;
    this.nodeDiscoveryEnable = false;
    this.nodeDiscoveryPersist = false;
    this.nodeConnectionTimeout = 0;
    this.activeNodes = Collections.emptyList();
    this.trustNodes = Collections.emptyList();
    this.nodeChannelReadTimeout = 0;
    this.nodeMaxActiveNodes = 30;
    this.nodeMaxActiveNodesWithSameIp = 2;
    this.minParticipationRate = 0;
    this.nodeListenPort = 0;
    this.nodeDiscoveryBindIp = "";
    this.nodeExternalIp = "";
    this.nodeDiscoveryPublicHomeNode = false;
    this.nodeP2pPingInterval = 0L;
    //this.syncNodeCount = 0;
    this.nodeP2pVersion = 0;
    this.rpcPort = 0;
    this.maintenanceTimeInterval = 0;
    this.tcpNettyWorkThreadNum = 0;
    this.udpNettyWorkThreadNum = 0;
    this.p2pNodeId = "";
    this.solidityNode = false;
    this.trustNodeAddr = "";
    this.walletExtensionApi = false;
  }

  /**
   * set parameters.
   */
  public void setParam(final String[] args, final String confFileName) {
    JCommander.newBuilder().addObject(this).build().parse(args);
    Config config = Configuration.getByFileName(this.shellConfFileName, confFileName);
    if (StringUtils.isNoneBlank(this.privateKey)) {
      this.setLocalWitnesses(new LocalWitnesses(this.privateKey));
      logger.debug("Got privateKey from cmd");
    } else if (config.hasPath("localwitness")) {
      this.localWitnesses = new LocalWitnesses();
      List<String> localwitness = config.getStringList("localwitness");
      if (localwitness.size() > 1) {
        logger.warn("localwitness size must be one, get the first one");
        localwitness = localwitness.subList(0, 1);
      }
      this.localWitnesses.setPrivateKeys(localwitness);
      logger.debug("Got privateKey from config.conf");
    } else if (config.hasPath("localwitnesskeystore")) {
      this.localWitnesses = new LocalWitnesses();
      List<String> privateKeys = new ArrayList<String>();
      if (this.isWitness()) {
        List<String> localwitness = config.getStringList("localwitnesskeystore");
        if (localwitness.size() > 0) {
          String fileName = System.getProperty("user.dir") + "/" + localwitness.get(0);
          String password;
          if (StringUtils.isEmpty(this.password)) {
            System.out.println("Please input your password.");
            password = WalletUtils.inputPassword();
          } else {
            password = this.password;
            this.password = null;
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
            System.exit(-1);
          } catch (CipherException e) {
            logger.error(e.getMessage());
            logger.error("Witness node start faild!");
            System.exit(-1);
          }
        }
      }
      this.localWitnesses.setPrivateKeys(privateKeys);
      logger.debug("Got privateKey from keystore");
    }

    if (this.isWitness() && CollectionUtils.isEmpty(this.localWitnesses.getPrivateKeys())) {
      logger.warn("This is a witness node,but localWitnesses is null");
    }

    this.storage = new org.gsc.config.args.Storage();
    this.storage.setDbDirectory(Optional.ofNullable(this.storageDbDirectory)
        .filter(StringUtils::isNotEmpty)
        .orElse(org.gsc.config.args.Storage.getDbDirectoryFromConfig(config)));

    this.storage.setIndexDirectory(Optional.ofNullable(this.storageIndexDirectory)
        .filter(StringUtils::isNotEmpty)
        .orElse(org.gsc.config.args.Storage.getIndexDirectoryFromConfig(config)));

    this.storage.setPropertyMapFromConfig(config);

    this.seedNode = new SeedNode();
    this.seedNode.setIpList(Optional.ofNullable(this.seedNodes)
        .filter(seedNode -> 0 != seedNode.size())
        .orElse(config.getStringList("seed.node.ip.list")));

    if (config.hasPath("net.type") && "mainnet".equalsIgnoreCase(config.getString("net.type"))) {
      //TODO Address prefix
    } else {
      //TODO Address prefix
    }

    if (config.hasPath("genesis.block")) {
      this.genesisBlock = new GenesisBlock();

      this.genesisBlock.setTimestamp(config.getString("genesis.block.timestamp"));
      this.genesisBlock.setParentHash(config.getString("genesis.block.parentHash"));

      if (config.hasPath("genesis.block.assets")) {
        this.genesisBlock.setAssets(getAccountsFromConfig(config));
        AccountStore.setAccount(config);
      }
      if (config.hasPath("genesis.block.witnesses")) {
        this.genesisBlock.setWitnesses(getWitnessesFromConfig(config));
      }
    } else {
      this.genesisBlock = GenesisBlock.getDefault();
    }

    this.needSyncCheck =
        config.hasPath("block.needSyncCheck") && config.getBoolean("block.needSyncCheck");

    this.nodeDiscoveryEnable =
        config.hasPath("node.discovery.enable") && config.getBoolean("node.discovery.enable");

    this.nodeDiscoveryPersist =
        config.hasPath("node.discovery.persist") && config.getBoolean("node.discovery.persist");

    this.nodeConnectionTimeout =
        config.hasPath("node.connection.timeout") ? config.getInt("node.connection.timeout") * 1000
            : 0;

    this.activeNodes = getNodes(config, "active.node");

    this.trustNodes = getNodes(config, "trust.node");

    this.nodeChannelReadTimeout =
        config.hasPath("node.channel.read.timeout") ? config.getInt("node.channel.read.timeout")
            : 0;

    this.nodeMaxActiveNodes =
        config.hasPath("node.maxActiveNodes") ? config.getInt("node.maxActiveNodes") : 30;

    this.nodeMaxActiveNodesWithSameIp =
        config.hasPath("node.maxActiveNodesWithSameIp") ? config.getInt("node.maxActiveNodesWithSameIp") : 2;

    this.minParticipationRate =
        config.hasPath("node.minParticipationRate") ? config.getInt("node.minParticipationRate")
            : 0;

    this.nodeListenPort =
        config.hasPath("node.listen.port") ? config.getInt("node.listen.port") : 0;

    bindIp(config);
    externalIp(config);

    this.nodeDiscoveryPublicHomeNode =
        config.hasPath("node.discovery.public.home.node") && config
            .getBoolean("node.discovery.public.home.node");

    this.nodeP2pPingInterval =
        config.hasPath("node.p2p.pingInterval") ? config.getLong("node.p2p.pingInterval") : 0;
//
//    this.syncNodeCount =
//        config.hasPath("sync.node.count") ? config.getLong("sync.node.count") : 0;

    this.nodeP2pVersion =
        config.hasPath("node.p2p.version") ? config.getInt("node.p2p.version") : 0;

    this.rpcPort =
        config.hasPath("node.rpc.port") ? config.getInt("node.rpc.port") : 50051;

    this.rpcThreadNum =
        config.hasPath("node.rpc.thread") ? config.getInt("node.rpc.thread")
            : Runtime.getRuntime().availableProcessors() / 2;

    this.maxConcurrentCallsPerConnection =
        config.hasPath("node.rpc.maxConcurrentCallsPerConnection") ?
            config.getInt("node.rpc.maxConcurrentCallsPerConnection") : Integer.MAX_VALUE;

    this.flowControlWindow = config.hasPath("node.rpc.flowControlWindow") ?
        config.getInt("node.rpc.flowControlWindow")
        : NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW;

    this.maxConnectionIdleInMillis = config.hasPath("node.rpc.maxConnectionIdleInMillis") ?
        config.getLong("node.rpc.maxConnectionIdleInMillis") : Long.MAX_VALUE;

    this.maxConnectionAgeInMillis = config.hasPath("node.rpc.maxConnectionAgeInMillis") ?
        config.getLong("node.rpc.maxConnectionAgeInMillis") : Long.MAX_VALUE;

    this.maxMessageSize = config.hasPath("node.rpc.maxMessageSize") ?
        config.getInt("node.rpc.maxMessageSize") : GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

    this.maxHeaderListSize = config.hasPath("node.rpc.maxHeaderListSize") ?
        config.getInt("node.rpc.maxHeaderListSize") : GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;

    this.maintenanceTimeInterval =
        config.hasPath("block.maintenanceTimeInterval") ? config
            .getInt("block.maintenanceTimeInterval") : 21600000L;

    this.tcpNettyWorkThreadNum = config.hasPath("node.tcpNettyWorkThreadNum") ? config
        .getInt("node.tcpNettyWorkThreadNum") : 0;

    this.udpNettyWorkThreadNum = config.hasPath("node.udpNettyWorkThreadNum") ? config
        .getInt("node.udpNettyWorkThreadNum") : 1;

    if (StringUtils.isEmpty(this.trustNodeAddr)) {
      this.trustNodeAddr =
          config.hasPath("node.trustNode") ? config.getString("node.trustNode") : null;
    }

    this.validateSignThreadNum = config.hasPath("node.validateSignThreadNum") ? config
        .getInt("node.validateSignThreadNum") : Runtime.getRuntime().availableProcessors() / 2;

    this.walletExtensionApi =
        config.hasPath("node.walletExtensionApi") && config.getBoolean("node.walletExtensionApi");

    initBackupProperty(config);
  }


  private static List<Witness> getWitnessesFromConfig(final com.typesafe.config.Config config) {
    return config.getObjectList("genesis.block.witnesses").stream()
        .map(Args::createWitness)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static Witness createWitness(final ConfigObject witnessAccount) {
    final Witness witness = new Witness();
    witness.setAddress(
        AddressUtil.decodeFromBase58Check(witnessAccount.get("address").unwrapped().toString()));
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
    account.setAddress(AddressUtil.decodeFromBase58Check(asset.get("address").unwrapped().toString()));
    account.setBalance(asset.get("balance").unwrapped().toString());
    return account;
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
   * get output directory.
   */
  public String getOutputDirectory() {
    if (!this.outputDirectory.equals("") && !this.outputDirectory.endsWith(File.separator)) {
      return this.outputDirectory + File.separator;
    }
    return this.outputDirectory;
  }

  private List<Node> getNodes(final com.typesafe.config.Config config, String path) {
    if (!config.hasPath(path)) {
      return Collections.EMPTY_LIST;
    }
    List<Node> ret = new ArrayList<>();
    List<String> list = config.getStringList(path);
    for (String configString : list) {
      Node n = Node.instanceOf(configString);
      ret.add(n);
    }
    return ret;
  }

  private void privateKey(final com.typesafe.config.Config config) {
    if (config.hasPath("private.key")) {
      this.privateKey = config.getString("private.key");
      if (this.privateKey.length() != ChainConstant.PRIVATE_KEY_LENGTH) {
        throw new RuntimeException(
            "The peer.privateKey needs to be Hex encoded and 32 byte length");
      }
    } else {
      this.privateKey = getGeneratedNodePrivateKey();
    }
  }

  private String getGeneratedNodePrivateKey() {
    String nodeId;
    try {
      File file = new File(
          this.outputDirectory + File.separator + this.storage.getDbDirectory(),
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

  private void bindIp(final com.typesafe.config.Config config) {
    if (!config.hasPath("node.discovery.bind.ip") || config.getString("node.discovery.bind.ip")
        .trim().isEmpty()) {
      if (this.nodeDiscoveryBindIp == null) {
        logger.info("Bind address wasn't set, Punching to identify it...");
        try (Socket s = new Socket("www.baidu.com", 80)) {
          this.nodeDiscoveryBindIp = s.getLocalAddress().getHostAddress();
          logger.info("UDP local bound to: {}", this.nodeDiscoveryBindIp);
        } catch (IOException e) {
          logger.warn("Can't get bind IP. Fall back to 0.0.0.0: " + e);
          this.nodeDiscoveryBindIp = "0.0.0.0";
        }
      }
    } else {
      this.nodeDiscoveryBindIp = config.getString("node.discovery.bind.ip").trim();
    }
  }

  private void externalIp(final com.typesafe.config.Config config) {
    if (!config.hasPath("node.discovery.external.ip") || config
        .getString("node.discovery.external.ip").trim().isEmpty()) {
      if (this.nodeExternalIp == null) {
        logger.info("External IP wasn't set, using checkip.amazonaws.com to identify it...");
        BufferedReader in = null;
        try {
          in = new BufferedReader(new InputStreamReader(
              new URL("http://checkip.amazonaws.com").openStream()));
          this.nodeExternalIp = in.readLine();
          if (this.nodeExternalIp == null || this.nodeExternalIp.trim().isEmpty()) {
            throw new IOException("Invalid address: '" + this.nodeExternalIp + "'");
          }
          try {
            InetAddress.getByName(this.nodeExternalIp);
          } catch (Exception e) {
            throw new IOException("Invalid address: '" + this.nodeExternalIp + "'");
          }
          logger.info("External address identified: {}", this.nodeExternalIp);
        } catch (IOException e) {
          this.nodeExternalIp = this.nodeDiscoveryBindIp;
          logger.warn(
              "Can't get external IP. Fall back to peer.bind.ip: " + this.nodeExternalIp + " :"
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
      this.nodeExternalIp = config.getString("node.discovery.external.ip").trim();
    }
  }

  public ECKey getMyKey() {
    if (StringUtils.isEmpty(this.p2pNodeId)) {
      this.p2pNodeId = getGeneratedNodePrivateKey();
    }

    return ECKey.fromPrivate(Hex.decode(this.p2pNodeId));
  }

  private void initBackupProperty(Config config) {
    this.backupPriority = config.hasPath("node.backup.priority")
        ? config.getInt("node.backup.priority") : 0;
    this.backupPort = config.hasPath("node.backup.port")
        ? config.getInt("node.backup.port") : 10001;
    this.backupMembers = config.hasPath("node.backup.members")
        ? config.getStringList("node.backup.members") : new ArrayList<>();
  }
}
