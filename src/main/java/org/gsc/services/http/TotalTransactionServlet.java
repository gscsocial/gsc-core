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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.core.Wallet;


@Component
@Slf4j(topic = "API")
public class TotalTransactionServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            boolean visible = Util.getVisible(request);
            NumberMessage reply = wallet.totalTransaction();
            if (reply != null) {
                response.getWriter().println(JsonFormat.printToString(reply, visible));
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
        doGet(request, response);
    }
}