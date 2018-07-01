package org.gsc.db;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.wrapper.BytesWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProducerScheduleStore extends ChainStore<BytesWrapper> {

  private static final byte[] ACTIVE_WITNESSES = "active_witnesses".getBytes();
  private static final byte[] CURRENT_SHUFFLED_WITNESSES = "current_shuffled_witnesses".getBytes();

  private static final int ADDRESS_BYTE_ARRAY_LENGTH = 21;

  @Autowired
  private ProducerScheduleStore(@Value("witness_schedule") String dbName) {
    super(dbName);
  }

  @Override
  public BytesWrapper get(byte[] key) {
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  private static ProducerScheduleStore instance;

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */

  public static ProducerScheduleStore create(String dbName) {
    if (instance == null) {
      synchronized (ProducerScheduleStore.class) {
        if (instance == null) {
          instance = new ProducerScheduleStore(dbName);
        }
      }
    }
    return instance;
  }


  private void saveData(byte[] species, List<ByteString> producersAddressList) {
    byte[] ba = new byte[producersAddressList.size() * ADDRESS_BYTE_ARRAY_LENGTH];
    int i = 0;
    for (ByteString address : producersAddressList) {
      System.arraycopy(address.toByteArray(), 0,
          ba, i * ADDRESS_BYTE_ARRAY_LENGTH, ADDRESS_BYTE_ARRAY_LENGTH);
//      logger.debug("saveCurrentShuffledWitnesses--ba:" + ByteArray.toHexString(ba));
      i++;
    }
    ;
    this.put(species, new BytesWrapper(ba));
  }

  private List<ByteString> getData(byte[] species) {
    List<ByteString> producersAddressList = new ArrayList<>();
    return Optional.ofNullable(this.dbSource.getData(species))
        .map(ba -> {
          int len = ba.length / ADDRESS_BYTE_ARRAY_LENGTH;
          for (int i = 0; i < len; ++i) {
            byte[] b = new byte[ADDRESS_BYTE_ARRAY_LENGTH];
            System.arraycopy(ba, i * ADDRESS_BYTE_ARRAY_LENGTH, b, 0, ADDRESS_BYTE_ARRAY_LENGTH);
//            logger.debug("address number" + i + ":" + ByteArray.toHexString(b));
            producersAddressList.add(ByteString.copyFrom(b));
          }
          logger.debug("getWitnesses:" + ByteArray.toStr(species) + producersAddressList);
          return producersAddressList;
        }).orElseThrow(
            () -> new IllegalArgumentException(
                "not found " + ByteArray.toStr(species) + "Witnesses"));
  }

  public void saveActiveProducers(List<ByteString> producersAddressList) {
//    producersAddressList.forEach(address -> {
//      logger.info("saveActiveWitnesses:" + ByteArray.toHexString(address.toByteArray()));
//    });
    saveData(ACTIVE_WITNESSES, producersAddressList);
  }

  public List<ByteString> getActiveProducers() {
//    getData(ACTIVE_WITNESSES).forEach(address -> {
//      logger.debug("getActiveWitnesses:" + ByteArray.toHexString(address.toByteArray()));
//    });
    return getData(ACTIVE_WITNESSES);
  }

//  ByteArray.toHexString(scheduledWitness.toByteArray())

  public void saveCurrentShuffledProducers(List<ByteString> producersAddressList) {
//    producersAddressList.forEach(address -> {
//      logger.info("saveCurrentShuffledWitnesses:" + ByteArray.toHexString(address.toByteArray()));
//    });
    saveData(CURRENT_SHUFFLED_WITNESSES, producersAddressList);
  }

  public List<ByteString> getCurrentShuffledProducers() {
//    getData(CURRENT_SHUFFLED_WITNESSES).forEach(address -> {
//      logger.debug("getCurrentShuffledWitnesses:" + ByteArray.toHexString(address.toByteArray()));
//    });
    return getData(CURRENT_SHUFFLED_WITNESSES);
  }
}
