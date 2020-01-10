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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;
import org.gsc.crypto.ECKey;
import org.gsc.utils.ByteArray;
import org.gsc.utils.Utils;
import org.gsc.core.Wallet;


@Component
@Slf4j(topic = "API")
public class GenerateAddressServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            ECKey ecKey = new ECKey(Utils.getRandom());
            byte[] priKey = ecKey.getPrivKeyBytes();
            byte[] address = ecKey.getAddress();
            String priKeyStr = Hex.encodeHexString(priKey);
            String base58check = Wallet.encode58Check(address);
            String hexString = ByteArray.toHexString(address);
            JSONObject jsonAddress = new JSONObject();
            jsonAddress.put("privateKey", priKeyStr);
            jsonAddress.put("address", base58check);
            jsonAddress.put("hexAddress", hexString);

            response.getWriter().println(jsonAddress.toJSONString());
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