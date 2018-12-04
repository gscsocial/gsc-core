package org.gsc.util;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.wrapper.*;
import org.gsc.services.http.JsonFormat;
import org.gsc.services.http.Util;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

@Slf4j
public class SelectLevelDBData {

    // public static String path = "/home/kay/workspace/ethereum/source/ethereumj/database/";
    public static String path = "/home/kay/workspace/mico/source/gsc-core/output-directory/database/";
    // public static String path = "/home/kay/Desktop/gsc-full1/output-directory/database/";

    // account  contract  block gsc-solidity full properties vote votes witness proposal peers
    // "/home/kay/workspace/mico/gsc-core/output-directory/database/";
    // "/home/kay/Desktop/gsc-full1/output-directory/database/";

    public static void main(String[] args) {
         data("asset-issue");
        // data("properties");
        // data("peers");
        // data("block");
        // data("block");
        // data("account");
        // data("witness");
        // data("witness_schedule");
        // data("votes");
        // data("trans");
        // data("transactionHistoryStore");
    }

    public static void data(String dataName) {
        Options options = new Options();
        options.createIfMissing(true);
        DB db = null;
        try {
            System.out.println("Path: " + path + dataName);
            db = factory.open(new File(path + dataName), options);

            logger.info("---------------------------------------------");
            System.out.println();
            DBIterator iterator = db.iterator();
            iterator.seekToFirst();
            int count = 0;
            while (iterator.hasNext()) {
                count++;
                switch (dataName) {
                    case "properties":
                        properties(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "block":
                        block(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "account":
                        account(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "witness":
                        witness(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "witness_schedule":
                        witness_schedule(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "votes":
                        votes(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "trans":
                        trans(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "transactionHistoryStore":
                        transactionHistoryStore(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "peers":
                        peers(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "asset-issue":
                        assetissue(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    default:
                        break;
                }
                iterator.next();
            }
            iterator.close();
            System.out.println(dataName + " Num: " + count);
            System.out.println();
            logger.info("---------------------------------------------");

            db.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadItemException e) {
            e.printStackTrace();
        }
    }

    public  static void assetissue(byte[] key, byte[] value) {

        String keyStr = ByteString.copyFrom(key).toStringUtf8();
        AssetIssueWrapper assetIssueWrapper = new AssetIssueWrapper(value);
        String valueStr = JsonFormat.printToString(assetIssueWrapper.getInstance());

        System.out.println("key:" + keyStr + ", value:" + valueStr);
    }

    public  static void peers(byte[] key, byte[] value) {

        String keyStr = ByteArray.toHexString(key);
        String valueStr = ByteArray.toHexString(value);

        System.out.println("key:" + keyStr + ", value:" + valueStr);
    }

    public static void block(byte[] key, byte[] value) throws BadItemException {

        String keyStr = ByteArray.toHexString(key);
        BlockWrapper blockWrapper = new BlockWrapper(value);
        String valueStr = Util.printBlock(blockWrapper.getInstance());

        System.out.println("key:" + keyStr + ", value:" + valueStr);
        blockWrapper.getInstance().getTransactionsList().forEach(transaction -> {
            System.out.println(Util.printTransaction(transaction));
        });
    }

    public static void properties(byte[] key, byte[] value) {

        String keyStr = ByteString.copyFrom(key).toStringUtf8();
        long valueLong = ByteArray.toLong(value);

        System.out.println("key:" + keyStr + ", value:" + valueLong);
    }

    public static void account(byte[] key, byte[] value) {

        String keyStr = ByteArray.toHexString(key);
        AccountWrapper accountWrapper = new AccountWrapper(value);
        String valueStr = JsonFormat.printToString(accountWrapper.getInstance());

        System.out.println("key:" + keyStr + ", value:" + valueStr);
    }

    public static void witness(byte[] key, byte[] value) {

        String keyStr = ByteArray.toHexString(key);
        WitnessWrapper witnessWrapper = new WitnessWrapper(value);
        String valueStr = JsonFormat.printToString(witnessWrapper.getInstance());

        System.out.println("key:" + keyStr + ", value:" + valueStr);
    }

    public static void witness_schedule(byte[] key, byte[] value) {

        String keyStr = ByteArray.toHexString(key);
        WitnessWrapper witnessWrapper = new WitnessWrapper(value);
        String valueStr = JsonFormat.printToString(witnessWrapper.getInstance());

        System.out.println("key:" + keyStr + ", value:" + valueStr);
    }

    public  static void votes(byte[] key, byte[] value) {

        String keyStr = ByteArray.toHexString(key);
        VotesWrapper votesWrapper = new VotesWrapper(value);
        String valueStr = JsonFormat.printToString(votesWrapper.getInstance());

        System.out.println("key:" + keyStr + ", value:" + valueStr);
    }

    public static void trans(byte[] key, byte[] value) throws BadItemException {

        String keyStr = ByteArray.toHexString(key);
        TransactionWrapper transactionWrapper = new TransactionWrapper(value);
        String valueStr = JsonFormat.printToString(transactionWrapper.getInstance());

        System.out.println("key:" + keyStr + ", value:" + valueStr);
    }

    public static void transactionHistoryStore(byte[] key, byte[] value) throws BadItemException {

        String keyStr = ByteArray.toHexString(key);
        TransactionWrapper transactionWrapper = new TransactionWrapper(value);
        String valueStr = JsonFormat.printToString(transactionWrapper.getInstance());

        System.out.println("key:" + keyStr + ", value:" + valueStr);
    }
}
