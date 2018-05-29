package org.gsc.consensus;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.wrapper.ProducerWrapper;
import org.gsc.db.Manager;
import org.gsc.db.ProducerScheduleStore;
import org.gsc.db.ProducerStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProducerController {

  @Autowired
  private Manager manager;

  @Autowired
  private ProducerScheduleStore prodScheduleStore;

  @Autowired
  private ProducerStore prodStore;

  @Setter
  @Getter
  private boolean isGeneratingBlock;


  @Autowired
  public void initProds() {
    List<ByteString> prodAddresses = new ArrayList<>();
    manager.getProdStore().getAllProducers().forEach(witnessCapsule -> {
      if (witnessCapsule.getIsJobs()) {
        prodAddresses.add(witnessCapsule.getAddress());
      }
    });
    sortProds(prodAddresses);
    prodScheduleStore.saveActiveProducers(prodAddresses);
    prodAddresses.forEach(address -> {
      logger.info("initProds shuffled addresses:" + ByteArray.toHexString(address.toByteArray()));
    });
  }

  private void sortProds(List<ByteString> list) {
    list.sort(Comparator.comparingLong((ByteString b) -> getProdByAddress(b).getVoteCount())
        .reversed()
        .thenComparing(Comparator.comparingInt(ByteString::hashCode).reversed()));
  }

  public ProducerWrapper getProdByAddress(ByteString address) {
    return this.manager.getProdStore().get(address.toByteArray());
  }

  public void addProd(ByteString address) {
    List<ByteString> l = prodScheduleStore.getActiveProducers();
    l.add(address);
    prodScheduleStore.saveActiveProducers(l);
  }

}
