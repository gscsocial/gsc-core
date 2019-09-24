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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.gsc.services.http.GetDelegatedResourceAccountIndexServlet;
import org.gsc.services.interfaceOnConfirmed.WalletOnConfirmed;

@Component
@Slf4j(topic = "API")
public class GetDelegatedResourceAccountIndexOnConfirmedServlet
        extends GetDelegatedResourceAccountIndexServlet {

    @Autowired
    private WalletOnConfirmed walletOnConfirmed;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        walletOnConfirmed.futureGet(() -> super.doGet(request, response));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        walletOnConfirmed.futureGet(() -> super.doPost(request, response));
    }
}
