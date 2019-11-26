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
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import java.io.File;
import java.io.IOException;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

@Slf4j
public class SelectLevelDBData {

    public static String path = "/home/kay/workspace/gsc/source/java-gsc/db-directory/database/";

    public static void main(String[] args) {

         data("properties");
        // data("peers");
    }

    public static void data(String dataName) {
        Options options = new Options();
        //options.createIfMissing(true);
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
                    case "peers":
                        peers(iterator.peekNext().getKey(), iterator.peekNext().getValue());
                        break;
                    case "nodes":
                        peers(iterator.peekNext().getKey(), iterator.peekNext().getValue());
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

}
