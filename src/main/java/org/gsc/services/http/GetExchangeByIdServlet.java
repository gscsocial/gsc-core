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


@Component
@Slf4j(topic = "API")
public class GetExchangeByIdServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String input = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(input);
            boolean visible = Util.getVisiblePost(input);
            JSONObject jsonObject = JSONObject.parseObject(input);
            long id = Util.getJsonLongValue(jsonObject, "id", true);
            response.getWriter()
                    .println(JsonFormat
                            .printToString(wallet.getExchangeById(ByteString.copyFrom(ByteArray.fromLong(id))),
                                    visible));
        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
            try {
                response.getWriter().println(Util.printErrorMsg(e));
            } catch (IOException ioe) {
                logger.debug("IOException: {}", ioe.getMessage());
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            boolean visible = Util.getVisible(request);
            String input = request.getParameter("id");
            response.getWriter()
                    .println(JsonFormat.printToString(wallet
                                    .getExchangeById(ByteString.copyFrom(ByteArray.fromLong(Long.parseLong(input)))),
                            visible));
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