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

import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.GrpcAPI.AccountPaginated;
import org.gsc.api.GrpcAPI.TransactionList;
import org.gsc.core.WalletConfirmed;
import org.gsc.services.http.JsonFormat;
import org.gsc.services.http.Util;


@Component
@Slf4j(topic = "API")
public class GetTransactionsFromThisServlet extends HttpServlet {

    @Autowired
    private WalletConfirmed walletConfirmed;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String input = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(input);
            boolean visible = Util.getVisiblePost(input);
            AccountPaginated.Builder builder = AccountPaginated.newBuilder();
            JsonFormat.merge(input, builder, visible);
            AccountPaginated accountPaginated = builder.build();
            ByteString thisAddress = accountPaginated.getAccount().getAddress();
            long offset = accountPaginated.getOffset();
            long limit = accountPaginated.getLimit();
            if (thisAddress != null && offset >= 0 && limit >= 0) {
                TransactionList list = walletConfirmed.getTransactionsFromThis(thisAddress, offset, limit);
                resp.getWriter().println(Util.printTransactionList(list, visible));
            } else {
                resp.getWriter().print("{}");
            }

        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
            try {
                resp.getWriter().println(e.getMessage());
            } catch (IOException ioe) {
                logger.debug("IOException: {}", ioe.getMessage());
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

    }
}
