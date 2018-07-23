package org.gsc.program;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.application.Application;
import org.gsc.common.application.ApplicationFactory;
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
import org.gsc.protos.Protocol;
import org.gsc.services.RpcApiService;
import org.gsc.services.WitnessService;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Start {
    private DatabaseGrpcClient databaseGrpcClient;
    private Manager dbManager;

    private ScheduledExecutorService syncExecutor = Executors.newSingleThreadScheduledExecutor();

    public void setDbManager(Manager dbManager) {
        this.dbManager = dbManager;
    }

    private void initGrpcClient(String addr) {
        try {
            databaseGrpcClient = new DatabaseGrpcClient(addr);
        } catch (Exception e) {
            logger.error("Failed to create database grpc client {}", addr);
            System.exit(0);
        }
    }

    private void shutdownGrpcClient() {
        if (databaseGrpcClient != null) {
            databaseGrpcClient.shutdown();
        }
    }

    public static void main(String [] args){
        logger.info("GSC node running.");
        Args.setParam(args, Constant.TESTNET_CONF);
        Args cfgArgs = Args.getInstance();

        if (cfgArgs.isHelp()) {
            logger.info("Here is the help message.");
            return;
        }
        if(cfgArgs.isSolidityNode()){ // solidity node needs not sync full data
            ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);
            Application appT = ApplicationFactory.create(context);
            FullNode.shutdown(appT);
            RpcApiService rpcApiService = context.getBean(RpcApiService.class);
            appT.addService(rpcApiService);

            appT.initServices(cfgArgs);
            appT.startServices();
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
        }else{
            DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
            beanFactory.setAllowCircularReferences(false);
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(beanFactory);
            context.register(DefaultConfig.class);
            context.refresh();
            Application appT = ApplicationFactory.create(context);
            shutdown(appT);

            RpcApiService rpcApiService = context.getBean(RpcApiService.class);
            appT.addService(rpcApiService);
            if (cfgArgs.isWitness()) {
                appT.addService(new WitnessService(appT, context));
            }
            appT.initServices(cfgArgs);
            appT.startServices();
            appT.startup();
            rpcApiService.blockUntilShutdown();
            rpcApiService.blockUntilShutdown();
        }

    }

    public static void shutdown(final Application app) {
        logger.info("********register application shutdown hook********");
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
    }

    private void syncSolidityBlock() throws BadBlockException {
        Protocol.DynamicProperties remoteDynamicProperties = databaseGrpcClient.getDynamicProperties();
        long remoteLastSolidityBlockNum = remoteDynamicProperties.getLastSolidityBlockNum();
        while (true) {
            long lastSolidityBlockNum = dbManager.getDynamicPropertiesStore()
                    .getLatestSolidifiedBlockNum();
            logger.info("sync solidity block, lastSolidityBlockNum:{}, remoteLastSolidityBlockNum:{}",
                    lastSolidityBlockNum, remoteLastSolidityBlockNum);
            if (lastSolidityBlockNum < remoteLastSolidityBlockNum) {
                Protocol.Block block = databaseGrpcClient.getBlock(lastSolidityBlockNum + 1);
                try {
                    BlockWrapper blockWrapper = new BlockWrapper(block);
                    dbManager.pushBlock(blockWrapper);

                    for (TransactionWrapper tx : blockWrapper.getTransactions()) {
                        TransactionInfoWrapper transactionInfoWrapper;
                        try {
                            transactionInfoWrapper = dbManager.getTransactionHistoryStore().get(tx.getTransactionId().getBytes());
                        } catch (BadItemException e) {
                            logger.warn("", e);
                            continue;
                        }
                        transactionInfoWrapper.setBlockNumber(blockWrapper.getNum());
                        transactionInfoWrapper.setBlockTimeStamp(blockWrapper.getTimeStamp());

                        dbManager.getTransactionHistoryStore().put(tx.getTransactionId().getBytes(), transactionInfoWrapper);
                    }
                    dbManager.getDynamicPropertiesStore()
                            .saveLatestSolidifiedBlockNum(lastSolidityBlockNum + 1);
                } catch (AccountResourceInsufficientException e) {
                    throw new BadBlockException("validate Account resource exception");
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
                } catch (TransactionExpirationException e) {
                    throw new BadBlockException("expiration exception");
                } catch (BadNumberBlockException e) {
                    throw new BadBlockException("bad number exception");
                } catch (NonCommonBlockException e) {
                    throw new BadBlockException("non common exception");
                }

            } else {
                break;
            }
        }
        logger.info("Sync with trust node completed!!!");
    }

    private void start(Args cfgArgs) {
        syncExecutor.scheduleWithFixedDelay(() -> {
            try {
                initGrpcClient(cfgArgs.getTrustNodeAddr());
                syncSolidityBlock();
                shutdownGrpcClient();
            } catch (Throwable t) {
                logger.error("Error in sync solidity block " + t.getMessage(), t);
            }
        }, 5000, 5000, TimeUnit.MILLISECONDS);
        //new Thread(() -> syncLoop(cfgArgs), logger.getName()).start();
    }
}
