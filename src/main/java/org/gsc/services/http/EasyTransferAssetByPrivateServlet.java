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

import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.EasyTransferAssetByPrivateMessage;
import org.gsc.api.GrpcAPI.EasyTransferResponse;
import org.gsc.api.GrpcAPI.Return.response_code;
import org.gsc.crypto.ECKey;
import org.gsc.core.Wallet;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class EasyTransferAssetByPrivateServlet extends HttpServlet {

    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        GrpcAPI.Return.Builder returnBuilder = GrpcAPI.Return.newBuilder();
        EasyTransferResponse.Builder responseBuild = EasyTransferResponse.newBuilder();
        boolean visible = false;
        try {
            String input = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            visible = Util.getVisiblePost(input);
            EasyTransferAssetByPrivateMessage.Builder build = EasyTransferAssetByPrivateMessage
                    .newBuilder();
            JsonFormat.merge(input, build, visible);
            byte[] privateKey = build.getPrivateKey().toByteArray();
            ECKey ecKey = ECKey.fromPrivate(privateKey);
            byte[] owner = ecKey.getAddress();
            TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
            builder.setOwnerAddress(ByteString.copyFrom(owner));
            builder.setToAddress(build.getToAddress());
            builder.setAssetName(ByteString.copyFrom(build.getAssetId().getBytes()));
            builder.setAmount(build.getAmount());

            TransactionWrapper transactionWrapper;
            transactionWrapper = wallet
                    .createTransactionWrapper(builder.build(), ContractType.TransferAssetContract);
            transactionWrapper.sign(privateKey);
            GrpcAPI.Return retur = wallet.broadcastTransaction(transactionWrapper.getInstance());
            responseBuild.setTransaction(transactionWrapper.getInstance());
            responseBuild.setResult(retur);
            response.getWriter().println(Util.printEasyTransferResponse(responseBuild.build(), visible));
        } catch (Exception e) {
            returnBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getMessage()));
            responseBuild.setResult(returnBuilder.build());
            try {
                response.getWriter().println(JsonFormat.printToString(responseBuild.build(), visible));
            } catch (IOException ioe) {
                logger.debug("IOException: {}", ioe.getMessage());
            }
            return;
        }
    }
}
