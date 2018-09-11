package org.gsc.db;

import lombok.extern.slf4j.Slf4j;
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

       // deposit.getDbManager();
        //BlockWrapper.BlockId blockId = dbManager.getHeadBlockId();

        Options options = new Options();
        options.createIfMissing(true);
        DB db = null;
        try {
            //db = factory.open(new File("/home/kay/workspace/mico/gsc/gsc-core/output-directory/database/contract"), options);
            db = factory.open(new File("/home/kay/workspace/mico/gsc/gsc-core/output_InheritanceTest/database/contract"), options);

            logger.info("---------------------------------------------");
            DBIterator iterator = db.iterator();
            iterator.seekToFirst();
            while (iterator.hasNext()){
                String key = asString(iterator.peekNext().getKey());
                String value = asString(iterator.peekNext().getValue());
                System.out.println("key:" + key.getBytes("utf-8").toString() + ", value:" + value);
                iterator.next();
            }
            iterator.close();
            logger.info("---------------------------------------------");

            db.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
