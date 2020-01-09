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
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.utils.ByteArray;
import org.gsc.core.Wallet;
import org.gsc.protos.Protocol.Account;


@Component
@Slf4j(topic = "API")
public class GetAccountByIdServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            boolean visible = Util.getVisible(request);
            String accountId = request.getParameter("account_id");
            Account.Builder build = Account.newBuilder();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("account_id", accountId);
            JsonFormat.merge(jsonObject.toJSONString(), build, visible);

            Account reply = wallet.getAccountById(build.build());
            if (reply != null) {
                if (visible) {
                    response.getWriter().println(JsonFormat.printToString(reply, true));

                } else {
                    response.getWriter().println(convertOutput(reply));
                }
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String account = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(account);
            boolean visible = Util.getVisiblePost(account);
            Account.Builder build = Account.newBuilder();
            JsonFormat.merge(account, build, visible);

            Account reply = wallet.getAccountById(build.build());
            if (reply != null) {
                if (visible) {
                    response.getWriter().println(JsonFormat.printToString(reply, true));
                } else {
                    response.getWriter().println(convertOutput(reply));
                }
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

    private String convertOutput(Account account) {
        // convert asset id
        if (account.getAssetIssuedID().isEmpty()) {
            return JsonFormat.printToString(account, false);
        } else {
            JSONObject accountJson = JSONObject.parseObject(JsonFormat.printToString(account, false));
            String assetId = accountJson.get("asset_issued_ID").toString();
            accountJson.put(
                    "asset_issued_ID", ByteString.copyFrom(ByteArray.fromHexString(assetId)).toStringUtf8());
            return accountJson.toJSONString();
        }
    }

}