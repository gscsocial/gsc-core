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

package org.gsc.services.interfaceOnConfirmed.http;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.net.node.NodeInfo;
import org.gsc.services.http.Util;
import org.gsc.services.interfaceOnConfirmed.NodeInfoOnConfirmedService;


@Component
@Slf4j(topic = "API")
public class GetNodeInfoOnConfirmedServlet extends HttpServlet {

    @Autowired
    private NodeInfoOnConfirmedService nodeInfoService;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            NodeInfo nodeInfo = nodeInfoService.getNodeInfo();
            response.getWriter().println(JSON.toJSONString(nodeInfo));
        } catch (Exception e) {
            logger.error("", e);
            try {
                response.getWriter().println(Util.printErrorMsg(e));
            } catch (IOException ioe) {
                logger.debug("IOException: {}", ioe.getMessage());
            }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            NodeInfo nodeInfo = nodeInfoService.getNodeInfo();
            response.getWriter().println(JSON.toJSONString(nodeInfo));
        } catch (Exception e) {
            logger.error("", e);
            try {
                response.getWriter().println(Util.printErrorMsg(e));
            } catch (IOException ioe) {
                logger.debug("IOException: {}", ioe.getMessage());
            }
        }
    }
}
