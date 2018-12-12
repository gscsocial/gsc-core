package org.gsc.wallet.account;

import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.overlay.Configuration;

@Slf4j
public class WalletTest001 {
    ManagedChannel fullChannel = null;

    String fullNode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);


}
