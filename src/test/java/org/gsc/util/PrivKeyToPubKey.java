package org.gsc.util;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.overlay.Parameter;
import org.gsc.core.Wallet;
import org.gsc.crypto.ECKey;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

@Slf4j
public class PrivKeyToPubKey {

    @Test
    public void privKeyToPubKey() {
        String privStr = "da146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d0";
        BigInteger privKey = new BigInteger(privStr, 16);

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

        final ECKey ecKey = ECKey.fromPrivate(privKey);
        byte[] address = ecKey.getAddress();
        String pubkey = Wallet.encode58Check(address);

        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Private Key: " + privKey);
        System.out.println("Address: " + ByteString.copyFrom(address));
        System.out.println("Public  Key: " + pubkey);
        System.out.println();
        logger.info("---------------------------------------------");
    }

    @Test
    public void addressTest() {
        String ownerAddress = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
        byte[] address = Hex.decode(ownerAddress);
        logger.info("---------------------------------------------");
        System.out.println();
        //System.out.println("Address: " + ByteString.copyFrom(address.toString());
        System.out.println();
        logger.info("---------------------------------------------");
    }
}
