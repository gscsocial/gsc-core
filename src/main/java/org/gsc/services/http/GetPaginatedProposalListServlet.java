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
import org.gsc.api.GrpcAPI.PaginatedMessage;
import org.gsc.api.GrpcAPI.ProposalList;
import org.gsc.core.Wallet;


@Component
@Slf4j(topic = "API")
public class GetPaginatedProposalListServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String input = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(input);
            boolean visible = Util.getVisiblePost(input);
            PaginatedMessage.Builder build = PaginatedMessage.newBuilder();
            JsonFormat.merge(input, build, visible);
            ProposalList reply = wallet.getPaginatedProposalList(build.getOffset(), build.getLimit());
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
}
