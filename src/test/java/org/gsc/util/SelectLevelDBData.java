package org.gsc.util;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.junit.Test;
import static org.iq80.leveldb.impl.Iq80DBFactory.*;

import java.io.File;
import java.io.IOException;

@Slf4j
public class SelectLevelDBData {

    @Test
    public void achieveDBData() {

        Options options = new Options();
        options.createIfMissing(true);
        DB db = null;
        try {
            // account  contract  block
             db = factory.open(new File("/home/kay/workspace/mico/gsc/gsc-core/output-directory/database/account"), options);
            //db = factory.open(new File("/home/kay/Desktop/gsc_core_main/output-directory/database/account"), options);

            logger.info("---------------------------------------------");
            System.out.println();
            DBIterator iterator = db.iterator();
            iterator.seekToFirst();
            int count = 0;
            while (iterator.hasNext()){
                count++;
                String key = ByteArray.toHexString(iterator.peekNext().getKey());
                String value = ByteArray.toHexString(iterator.peekNext().getValue());
                System.out.println("key:" + key+ ", value:" + value);
                iterator.next();
            }
            iterator.close();
            System.out.println("Num: " + count);
            System.out.println();
            logger.info("---------------------------------------------");

            db.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
