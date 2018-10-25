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
        String privStr = "fd146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d6";
        BigInteger privKey = new BigInteger(privStr, 16);

        String str = "6f74686572206572726f72203a20546865206b657920617267756d656e742063616e6e6f74206265206e756c6c";

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

        final ECKey ecKey = ECKey.fromPrivate(privKey);
        byte[] address = ecKey.getAddress();
        String pubkey = Wallet.encode58Check(address);
        byte[] decodeAddr = Wallet.decodeFromBase58Check(pubkey);

        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Private Key: " + privKey);
        System.out.println("Address: " + Hex.toHexString(address));
        System.out.println("Address(Base58): " + Hex.toHexString(address));
        System.out.println("Public  Key: " + Hex.toHexString(ecKey.getPubKey()));
        System.out.println("Public  Key(Base58): " + pubkey);
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

    @Test
    public void toHexString(){
        String str = "http://Mercury.org";
        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Hex String: " + Hex.toHexString(str.getBytes()));
        System.out.println();
        logger.info("---------------------------------------------");
    }

    @Test
    public void ByteToString(){
        String str = "6f74686572206572726f72203a20546865206b657920617267756d656e742063616e6e6f74206265206e756c6c";
        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Hex String: " + hexStr2Str(str));
        System.out.println();
        logger.info("---------------------------------------------");
    }

    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length;i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

}
