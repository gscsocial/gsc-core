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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.gsc.utils.ByteArray;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;


@Component
@Slf4j(topic = "API")
public class ValidateAddressServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String input = request.getParameter("address");
        try {
            response.getWriter().println(validAddress(input));
        } catch (IOException e) {
            logger.debug("IOException: {}", e.getMessage());
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String input = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(input);
            JSONObject jsonAddress = JSON.parseObject(input);
            response.getWriter().println(validAddress(jsonAddress.getString("address")));
        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
        }
    }

    private String validAddress(String input) {
        byte[] address = null;
        boolean result = true;
        String msg;
        try {
            if (input.length() == Constant.ADDRESS_SIZE) {
                //hex
                address = ByteArray.fromHexString(input);
                msg = "Hex string format";
            } else if (input.length() == 34) {
                //base58check
                address = Wallet.decodeFromBase58Check(input);
                msg = "Base58check format";
            } else if (input.length() == 28) {
                //base64
                address = Base64.getDecoder().decode(input);
                msg = "Base64 format";
            } else {
                result = false;
                msg = "Length error";
            }
            if (result) {
                result = Wallet.addressValid(address);
                if (!result) {
                    msg = "Invalid address";
                }
            }
        } catch (Exception e) {
            result = false;
            msg = e.getMessage();
        }

        JSONObject jsonAddress = new JSONObject();
        jsonAddress.put("result", result);
        jsonAddress.put("message", msg);

        return jsonAddress.toJSONString();
    }
}