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

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TransactionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.TransactionSign;


@Component
@Slf4j(topic = "API")
public class TransactionSignServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String contract = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(contract);
            JSONObject input = JSONObject.parseObject(contract);
            boolean visible = Util.getVisibleOnlyForSign(input);
            String strTransaction = input.getJSONObject("transaction").toJSONString();
            Transaction transaction = Util.packTransaction(strTransaction, visible);
            JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction,
                    visible));
            input.put("transaction", jsonTransaction);
            TransactionSign.Builder build = TransactionSign.newBuilder();
            JsonFormat.merge(input.toJSONString(), build, visible);
            TransactionWrapper reply = wallet.getTransactionSign(build.build());
            if (reply != null) {
                response.getWriter().println(Util.printCreateTransaction(reply.getInstance(), visible));
            } else {
                response.getWriter().println("{}");
            }
        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
            try {
                response.getWriter().println(Util.printErrorMsg(e));
            } catch (IOException ioe) {
                logger.debug("IOException: {}", ioe.getMessage());
            }
        }
    }
}
