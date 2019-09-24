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

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.GrpcAPI.BlockList;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.core.Wallet;


@Component
@Slf4j(topic = "API")
public class GetBlockByLatestNumServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;
    private static final long BLOCK_LIMIT_NUM = 100;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            boolean visible = Util.getVisible(request);
            long getNum = Long.parseLong(request.getParameter("num"));
            if (getNum > 0 && getNum < BLOCK_LIMIT_NUM) {
                BlockList reply = wallet.getBlockByLatestNum(getNum);
                if (reply != null) {
                    response.getWriter().println(Util.printBlockList(reply, visible));
                    return;
                }
            }
            response.getWriter().println("{}");
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
            String input = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(input);
            boolean visible = Util.getVisiblePost(input);
            NumberMessage.Builder build = NumberMessage.newBuilder();
            JsonFormat.merge(input, build, visible);
            long getNum = build.getNum();
            if (getNum > 0 && getNum < BLOCK_LIMIT_NUM) {
                BlockList reply = wallet.getBlockByLatestNum(getNum);
                if (reply != null) {
                    response.getWriter().println(Util.printBlockList(reply, visible));
                    return;
                }
            }
            response.getWriter().println("{}");
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