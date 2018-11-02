package org.gsc.util;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.overlay.Parameter;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.utils.Utils;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.exception.HeaderNotFound;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.crypto.ECKey;
import org.gsc.db.Manager;
import org.gsc.db.api.pojo.Transaction;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.services.http.Util;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import static org.apache.commons.codec.digest.DigestUtils.sha256;

@Slf4j
public class PrivKeyToPubKey {



    @Test
    public void privKeyToPubKey() {
        String privStr = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";
        BigInteger privKey = new BigInteger(privStr, 16);

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

        final ECKey ecKey = ECKey.fromPrivate(privKey);
        byte[] address = ecKey.getAddress();

        String pubkey = Wallet.encode58Check(address);
        byte[] decodeAddr = Wallet.decodeFromBase58Check(pubkey);

        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Private Key: " + privStr);
        System.out.println("Address(Base58): " + Hex.toHexString(address));
        System.out.println("Public  Key: " + Hex.toHexString(ecKey.getPubKey()));
        System.out.println("Public  Key(Base58): " + pubkey);
        System.out.println();
        logger.info("---------------------------------------------");

        String b="26cd2a3d9f938e13cd947ec05abc7fe734df8dd826";
        String g="26" + "cd2a3d9f938e13cd947ec05abc7fe734df8dd826";
       // String Base58Address = "GNPhKboo7ez2MDH88qnwLGM5Vwr5vYSwb6";
        //byte[] Baddress = Wallet.decodeFromBase58Check(Base58Address);
        String Gaddress = Wallet.encode58Check(Hex.decode(g));

        System.out.println(Hex.toHexString(Wallet.decodeFromBase58Check("GcYjYPW92ezr3JUVh9exppDXaK4hzzuqnG")));
        logger.info("Baddress Key: " + Hex.toHexString(address));
        logger.info("Gaddress Key: " + Gaddress);
    }

    @Test
    public void generateAddress(){
        ECKey ecKey = new ECKey(Utils.getRandom());
        byte[] priKey = ecKey.getPrivKeyBytes();
        byte[] address = ecKey.getAddress();
        logger.info("Private Key: " + Hex.toHexString(priKey));
        logger.info("Hex Address: " + ByteArray.toHexString(address));
        logger.info("GSC Address: " + Wallet.encode58Check(address));
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

    @Test
    public void sign(){

        Manager dbManager = null; // @Autowired

        String privStr = "ad146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d0";
        BigInteger privKey = new BigInteger(privStr, 16);

        ECKey ecKey = ECKey.fromPrivate(privKey);

        Contract.TransferContract.Builder transferContract = Contract.TransferContract.newBuilder();
        transferContract.setOwnerAddress(ByteString.copyFrom(ecKey.getAddress()));
        transferContract.setToAddress(ByteString.copyFrom(new ECKey(Utils.getRandom()).getAddress()));
        transferContract.setAmount(1000000000000000L);

        TransactionWrapper transactionWrapper =  new TransactionWrapper(transferContract.build(),
                Protocol.Transaction.Contract.ContractType.TransferContract);

        try {
            BlockWrapper headBlock = null;
            List<BlockWrapper> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
            if (CollectionUtils.isEmpty(blockList)) {
                logger.error("latest block not found");
            } else {
                headBlock = blockList.get(0);
            }
            transactionWrapper.setReference(headBlock.getNum(), headBlock.getBlockId().getBytes());
            long expiration = headBlock.getTimeStamp() + Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;
            transactionWrapper.setExpiration(expiration);
            transactionWrapper.setTimestamp();
        } catch (Exception e) {
            logger.error("Header not found.");
            e.printStackTrace();
        }

        Protocol.Transaction transaction = transactionWrapper.getInstance();

        Protocol.Transaction.Builder txSigned = transaction.toBuilder();
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = sha256(rawData);
        List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
        for (int i = 0; i < contractList.size(); i++) {
            ECKey.ECDSASignature signature = ecKey.sign(hash);
            ByteString byteString = ByteString.copyFrom(signature.toByteArray());
            txSigned.addSignature(byteString);
        }

        logger.info("\n Transcation: " + Util.printTransaction(txSigned.build()));
    }

}
