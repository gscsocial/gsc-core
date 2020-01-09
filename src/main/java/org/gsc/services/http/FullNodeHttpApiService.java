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

package org.gsc.services.http;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.application.Service;
import org.gsc.config.args.Args;

@Component
@Slf4j(topic = "API")
public class FullNodeHttpApiService implements Service {

    private int port = Args.getInstance().getFullNodeHttpPort();

    private Server server;

    @Autowired
    private GetAccountServlet getAccountServlet;
    @Autowired
    private TransferServlet transferServlet;
    @Autowired
    private BroadcastServlet broadcastServlet;
    @Autowired
    private TransactionSignServlet transactionSignServlet;
    @Autowired
    private UpdateAccountServlet updateAccountServlet;
    @Autowired
    private VoteWitnessAccountServlet voteWitnessAccountServlet;
    @Autowired
    private CreateAssetIssueServlet createAssetIssueServlet;
    @Autowired
    private UpdateWitnessServlet updateWitnessServlet;
    @Autowired
    private CreateAccountServlet createAccountServlet;
    @Autowired
    private CreateWitnessServlet createWitnessServlet;
    @Autowired
    private TransferAssetServlet transferAssetServlet;
    @Autowired
    private ParticipateAssetIssueServlet participateAssetIssueServlet;
    @Autowired
    private FreezeBalanceServlet freezeBalanceServlet;
    @Autowired
    private UnFreezeBalanceServlet unFreezeBalanceServlet;
    @Autowired
    private UnFreezeAssetServlet unFreezeAssetServlet;
    @Autowired
    private UpdateAssetServlet updateAssetServlet;
    @Autowired
    private ListNodesServlet listNodesServlet;
    @Autowired
    private WithdrawBalanceServlet withdrawBalanceServlet;
    @Autowired
    private GetAssetIssueByAccountServlet getAssetIssueByAccountServlet;
    @Autowired
    private GetAccountNetServlet getAccountNetServlet;
    @Autowired
    private GetAssetIssueByNameServlet getAssetIssueByNameServlet;
    @Autowired
    private GetAssetIssueListByNameServlet getAssetIssueListByNameServlet;
    @Autowired
    private GetAssetIssueByIdServlet getAssetIssueByIdServlet;
    @Autowired
    private GetNowBlockServlet getNowBlockServlet;
    @Autowired
    private GetBlockByNumServlet getBlockByNumServlet;
    @Autowired
    private GetBlockByIdServlet getBlockByIdServlet;
    @Autowired
    private GetBlockByLimitNextServlet getBlockByLimitNextServlet;
    @Autowired
    private GetBlockByLatestNumServlet getBlockByLatestNumServlet;
    @Autowired
    private GetTransactionByIdServlet getTransactionByIdServlet;
    @Autowired
    private GetTransactionInfoByIdServlet getTransactionInfoByIdServlet;
    @Autowired
    private GetTransactionCountByBlockNumServlet getTransactionCountByBlockNumServlet;
    @Autowired
    private ListWitnessesServlet listWitnessesServlet;
    @Autowired
    private GetAssetIssueListServlet getAssetIssueListServlet;
    @Autowired
    private GetPaginatedAssetIssueListServlet getPaginatedAssetIssueListServlet;
    @Autowired
    private GetPaginatedProposalListServlet getPaginatedProposalListServlet;
    @Autowired
    private GetPaginatedExchangeListServlet getPaginatedExchangeListServlet;
    @Autowired
    private TotalTransactionServlet totalTransactionServlet;
    @Autowired
    private GetNextMaintenanceTimeServlet getNextMaintenanceTimeServlet;
    @Autowired
    private EasyTransferServlet easyTransferServlet;
    @Autowired
    private EasyTransferByPrivateServlet easyTransferByPrivateServlet;
    @Autowired
    private EasyTransferAssetServlet easyTransferAssetServlet;
    @Autowired
    private EasyTransferAssetByPrivateServlet easyTransferAssetByPrivateServlet;
    @Autowired
    private CreateAddressServlet createAddressServlet;
    @Autowired
    private GenerateAddressServlet generateAddressServlet;
    @Autowired
    private ValidateAddressServlet validateAddressServlet;
    @Autowired
    private DeployContractServlet deployContractServlet;
    @Autowired
    private TriggerSmartContractServlet triggerSmartContractServlet;
    @Autowired
    private TriggerConstantContractServlet triggerConstantContractServlet;
    @Autowired
    private GetContractServlet getContractServlet;
    @Autowired
    private ClearABIServlet clearABIServlet;
    @Autowired
    private ProposalCreateServlet proposalCreateServlet;
    @Autowired
    private ProposalApproveServlet proposalApproveServlet;
    @Autowired
    private ProposalDeleteServlet proposalDeleteServlet;
    @Autowired
    private ListProposalsServlet listProposalsServlet;
    @Autowired
    private GetProposalByIdServlet getProposalByIdServlet;
    @Autowired
    private ExchangeCreateServlet exchangeCreateServlet;
    @Autowired
    private ExchangeInjectServlet exchangeInjectServlet;
    @Autowired
    private ExchangeTransactionServlet exchangeTransactionServlet;
    @Autowired
    private ExchangeWithdrawServlet exchangeWithdrawServlet;
    @Autowired
    private GetExchangeByIdServlet getExchangeByIdServlet;
    @Autowired
    private ListExchangesServlet listExchangesServlet;
    @Autowired
    private GetChainParametersServlet getChainParametersServlet;
    @Autowired
    private GetAccountResourceServlet getAccountResourceServlet;
    @Autowired
    private GetNodeInfoServlet getNodeInfoServlet;
    @Autowired
    private AddTransactionSignServlet addTransactionSignServlet;
    @Autowired
    private GetTransactionSignWeightServlet getTransactionSignWeightServlet;
    @Autowired
    private GetTransactionApprovedListServlet getTransactionApprovedListServlet;
    @Autowired
    private AccountPermissionUpdateServlet accountPermissionUpdateServlet;
    @Autowired
    private UpdateSettingServlet updateSettingServlet;
    @Autowired
    private UpdateCpuLimitServlet updateCpuLimitServlet;
    @Autowired
    private GetDelegatedResourceAccountIndexServlet getDelegatedResourceAccountIndexServlet;
    @Autowired
    private GetDelegatedResourceServlet getDelegatedResourceServlet;
    @Autowired
    private SetAccountIdServlet setAccountServlet;
    @Autowired
    private GetAccountByIdServlet getAccountByIdServlet;
    @Autowired
    private VoteStatisticsServlet voteStatisticsServlet;


    @Override
    public void init() {

    }

    @Override
    public void init(Args args) {
    }

    @Override
    public void start() {
        try {
            server = new Server(port);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/wallet/");
            server.setHandler(context);

            context.addServlet(new ServletHolder(getAccountServlet), "/getaccount");
            context.addServlet(new ServletHolder(transferServlet), "/createtransaction");
            context.addServlet(new ServletHolder(broadcastServlet), "/broadcasttransaction");
            context.addServlet(new ServletHolder(transactionSignServlet), "/gettransactionsign");
            context.addServlet(new ServletHolder(updateAccountServlet), "/updateaccount");
            context.addServlet(new ServletHolder(voteWitnessAccountServlet), "/votewitnessaccount");
            context.addServlet(new ServletHolder(createAssetIssueServlet), "/createassetissue");
            context.addServlet(new ServletHolder(updateWitnessServlet), "/updatewitness");
            context.addServlet(new ServletHolder(createAccountServlet), "/createaccount");
            context.addServlet(new ServletHolder(createWitnessServlet), "/createwitness");
            context.addServlet(new ServletHolder(transferAssetServlet), "/transferasset");
            context.addServlet(new ServletHolder(participateAssetIssueServlet), "/participateassetissue");
            context.addServlet(new ServletHolder(freezeBalanceServlet), "/freezebalance");
            context.addServlet(new ServletHolder(unFreezeBalanceServlet), "/unfreezebalance");
            context.addServlet(new ServletHolder(unFreezeAssetServlet), "/unfreezeasset");
            context.addServlet(new ServletHolder(withdrawBalanceServlet), "/withdrawbalance");
            context.addServlet(new ServletHolder(updateAssetServlet), "/updateasset");
            context.addServlet(new ServletHolder(listNodesServlet), "/listnodes");
            context.addServlet(
                    new ServletHolder(getAssetIssueByAccountServlet), "/getassetissuebyaccount");
            context.addServlet(new ServletHolder(getAccountNetServlet), "/getaccountnet");
            context.addServlet(new ServletHolder(getAssetIssueByNameServlet), "/getassetissuebyname");
            context.addServlet(new ServletHolder(getAssetIssueListByNameServlet),
                    "/getassetissuelistbyname");
            context.addServlet(new ServletHolder(getAssetIssueByIdServlet), "/getassetissuebyid");
            context.addServlet(new ServletHolder(getNowBlockServlet), "/getnowblock");
            context.addServlet(new ServletHolder(getBlockByNumServlet), "/getblockbynum");
            context.addServlet(new ServletHolder(getBlockByIdServlet), "/getblockbyid");
            context.addServlet(new ServletHolder(getBlockByLimitNextServlet), "/getblockbylimitnext");
            context.addServlet(new ServletHolder(getBlockByLatestNumServlet), "/getblockbylatestnum");
            context.addServlet(new ServletHolder(getTransactionByIdServlet), "/gettransactionbyid");

            context.addServlet(
                    new ServletHolder(getTransactionInfoByIdServlet), "/gettransactioninfobyid");
            context.addServlet(
                    new ServletHolder(getTransactionCountByBlockNumServlet),
                    "/gettransactioncountbyblocknum");
            context.addServlet(new ServletHolder(listWitnessesServlet), "/listwitnesses");
            context.addServlet(new ServletHolder(getAssetIssueListServlet), "/getassetissuelist");
            context.addServlet(
                    new ServletHolder(getPaginatedAssetIssueListServlet), "/getpaginatedassetissuelist");
            context.addServlet(
                    new ServletHolder(getPaginatedProposalListServlet), "/getpaginatedproposallist");
            context.addServlet(
                    new ServletHolder(getPaginatedExchangeListServlet), "/getpaginatedexchangelist");
            context.addServlet(new ServletHolder(totalTransactionServlet), "/totaltransaction");
            context.addServlet(
                    new ServletHolder(getNextMaintenanceTimeServlet), "/getnextmaintenancetime");
            context.addServlet(new ServletHolder(createAddressServlet), "/createaddress");
            context.addServlet(new ServletHolder(easyTransferServlet), "/easytransfer");
            context.addServlet(new ServletHolder(easyTransferByPrivateServlet), "/easytransferbyprivate");
            context.addServlet(new ServletHolder(easyTransferAssetServlet), "/easytransferasset");
            context.addServlet(new ServletHolder(easyTransferAssetByPrivateServlet),
                    "/easytransferassetbyprivate");
            context.addServlet(new ServletHolder(generateAddressServlet), "/generateaddress");
            context.addServlet(new ServletHolder(validateAddressServlet), "/validateaddress");
            context.addServlet(new ServletHolder(deployContractServlet), "/deploycontract");
            context.addServlet(new ServletHolder(triggerSmartContractServlet), "/triggersmartcontract");
            context.addServlet(new ServletHolder(triggerConstantContractServlet),
                    "/triggerconstantcontract");
            context.addServlet(new ServletHolder(getContractServlet), "/getcontract");
            context.addServlet(new ServletHolder(clearABIServlet), "/clearabi");
            context.addServlet(new ServletHolder(proposalCreateServlet), "/proposalcreate");
            context.addServlet(new ServletHolder(proposalApproveServlet), "/proposalapprove");
            context.addServlet(new ServletHolder(proposalDeleteServlet), "/proposaldelete");
            context.addServlet(new ServletHolder(listProposalsServlet), "/listproposals");
            context.addServlet(new ServletHolder(getProposalByIdServlet), "/getproposalbyid");
            context.addServlet(new ServletHolder(exchangeCreateServlet), "/exchangecreate");
            context.addServlet(new ServletHolder(exchangeInjectServlet), "/exchangeinject");
            context.addServlet(new ServletHolder(exchangeTransactionServlet), "/exchangetransaction");
            context.addServlet(new ServletHolder(exchangeWithdrawServlet), "/exchangewithdraw");
            context.addServlet(new ServletHolder(getExchangeByIdServlet), "/getexchangebyid");
            context.addServlet(new ServletHolder(listExchangesServlet), "/listexchanges");
            context.addServlet(new ServletHolder(getChainParametersServlet), "/getchainparameters");
            context.addServlet(new ServletHolder(getAccountResourceServlet), "/getaccountresource");
            context.addServlet(new ServletHolder(addTransactionSignServlet), "/addtransactionsign");
            context.addServlet(new ServletHolder(getTransactionSignWeightServlet), "/getsignweight");
            context.addServlet(new ServletHolder(getTransactionApprovedListServlet), "/getapprovedlist");
            context.addServlet(new ServletHolder(accountPermissionUpdateServlet),
                    "/accountpermissionupdate");
            context.addServlet(new ServletHolder(getNodeInfoServlet), "/getnodeinfo");
            context.addServlet(new ServletHolder(updateSettingServlet), "/updatesetting");
            context.addServlet(new ServletHolder(updateCpuLimitServlet), "/updatecpulimit");
            context.addServlet(new ServletHolder(getDelegatedResourceServlet), "/getdelegatedresource");
            context.addServlet(
                    new ServletHolder(getDelegatedResourceAccountIndexServlet),
                    "/getdelegatedresourceaccountindex");
            context.addServlet(new ServletHolder(setAccountServlet), "/setaccountid");
            context.addServlet(new ServletHolder(getAccountByIdServlet), "/getaccountbyid");
            context.addServlet(new ServletHolder(voteStatisticsServlet), "/voteStatistics");

            int maxHttpConnectNumber = Args.getInstance().getMaxHttpConnectNumber();
            if (maxHttpConnectNumber > 0) {
                server.addBean(new ConnectionLimit(maxHttpConnectNumber, server));
            }
            server.start();
        } catch (Exception e) {
            logger.debug("IOException: {}", e.getMessage());
        }
    }

    @Override
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            logger.debug("IOException: {}", e.getMessage());
        }
    }
}
