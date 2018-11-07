package org.gsc.program;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.application.Application;
import org.gsc.common.application.ApplicationFactory;
import org.gsc.common.application.GSCApplicationContext;
import org.gsc.common.overlay.client.DatabaseGrpcClient;
import org.gsc.common.overlay.discover.DiscoverServer;
import org.gsc.common.overlay.discover.node.NodeManager;
import org.gsc.common.overlay.server.ChannelManager;
import org.gsc.config.DefaultConfig;
import org.gsc.config.args.Args;
import org.gsc.core.Constant;
import org.gsc.core.exception.*;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionInfoWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.db.Manager;
import org.gsc.services.RpcApiService;
import org.gsc.services.WitnessService;
import org.gsc.services.http.FullNodeHttpApiService;
import org.gsc.services.http.solidity.SolidityNodeHttpApiService;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.gsc.protos.Protocol.DynamicProperties;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.gsc.protos.Protocol.Block;
import org.springframework.context.ApplicationContext;

@Slf4j
public class Start {
    private DatabaseGrpcClient databaseGrpcClient;
    private Manager dbManager;

    private ScheduledExecutorService syncExecutor = Executors.newSingleThreadScheduledExecutor();

    public void setDbManager(Manager dbManager) {
        this.dbManager = dbManager;
    }

    public static void main(String[] args) {
        logger.info("GSC node running.");
        Args.setParam(args, Constant.TESTNET_CONF);
        Args cfgArgs = Args.getInstance();

        if (cfgArgs.isHelp()) {
            logger.info("Here is the help message.");
            return;
        }

        if (Args.getInstance().isDebug()) {
            logger.info("in debug mode, it won't check energy time");
        } else {
            logger.info("not in debug mode, it will check energy time");
        }

        if(cfgArgs.isSolidityNode()){
            ApplicationContext context = new GSCApplicationContext(DefaultConfig.class);

            Application appT = ApplicationFactory.create(context);
            shutdown(appT);

            //appT.init(cfgArgs);
            RpcApiService rpcApiService = context.getBean(RpcApiService.class);
            appT.addService(rpcApiService);
            //http
            SolidityNodeHttpApiService httpApiService = context.getBean(SolidityNodeHttpApiService.class);
            appT.addService(httpApiService);

            appT.initServices(cfgArgs);
            appT.startServices();
            //    appT.startup();

            //Disable peer discovery for solidity node
            DiscoverServer discoverServer = context.getBean(DiscoverServer.class);
            discoverServer.close();
            ChannelManager channelManager = context.getBean(ChannelManager.class);
            channelManager.close();
            NodeManager nodeManager = context.getBean(NodeManager.class);
            nodeManager.close();

            Start node = new Start();
            node.setDbManager(appT.getDbManager());
            node.start(cfgArgs);

            rpcApiService.blockUntilShutdown();
        }else {
            DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
            beanFactory.setAllowCircularReferences(false);
            GSCApplicationContext context =
                    new GSCApplicationContext(beanFactory);
            context.register(DefaultConfig.class);

            context.refresh();

            Application appT = ApplicationFactory.create(context);
            shutdown(appT);

            // grpc api server
            RpcApiService rpcApiService = context.getBean(RpcApiService.class);
            appT.addService(rpcApiService);
            if (cfgArgs.isWitness()) {
                appT.addService(new WitnessService(appT, context));
            }

            // http api server
            FullNodeHttpApiService httpApiService = context.getBean(FullNodeHttpApiService.class);
            appT.addService(httpApiService);

            appT.initServices(cfgArgs);
            appT.startServices();
            appT.startup();

            rpcApiService.blockUntilShutdown();
        }
    }

    public void start(Args cfgArgs) {
        syncExecutor.scheduleWithFixedDelay(() -> {
            try {
                initGrpcClient(cfgArgs.getTrustNodeAddr());
                syncSolidityBlock();
                shutdownGrpcClient();
            } catch (Throwable t) {
                logger.error("Error in sync solidity block" + t.getMessage(), t);
            }
        }, 5000, 5000, TimeUnit.MILLISECONDS);
        //new Thread(() -> syncLoop(cfgArgs), logger.getName()).start();
    }

    private void initGrpcClient(String addr) {
        try {
            databaseGrpcClient = new DatabaseGrpcClient(addr);
        } catch (Exception e) {
            logger.error("Failed to create database grpc client {}", addr);
            System.exit(0);
        }
    }

    private void syncSolidityBlock() throws BadBlockException {
        DynamicProperties remoteDynamicProperties = databaseGrpcClient.getDynamicProperties();
        long remoteLastSolidityBlockNum = remoteDynamicProperties.getLastSolidityBlockNum();
        while (true) {

            long lastSolidityBlockNum = dbManager.getDynamicPropertiesStore()
                    .getLatestSolidifiedBlockNum();
            logger.info("sync solidity block, lastSolidityBlockNum:{}, remoteLastSolidityBlockNum:{}",
                    lastSolidityBlockNum, remoteLastSolidityBlockNum);
            if (lastSolidityBlockNum < remoteLastSolidityBlockNum) {
                Block block = databaseGrpcClient.getBlock(lastSolidityBlockNum + 1);
                try {
                    BlockWrapper blockWrapper = new BlockWrapper(block);
                    dbManager.pushBlock(blockWrapper);
                    for (TransactionWrapper trx : blockWrapper.getTransactions()) {
                        TransactionInfoWrapper ret;
                        try {
                            ret = dbManager.getTransactionHistoryStore().get(trx.getTransactionId().getBytes());
                        } catch (BadItemException ex) {
                            logger.warn(" ", ex);
                            continue;
                        }
                        ret.setBlockNumber(blockWrapper.getNum());
                        ret.setBlockTimeStamp(blockWrapper.getTimeStamp());
                        dbManager.getTransactionHistoryStore().put(trx.getTransactionId().getBytes(), ret);
                    }
                    dbManager.getDynamicPropertiesStore()
                            .saveLatestSolidifiedBlockNum(lastSolidityBlockNum + 1);
                } catch (AccountResourceInsufficientException e) {
                    throw new BadBlockException("validate AccountResource exception");
                } catch (ValidateScheduleException e) {
                    throw new BadBlockException("validate schedule exception");
                } catch (ValidateSignatureException e) {
                    throw new BadBlockException("validate signature exception");
                } catch (ContractValidateException e) {
                    throw new BadBlockException("ContractValidate exception");
                } catch (ContractExeException | UnLinkedBlockException e) {
                    throw new BadBlockException("Contract Execute exception");
                } catch (TaposException e) {
                    throw new BadBlockException("tapos exception");
                } catch (DupTransactionException e) {
                    throw new BadBlockException("dup exception");
                } catch (TooBigTransactionException e) {
                    throw new BadBlockException("too big exception");
                } catch (TooBigTransactionResultException e) {
                    throw new BadBlockException("too big exception result");
                } catch (TransactionExpirationException e) {
                    throw new BadBlockException("expiration exception");
                } catch (BadNumberBlockException e) {
                    throw new BadBlockException("bad number exception");
                } catch (ReceiptException e) {
                    throw new BadBlockException("Receipt exception");
                } catch (NonCommonBlockException e) {
                    throw new BadBlockException("non common exception");
                } catch (TransactionTraceException e) {
                    throw new BadBlockException("TransactionTrace Exception");
                } catch (ReceiptCheckErrException e) {
                    throw new BadBlockException("OutOfSlotTime Exception");
                } catch (UnsupportVMException e) {
                    throw new BadBlockException(e.getMessage());
                }
            } else {
                break;
            }
        }
        logger.info("Sync with trust node completed!!!");
    }

    private void shutdownGrpcClient() {
        if (databaseGrpcClient != null) {
            databaseGrpcClient.shutdown();
        }
    }

    public static void shutdown(final Application app) {
        logger.info("********register application shutdown hook********");
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
    }
}
