package org.gsc.keystore;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.AddressUtil;
import org.gsc.common.utils.ByteArray;
import org.gsc.crypto.jce.SpongyCastleProvider;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

@Slf4j
public class WalletUtilsTest {

    private String FILE_PATH = WalletUtils.getDefaultKeyDirectory()+File.separator;

    @Test
    public void validPasswordFormat(){
        Assert.assertFalse(WalletUtils.passwordValid(""));
        Assert.assertFalse(WalletUtils.passwordValid("123cb"));
    }
    @Test
    public void testGenerateFullNewWalletFile() throws CipherException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
//        Security.addProvider(new BouncyCastleProvider());
//        SpongyCastleProvider.getInstance();
//        String fileName = WalletUtils.generateFullNewWalletFile("1ab321",new File(FILE_PATH));
//        System.out.println("full file name :"+fileName);
//        String lightFileName = WalletUtils.generateLightNewWalletFile("123bcx",new File(FILE_PATH));
//        System.out.println("light file name:"+lightFileName);
//        Credentials fulleCreadentials = WalletUtils.loadCredentials("1ab321",new File(FILE_PATH+fileName));
//        String address = fulleCreadentials.getAddress();
//        System.out.println("address："+address);
        Assert.assertTrue(AddressUtil.addressValid(ByteArray.fromString("LM9Z96QPSaZpuHaBstbE2rot6pfEof3zEv")));
    }

}
