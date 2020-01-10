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

import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TransactionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.GrpcAPI.Return.response_code;
import org.gsc.api.GrpcAPI.TransactionExtention;
import org.gsc.utils.ByteArray;
import org.gsc.core.Wallet;
import org.gsc.core.exception.ContractValidateException;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j(topic = "API")
public class TriggerConstantContractServlet extends HttpServlet {
    private final String functionSelector = "function_selector";

    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

    protected void validateParameter(String contract) {
        JSONObject jsonObject = JSONObject.parseObject(contract);
        if (!jsonObject.containsKey("owner_address")
                || StringUtil.isNullOrEmpty(jsonObject.getString("owner_address"))) {
            throw new InvalidParameterException("owner_address isn't set.");
        }
        if (!jsonObject.containsKey("contract_address")
                || StringUtil.isNullOrEmpty(jsonObject.getString("contract_address"))) {
            throw new InvalidParameterException("contract_address isn't set.");
        }
        if (!jsonObject.containsKey(functionSelector)
                || StringUtil.isNullOrEmpty(jsonObject.getString(functionSelector))) {
            throw new InvalidParameterException("function_selector isn't set.");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        TriggerSmartContract.Builder build = TriggerSmartContract.newBuilder();
        TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
        Return.Builder retBuilder = Return.newBuilder();
        boolean visible = false;
        try {
            String contract = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(contract);
            visible = Util.getVisiblePost(contract);
            validateParameter(contract);
            JsonFormat.merge(contract, build, visible);
            JSONObject jsonObject = JSONObject.parseObject(contract);
            String selector = jsonObject.getString(functionSelector);
            String parameter = jsonObject.getString("parameter");
            String data = Util.parseMethod(selector, parameter);
            build.setData(ByteString.copyFrom(ByteArray.fromHexString(data)));
            long feeLimit = Util.getJsonLongValue(jsonObject, "fee_limit");

            TransactionWrapper trxCap = wallet
                    .createTransactionWrapper(build.build(), ContractType.TriggerSmartContract);

            Transaction.Builder txBuilder = trxCap.getInstance().toBuilder();
            Transaction.raw.Builder rawBuilder = trxCap.getInstance().getRawData().toBuilder();
            rawBuilder.setFeeLimit(feeLimit);
            txBuilder.setRawData(rawBuilder);

            Transaction trx = wallet
                    .triggerConstantContract(build.build(), new TransactionWrapper(txBuilder.build()),
                            trxExtBuilder,
                            retBuilder);
            trx = Util.setTransactionPermissionId(jsonObject, trx);
            trxExtBuilder.setTransaction(trx);
            retBuilder.setResult(true).setCode(response_code.SUCCESS);

        } catch (ContractValidateException e) {
            retBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getMessage()));

        } catch (Exception e) {
            String errString = null;
            if (e.getMessage() != null) {
                errString = e.getMessage().replaceAll("[\"]", "\'");
            }
            retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + errString));
        }
        trxExtBuilder.setResult(retBuilder);
        response.getWriter().println(Util.printTransactionExtention(trxExtBuilder.build(), visible));
    }
}