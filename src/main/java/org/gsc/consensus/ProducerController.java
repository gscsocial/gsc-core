package org.gsc.consensus;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.ProducerWrapper;
import org.gsc.db.AccountStore;
import org.gsc.db.GlobalPropertiesStore;
import org.gsc.db.Manager;
import org.gsc.db.ProducerScheduleStore;
import org.gsc.db.ProducerStore;
import org.gsc.db.VotesStore;
import org.joda.time.DateTime;
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

  @Autowired
  private GlobalPropertiesStore globalStore;

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

  /**
   * get slot at time.
   */
  public long getSlotAtTime(long when) {
    long firstSlotTime = getSlotTime(1);
    if (when < firstSlotTime) {
      return 0;
    }
    logger
        .debug("nextFirstSlotTime:[{}],when[{}]", new DateTime(firstSlotTime), new DateTime(when));
    return (when - firstSlotTime) / ChainConstant.BLOCK_PRODUCED_INTERVAL + 1;
  }

  public boolean lastHeadBlockIsMaintenance() {
    return manager.lastHeadBlockIsMaintenance();
  }

  /**
   * get absolute Slot At Time
   */
  public long getAbSlotAtTime(long when) {
    return (when - manager.getGenesisBlock().getTimeStamp()) / ChainConstant.BLOCK_PRODUCED_INTERVAL;
  }

  /**
   * get slot time.
   */
  public long getSlotTime(long slotNum) {
    if (slotNum == 0) {
      return System.currentTimeMillis();
    }
    long interval = ChainConstant.BLOCK_PRODUCED_INTERVAL;

    if (manager.getGlobalPropertiesStore().getLatestBlockHeaderNumber() == 0) {
      return manager.getGenesisBlock().getTimeStamp() + slotNum * interval;
    }

    if (lastHeadBlockIsMaintenance()) {
      slotNum += globalStore.getMaintenanceSkipSlots();
    }

    long headSlotTime = manager.getGlobalPropertiesStore().getLatestBlockHeaderTimestamp();
    headSlotTime = headSlotTime
        - ((headSlotTime - manager.getGenesisBlock().getTimeStamp()) % interval);

    return headSlotTime + interval * slotNum;
  }

  /**
   * validate witness schedule.
   */
  public boolean validateProducerSchedule(BlockWrapper block) {

    ByteString witnessAddress = block.getInstance().getBlockHeader().getRawData()
        .getProducerAddress();
    //to deal with other condition later
    if (manager.getGlobalPropertiesStore().getLatestBlockHeaderNumber() != 0 && manager
        .getGlobalPropertiesStore().getLatestBlockHeaderHash()
        .equals(block.getParentHash())) {
      long slot = getSlotAtTime(block.getTimeStamp());
      final ByteString scheduledWitness = getScheduledProducer(slot);
      if (!scheduledWitness.equals(witnessAddress)) {
        logger.warn(
            "Witness is out of order, scheduledWitness[{}],blockWitnessAddress[{}],blockTimeStamp[{}],slot[{}]",
            ByteArray.toHexString(scheduledWitness.toByteArray()),
            ByteArray.toHexString(witnessAddress.toByteArray()), new DateTime(block.getTimeStamp()),
            slot);
        return false;
      }
      logger.debug("Validate witnessSchedule successfully,scheduledWitness:{}",
          ByteArray.toHexString(witnessAddress.toByteArray()));
    }

    return true;
  }

  public boolean activeWitnessesContain(final Set<ByteString> localWitnesses) {
    List<ByteString> activeWitnesses = prodScheduleStore.getActiveProducers();
    for (ByteString witnessAddress : localWitnesses) {
      if (activeWitnesses.contains(witnessAddress)) {
        return true;
      }
    }
    return false;
  }

  /**
   * get ScheduledWitness by slot.
   */
  public ByteString getScheduledProducer(final long slot) {

    final long currentSlot = getHeadSlot() + slot;

    if (currentSlot < 0) {
      throw new RuntimeException("currentSlot should be positive.");
    }

    int numberActiveWitness = this.prodScheduleStore.getActiveProducers().size();
    int singleRepeat = this.manager.getGlobalPropertiesStore().getSingleRepeat();
    if (numberActiveWitness <= 0) {
      throw new RuntimeException("Active Witnesses is null.");
    }
    int witnessIndex = (int) currentSlot % (numberActiveWitness * singleRepeat);
    witnessIndex /= singleRepeat;
    logger.debug("currentSlot:" + currentSlot
        + ", witnessIndex" + witnessIndex
        + ", currentActiveWitnesses size:" + numberActiveWitness);

    final ByteString scheduledWitness = this.prodScheduleStore.getActiveProducers().get(witnessIndex);
    logger.info("scheduledWitness:" + ByteArray.toHexString(scheduledWitness.toByteArray())
        + ", currentSlot:" + currentSlot);

    return scheduledWitness;
  }

  public long getHeadSlot() {
    return (manager.getGlobalPropertiesStore().getLatestBlockHeaderTimestamp() - manager.getGenesisBlock()
        .getTimeStamp())
        / ChainConstant.BLOCK_PRODUCED_INTERVAL;
  }

  private Map<ByteString, Long> countVote(VotesStore votesStore) {
    final Map<ByteString, Long> countWitness = Maps.newHashMap();
    final List<VotesWrapper> votesList = votesStore.getAllVotes();
    AccountStore accountStore = this.manager.getAccountStore();
    logger.info("there is {} new votes in this epoch", votesList.size());
    votesList.forEach(votes -> {
//      logger.info("there is account ,account address is {}",
//          account.createReadableString());

      Optional<Long> sum = votes.getNewVotes().stream().map(vote -> vote.getVoteCount())
          .reduce((a, b) -> a + b);
      if (sum.isPresent()) {
        AccountWrapper account = accountStore.get(votes.createDbKey());
        if (sum.get() <= account.getPower()) {
          // TODO add vote reward
          // long reward = Math.round(sum.get() * this.manager.getDynamicPropertiesStore()
          //    .getVoteRewardRate());
          //account.setBalance(account.getBalance() + reward);
          //accountStore.put(account.createDbKey(), account);
          votes.getOldVotes().forEach(vote -> {
            //TODO validate witness //active_witness
            ByteString voteAddress = vote.getVoteAddress();
            long voteCount = vote.getVoteCount();
            if (countWitness.containsKey(voteAddress)) {
              countWitness.put(voteAddress, countWitness.get(voteAddress) - voteCount);
            } else {
              countWitness.put(voteAddress, -voteCount);
            }
          });
          votes.getNewVotes().forEach(vote -> {
            //TODO validate witness //active_witness
            ByteString voteAddress = vote.getVoteAddress();
            long voteCount = vote.getVoteCount();
            if (countWitness.containsKey(voteAddress)) {
              countWitness.put(voteAddress, countWitness.get(voteAddress) + voteCount);
            } else {
              countWitness.put(voteAddress, voteCount);
            }
          });
        } else {
          logger.info(
              "account" + ByteArray.fromHexString(account.getAddress(.toByteArray())
                  + ",Power[" + account.getPower()
                  + "] < voteSum["
                  + sum.get() + "]");
        }
      }
    });
    votesStore.reset();
    return countWitness;
  }

}
