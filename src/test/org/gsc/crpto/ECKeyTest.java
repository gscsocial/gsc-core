package org.gsc.crpto;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.Parameter;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.jce.ECKeyPairGenerator;
import org.junit.Test;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.encoders.Hex;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

import static org.junit.Assert.assertEquals;

@Slf4j
public class ECKeyTest {

    @Test
    public void testGeClientTestEcKey() {
//        Security.addProvider(new BouncyCastleProvider());
//        KeyPair keyPair = ECKeyPairGenerator.generateKeyPair();
//        PrivateKey privateKey = keyPair.getPrivate();
//        System.out.println(Hex.toHexString(privateKey.getEncoded()));
//        PublicKey publicKey = keyPair.getPublic();
//        ECKey ecKey = ECKey.fromPrivate(privateKey.getEncoded());
//
//        logger.info("address = {}", ByteArray.toHexString(ecKey.getAddress()));
//
//        assertEquals(Parameter.WalletConstant.ADD_PRE_FIX_STRING_MAINNET + "125b6c87b3d67114b3873977888c34582f27bbb0",
//                ByteArray.toHexString(ecKey.getAddress()));
    }
}
