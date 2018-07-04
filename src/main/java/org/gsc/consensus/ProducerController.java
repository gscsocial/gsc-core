package org.gsc.consensus;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.StringUtil;
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
import org.gsc.db.iterator.DBIterator;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class ProducerController {

  @Setter
  private Manager manager;

  @Autowired
  private ProducerScheduleStore prodScheduleStore;

  @Autowired
  private ProducerStore prodStore;

  @Autowired
  private GlobalPropertiesStore globalStore;

  @Autowired
  private VotesStore votesStore;

  @Autowired
  private AccountStore accountStore;

  @Setter
  @Getter

  private boolean isGeneratingBlock;

  public static ProducerController createInstance(Manager manager) {
    ProducerController instance = new ProducerController();
    instance.setManager(manager);
    return instance;
  }

  public void initProds() {
    List<ByteString> prodAddresses = new ArrayList<>();
    manager.getProdStore().getAllProducers().forEach(witnessWrapper -> {
      if (witnessWrapper.getIsJobs()) {
        prodAddresses.add(witnessWrapper.getAddress());
      }
    });
    sortProds(prodAddresses);
    this.manager.getProdScheduleStore().saveActiveProducers(prodAddresses);
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
    return prodStore.get(address.toByteArray());
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
   DBIterator dbIterator = votesStore.getIterator();

    long sizeCount = 0;
    while (dbIterator.hasNext()) {
      Entry<byte[], byte[]> next = dbIterator.next();
      VotesWrapper votes = new VotesWrapper(next.getValue());

//      logger.info("there is account ,account address is {}",
//          account.createReadableString());

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


      sizeCount++;
      votesStore.delete(next.getKey());
    }
    logger.info("there is {} new votes in this epoch", sizeCount);

    return countWitness;
  }

  /**
   * update prod.
   */
  public void updateProducer() {
    Map<ByteString, Long> countWitness = countVote(votesStore);

    //Only possible during the initialization phase
    if (countWitness.size() == 0) {
      logger.info("No vote, no change to witness.");
    } else {
      List<ByteString> currentWits = prodScheduleStore.getActiveProducers();
      List<ByteString> newWitnessAddressList = new ArrayList<>();
      prodStore.getAllProducers().forEach(witnessWrapper -> {
        newWitnessAddressList.add(witnessWrapper.getAddress());
      });

      countWitness.forEach((address, voteCount) -> {
        final ProducerWrapper witnessWrapper = prodStore
            .get(StringUtil.createDbKey(address));
        if (null == witnessWrapper) {
          logger.warn("witnessWrapper is null.address is {}",
              StringUtil.createReadableString(address));
          return;
        }

        AccountWrapper account = accountStore
            .get(StringUtil.createDbKey(address));
        if (account == null) {
          logger.warn(
              "witnessAccount[" + StringUtil.createReadableString(address) + "] not exists");
        } else {
          witnessWrapper.setVoteCount(witnessWrapper.getVoteCount() + voteCount);
          witnessWrapper.setIsJobs(false);
          prodStore.put(witnessWrapper.createDbKey(), witnessWrapper);
          logger.info("address is {}  ,countVote is {}", witnessWrapper.createReadableString(),
              witnessWrapper.getVoteCount());

        }
      });

      sortProds(newWitnessAddressList);
      if (newWitnessAddressList.size() > ChainConstant.MAX_ACTIVE_WITNESS_NUM) {
        prodScheduleStore.saveActiveProducers(newWitnessAddressList.subList(0, ChainConstant.MAX_ACTIVE_WITNESS_NUM));
      } else {
        prodScheduleStore.saveActiveProducers(newWitnessAddressList);
      }

      if (newWitnessAddressList.size() > ChainConstant.WITNESS_STANDBY_LENGTH) {
        payStandbyWitness(newWitnessAddressList.subList(0, ChainConstant.WITNESS_STANDBY_LENGTH));
      } else {
        payStandbyWitness(newWitnessAddressList);
      }

      prodScheduleStore.getActiveProducers().forEach(address -> {
        ProducerWrapper prod = getProdByAddress(address);
        prod.setIsJobs(true);
        prodStore.put(prod.createDbKey(), prod);
      });

      logger.info(
          "updateWitness,before:{} ", StringUtil.getAddressStringList(currentWits)
              + ",\nafter:{} " + StringUtil.getAddressStringList( prodScheduleStore.getActiveProducers()));
    }

  }

  public int calculateParticipationRate() {
    return globalStore.calculateFilledSlotsCount();
  }

  public List<ByteString> getActiveProducers() {
    return this.manager.getProdScheduleStore().getActiveProducers();
  }

  public void dumpParticipationLog() {
    StringBuilder builder = new StringBuilder();
    int[] blockFilledSlots = manager.getGlobalPropertiesStore().getBlockFilledSlots();
    builder.append("dump participation log \n ").append("blockFilledSlots:")
        .append(Arrays.toString(blockFilledSlots)).append(",");
    long headSlot = getHeadSlot();
    builder.append("\n").append(" headSlot:").append(headSlot).append(",");

    List<ByteString> activeWitnesses = getActiveProducers();
    activeWitnesses.forEach(a -> {
      ProducerWrapper witnessWrapper = manager.getProdStore().get(a.toByteArray());
      builder.append("\n").append(" witness:").append(witnessWrapper.createReadableString())
          .append(",").
          append("latestBlockNum:").append(witnessWrapper.getLatestBlockNum()).append(",").
          append("LatestSlotNum:").append(witnessWrapper.getLatestSlotNum()).append(".");
    });
    logger.debug(builder.toString());
  }

  private void payStandbyWitness(List<ByteString> list) {
    long voteSum = 0;
    long totalPay = ChainConstant.WITNESS_STANDBY_ALLOWANCE;
    for (ByteString b : list) {
      voteSum += getProdByAddress(b).getVoteCount();
    }
    if (voteSum > 0) {
      for (ByteString b : list) {
        long pay = getProdByAddress(b).getVoteCount() * totalPay / voteSum;
        AccountWrapper accountWrapper = manager.getAccountStore().get(b.toByteArray());
        accountWrapper.setAllowance(accountWrapper.getAllowance() + pay);
        manager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
      }
    }
  }
}
