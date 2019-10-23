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

package org.gsc.services.interfaceOnConfirmed.http.confirmed;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.gsc.application.Service;
import org.gsc.config.args.Args;
import org.gsc.services.interfaceOnConfirmed.http.*;

@Slf4j(topic = "API")
public class ConfirmedHttpApiService implements Service {

    private int port = Args.getInstance().getConfirmedHttpPort();

    private Server server;

    @Autowired
    private GetAccountOnConfirmedServlet accountOnConfirmedServlet;
    @Autowired
    private GetTransactionByIdOnConfirmedServlet getTransactionByIdOnConfirmedServlet;
    @Autowired
    private GetTransactionInfoByIdOnConfirmedServlet getTransactionInfoByIdOnConfirmedServlet;
    @Autowired
    private ListWitnessesOnConfirmedServlet listWitnessesOnConfirmedServlet;
    @Autowired
    private GetAssetIssueListOnConfirmedServlet getAssetIssueListOnConfirmedServlet;
    @Autowired
    private GetPaginatedAssetIssueListOnConfirmedServlet getPaginatedAssetIssueListOnConfirmedServlet;
    @Autowired
    private GetNowBlockOnConfirmedServlet getNowBlockOnConfirmedServlet;
    @Autowired
    private GetBlockByNumOnConfirmedServlet getBlockByNumOnConfirmedServlet;
    @Autowired
    private GetNodeInfoOnConfirmedServlet getNodeInfoOnConfirmedServlet;
    @Autowired
    private GetDelegatedResourceOnConfirmedServlet getDelegatedResourceOnConfirmedServlet;
    @Autowired
    private GetDelegatedResourceAccountIndexOnConfirmedServlet
            getDelegatedResourceAccountIndexOnConfirmedServlet;
    @Autowired
    private GetExchangeByIdOnConfirmedServlet getExchangeByIdOnConfirmedServlet;
    @Autowired
    private ListExchangesOnConfirmedServlet listExchangesOnConfirmedServlet;
    @Autowired
    private GetTransactionCountByBlockNumOnConfirmedServlet
            getTransactionCountByBlockNumOnConfirmedServlet;
    @Autowired
    private GetAssetIssueByNameOnConfirmedServlet getAssetIssueByNameOnConfirmedServlet;
    @Autowired
    private GetAssetIssueByIdOnConfirmedServlet getAssetIssueByIdOnConfirmedServlet;
    @Autowired
    private GetAssetIssueListByNameOnConfirmedServlet getAssetIssueListByNameOnConfirmedServlet;
    @Autowired
    private GetAccountByIdOnConfirmedServlet getAccountByIdOnConfirmedServlet;
    @Autowired
    private GetBlockByIdOnConfirmedServlet getBlockByIdOnConfirmedServlet;
    @Autowired
    private GetBlockByLimitNextOnConfirmedServlet getBlockByLimitNextOnConfirmedServlet;
    @Autowired
    private GetBlockByLatestNumOnConfirmedServlet getBlockByLatestNumOnConfirmedServlet;

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
            context.setContextPath("/");
            server.setHandler(context);

            // same as Start
            context.addServlet(new ServletHolder(accountOnConfirmedServlet), "/walletconfirmed/getaccount");
            context.addServlet(new ServletHolder(listWitnessesOnConfirmedServlet),
                    "/walletconfirmed/listwitnesses");
            context.addServlet(new ServletHolder(getAssetIssueListOnConfirmedServlet),
                    "/walletconfirmed/getassetissuelist");
            context.addServlet(new ServletHolder(getPaginatedAssetIssueListOnConfirmedServlet),
                    "/walletconfirmed/getpaginatedassetissuelist");
            context.addServlet(new ServletHolder(getAssetIssueByNameOnConfirmedServlet),
                    "/walletconfirmed/getassetissuebyname");
            context.addServlet(new ServletHolder(getAssetIssueByIdOnConfirmedServlet),
                    "/walletconfirmed/getassetissuebyid");
            context.addServlet(new ServletHolder(getAssetIssueListByNameOnConfirmedServlet),
                    "/walletconfirmed/getassetissuelistbyname");
            context.addServlet(new ServletHolder(getNowBlockOnConfirmedServlet),
                    "/walletconfirmed/getnowblock");
            context.addServlet(new ServletHolder(getBlockByNumOnConfirmedServlet),
                    "/walletconfirmed/getblockbynum");
            context.addServlet(new ServletHolder(getDelegatedResourceOnConfirmedServlet),
                    "/walletconfirmed/getdelegatedresource");
            context.addServlet(new ServletHolder(getDelegatedResourceAccountIndexOnConfirmedServlet),
                    "/walletconfirmed/getdelegatedresourceaccountindex");
            context.addServlet(new ServletHolder(getExchangeByIdOnConfirmedServlet),
                    "/walletconfirmed/getexchangebyid");
            context.addServlet(new ServletHolder(listExchangesOnConfirmedServlet),
                    "/walletconfirmed/listexchanges");
            context.addServlet(new ServletHolder(getAccountByIdOnConfirmedServlet),
                    "/walletconfirmed/getaccountbyid");
            context.addServlet(new ServletHolder(getBlockByIdOnConfirmedServlet),
                    "/walletconfirmed/getblockbyid");
            context.addServlet(new ServletHolder(getBlockByLimitNextOnConfirmedServlet),
                    "/walletconfirmed/getblockbylimitnext");
            context.addServlet(new ServletHolder(getBlockByLatestNumOnConfirmedServlet),
                    "/walletconfirmed/getblockbylatestnum");
            // only for ConfirmedNode
            context.addServlet(new ServletHolder(getTransactionByIdOnConfirmedServlet),
                    "/walletconfirmed/gettransactionbyid");
            context.addServlet(new ServletHolder(getTransactionInfoByIdOnConfirmedServlet),
                    "/walletconfirmed/gettransactioninfobyid");

            context.addServlet(new ServletHolder(getTransactionCountByBlockNumOnConfirmedServlet),
                    "/walletconfirmed/gettransactioncountbyblocknum");

            context.addServlet(new ServletHolder(getNodeInfoOnConfirmedServlet), "/wallet/getnodeinfo");
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
