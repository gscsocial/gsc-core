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

package org.gsc.services.http.confirmed;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.application.Service;
import org.gsc.config.args.Args;
import org.gsc.services.http.*;


@Component
@Slf4j(topic = "API")
public class ConfirmedHttpApiService implements Service {

    private int port = Args.getInstance().getConfirmedHttpPort();

    private Server server;

    @Autowired
    private GetAccountServlet getAccountServlet;

    @Autowired
    private GetTransactionByIdConfirmedServlet getTransactionByIdServlet;
    @Autowired
    private GetTransactionInfoByIdConfirmedServlet getTransactionInfoByIdServlet;
    @Autowired
    private GetTransactionsFromThisServlet getTransactionsFromThisServlet;
    @Autowired
    private GetTransactionsToThisServlet getTransactionsToThisServlet;
    @Autowired
    private GetTransactionCountByBlockNumServlet getTransactionCountByBlockNumServlet;
    @Autowired
    private GetDelegatedResourceServlet getDelegatedResourceServlet;
    @Autowired
    private GetDelegatedResourceAccountIndexServlet getDelegatedResourceAccountIndexServlet;
    @Autowired
    private GetExchangeByIdServlet getExchangeByIdServlet;
    @Autowired
    private ListExchangesServlet listExchangesServlet;

    @Autowired
    private ListWitnessesServlet listWitnessesServlet;
    @Autowired
    private GetAssetIssueListServlet getAssetIssueListServlet;
    @Autowired
    private GetPaginatedAssetIssueListServlet getPaginatedAssetIssueListServlet;
    @Autowired
    private GetAssetIssueByNameServlet getAssetIssueByNameServlet;
    @Autowired
    private GetAssetIssueByIdServlet getAssetIssueByIdServlet;
    @Autowired
    private GetAssetIssueListByNameServlet getAssetIssueListByNameServlet;
    @Autowired
    private GetNowBlockServlet getNowBlockServlet;
    @Autowired
    private GetBlockByNumServlet getBlockByNumServlet;
    @Autowired
    private GetNodeInfoServlet getNodeInfoServlet;
    @Autowired
    private GetAccountByIdServlet getAccountByIdServlet;
    @Autowired
    private GetBlockByIdServlet getBlockByIdServlet;
    @Autowired
    private GetBlockByLimitNextServlet getBlockByLimitNextServlet;
    @Autowired
    private GetBlockByLatestNumServlet getBlockByLatestNumServlet;

    @Override
    public void init() {

    }

    @Override
    public void init(Args args) {

    }

    @Override
    public void start() {
        Args args = Args.getInstance();
        try {
            server = new Server(port);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            // same as Start
            context.addServlet(new ServletHolder(getAccountServlet), "/walletconfirmed/getaccount");
            context.addServlet(new ServletHolder(listWitnessesServlet), "/walletconfirmed/listwitnesses");
            context.addServlet(new ServletHolder(getAssetIssueListServlet),
                    "/walletconfirmed/getassetissuelist");
            context.addServlet(new ServletHolder(getPaginatedAssetIssueListServlet),
                    "/walletconfirmed/getpaginatedassetissuelist");
            context.addServlet(new ServletHolder(getAssetIssueByNameServlet),
                    "/walletconfirmed/getassetissuebyname");
            context.addServlet(new ServletHolder(getAssetIssueByIdServlet),
                    "/walletconfirmed/getassetissuebyid");
            context.addServlet(new ServletHolder(getAssetIssueListByNameServlet),
                    "/walletconfirmed/getassetissuelistbyname");
            context.addServlet(new ServletHolder(getNowBlockServlet), "/walletconfirmed/getnowblock");
            context.addServlet(new ServletHolder(getBlockByNumServlet), "/walletconfirmed/getblockbynum");
            context.addServlet(new ServletHolder(getDelegatedResourceServlet),
                    "/walletconfirmed/getdelegatedresource");
            context.addServlet(new ServletHolder(getDelegatedResourceAccountIndexServlet),
                    "/walletconfirmed/getdelegatedresourceaccountindex");
            context
                    .addServlet(new ServletHolder(getExchangeByIdServlet),
                            "/walletconfirmed/getexchangebyid");
            context.addServlet(new ServletHolder(listExchangesServlet),
                    "/walletconfirmed/listexchanges");

            context.addServlet(new ServletHolder(getAccountByIdServlet),
                    "/walletconfirmed/getaccountbyid");
            context.addServlet(new ServletHolder(getBlockByIdServlet),
                    "/walletconfirmed/getblockbyid");
            context.addServlet(new ServletHolder(getBlockByLimitNextServlet),
                    "/walletconfirmed/getblockbylimitnext");
            context.addServlet(new ServletHolder(getBlockByLatestNumServlet),
                    "/walletconfirmed/getblockbylatestnum");

            // only for ConfirmedNode
            context.addServlet(new ServletHolder(getTransactionByIdServlet),
                    "/walletconfirmed/gettransactionbyid");

            context
                    .addServlet(new ServletHolder(getTransactionInfoByIdServlet),
                            "/walletconfirmed/gettransactioninfobyid");
            context
                    .addServlet(new ServletHolder(getTransactionCountByBlockNumServlet),
                            "/walletconfirmed/gettransactioncountbyblocknum");
            // for extension api
            if (args.isWalletExtensionApi()) {
                context.addServlet(new ServletHolder(getTransactionsFromThisServlet),
                        "/walletextension/gettransactionsfromthis");
                context
                        .addServlet(new ServletHolder(getTransactionsToThisServlet),
                                "/walletextension/gettransactionstothis");
            }
            context.addServlet(new ServletHolder(getNodeInfoServlet), "/wallet/getnodeinfo");
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
            logger.debug("Exception: {}", e.getMessage());
        }
    }
}
