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

package org.gsc.utils;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.services.http.Util;
import org.rocksdb.*;

@Slf4j
public class SelectRocksDBData {

    public static String path = "/home/kay/workspace/gsc/source/gsc-core/gsc-db/database/";

    public static void main(String[] args) throws BadItemException {

         data("dynamic_parameter");
//         data("block");
//        data("account");
    }

    public static void data(String dataName) throws BadItemException {
        Options options = new Options();
        //options.createIfMissing(true);
        RocksDB db = null;
        try {
            System.out.println("Path: " + path + dataName);
            db = RocksDB.open(options, path + dataName);

            ReadOptions readOpts = new ReadOptions();
            readOpts = readOpts.setPrefixSameAsStart(true)
                    .setVerifyChecksums(false);

            logger.info("---------------------------------------------");
            System.out.println();

            RocksIterator iterator = db.newIterator(readOpts);
            iterator.seekToFirst();

            int count = 0;
            while (iterator.isValid()) {
                count++;
                switch (dataName) {
                    case "dynamic_parameter":
                        properties(iterator.key(), iterator.value());
                        break;

                    case "peers":
                        peers(iterator.key(), iterator.value());
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
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }


    public  static void peers(byte[] key, byte[] value) {

        String keyStr = ByteArray.toHexString(key);
        String valueStr = ByteArray.toHexString(value);

        System.out.println("key:" + keyStr + ", value:" + valueStr);
    }

    public static void properties(byte[] key, byte[] value) {

        String keyStr = ByteString.copyFrom(key).toStringUtf8();
        long valueLong = ByteArray.toLong(value);

        System.out.println(keyStr + "\t:" + valueLong);
    }

    public static void block(byte[] key, byte[] value) throws BadItemException {

        String keyStr = ByteArray.toHexString(key);
        BlockWrapper blockWrapper = new BlockWrapper(value);
        String valueStr = Util.printBlock(blockWrapper.getInstance(), true);

        System.out.println("key:" + keyStr + ", value:" + valueStr);
        blockWrapper.getInstance().getTransactionsList().forEach(transaction -> {
            System.out.println(Util.printTransaction(transaction, true));
        });
    }

}
