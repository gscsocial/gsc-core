package org.gsc.core.witness;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.WitnessWrapper;
import org.joda.time.DateTime;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.StringUtil;
import org.gsc.common.utils.Time;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.core.wrapper.VotesWrapper;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.args.Args;
import org.gsc.db.AccountStore;
import org.gsc.db.Manager;
import org.gsc.db.VotesStore;
import org.gsc.db.WitnessStore;
import org.gsc.core.exception.HeaderNotFound;

@Slf4j
public class WitnessController {

  @Setter
  @Getter
  private Manager manager;

  @Setter
  @Getter
  private boolean isGeneratingBlock;

  public static WitnessController createInstance(Manager manager) {
    WitnessController instance = new WitnessController();
    instance.setManager(manager);
    return instance;
  }


  public void initWits() {
    // getWitnesses().clear();
    List<ByteString> witnessAddresses = new ArrayList<>();
    manager.getWitnessStore().getAllWitnesses().forEach(witnessCapsule -> {
      if (witnessCapsule.getIsJobs()) {
        witnessAddresses.add(witnessCapsule.getAddress());
      }
    });
    sortWitness(witnessAddresses);
    setActiveWitnesses(witnessAddresses);
    witnessAddresses.forEach(address -> {
      logger.info("initWits shuffled addresses:" + ByteArray.toHexString(address.toByteArray()));
    });
    setCurrentShuffledWitnesses(witnessAddresses);
  }

  public WitnessWrapper getWitnesseByAddress(ByteString address) {
    return this.manager.getWitnessStore().get(address.toByteArray());
  }

  public List<ByteString> getActiveWitnesses() {
    return this.manager.getWitnessScheduleStore().getActiveWitnesses();
  }

  public void setActiveWitnesses(List<ByteString> addresses) {
    this.manager.getWitnessScheduleStore().saveActiveWitnesses(addresses);
  }

  public void addWitness(ByteString address) {
    List<ByteString> l = getActiveWitnesses();
    l.add(address);
    setActiveWitnesses(l);
  }

  public List<ByteString> getCurrentShuffledWitnesses() {
    return this.manager.getWitnessScheduleStore().getCurrentShuffledWitnesses();
  }

  public void setCurrentShuffledWitnesses(List<ByteString> addresses) {
    this.manager.getWitnessScheduleStore().saveCurrentShuffledWitnesses(addresses);
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

  public BlockWrapper getGenesisBlock() {
    return manager.getGenesisBlock();
  }

  public BlockWrapper getHead() throws HeaderNotFound {
    return manager.getHead();
  }

  public boolean lastHeadBlockIsMaintenance() {
    return manager.lastHeadBlockIsMaintenance();
  }

  /**
   * get absolute Slot At Time
   */
  public long getAbSlotAtTime(long when) {
    return (when - getGenesisBlock().getTimeStamp()) / ChainConstant.BLOCK_PRODUCED_INTERVAL;
  }

  /**
   * get slot time.
   */
  public long getSlotTime(long slotNum) {
    if (slotNum == 0) {
      return Time.getCurrentMillis();
    }
    long interval = ChainConstant.BLOCK_PRODUCED_INTERVAL; // BLOCK_PRODUCED_INTERVAL = 3000;

    if (manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 0) {
      return getGenesisBlock().getTimeStamp() + slotNum * interval; // Genesis first block
    }

    if (lastHeadBlockIsMaintenance()) {
      slotNum += manager.getSkipSlotInMaintenance();  // MAINTENANCE_SKIP_SLOTS = 2;
    }

    long headSlotTime = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp(); //DB保存的时间戳
    headSlotTime = headSlotTime
        - ((headSlotTime - getGenesisBlock().getTimeStamp()) % interval);

    return headSlotTime + interval * slotNum;
  }

  /**
   * validate witness schedule.
   */
  public boolean validateWitnessSchedule(BlockWrapper block) {

    ByteString witnessAddress = block.getInstance().getBlockHeader().getRawData()
        .getWitnessAddress();
    //to deal with other condition later
    if (manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 0) {
      return true;
    }
    long blockAbSlot = getAbSlotAtTime(block.getTimeStamp());
    long headBlockAbSlot = getAbSlotAtTime(
        manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp());
    if (blockAbSlot <= headBlockAbSlot) {
      logger.warn("blockAbSlot is equals with headBlockAbSlot[" + blockAbSlot + "]");
      return false;
    }

    long slot = getSlotAtTime(block.getTimeStamp());
    final ByteString scheduledWitness = getScheduledWitness(slot);
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
    return true;
  }

  public boolean activeWitnessesContain(final Set<ByteString> localWitnesses) {
    List<ByteString> activeWitnesses = this.getActiveWitnesses();
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
  public ByteString getScheduledWitness(final long slot) {

    logger.info("slot: " + slot);
    final long currentSlot = getHeadSlot() + slot;

    /**
     * slot: 1
     * LatestBlockHeaderTimestamp: 1540199073000
     * TimeStamp: 0
     * INTERVAL: 3000
     *
     * currentSlot:513399692, witnessIndex:0, currentActiveWitnesses size:1
     *
     * getHeadSlot = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - getGenesisBlock()
     *         .getTimeStamp())
     *         / ChainConstant.BLOCK_PRODUCED_INTERVAL;
     */

    if (currentSlot < 0) {
      throw new RuntimeException("currentSlot should be positive.");
    }

    int numberActiveWitness = this.getActiveWitnesses().size();
    int singleRepeat = ChainConstant.SINGLE_REPEAT; //SINGLE_REPEAT = 1;
    if (numberActiveWitness <= 0) {
      throw new RuntimeException("Active Witnesses is null.");
    }
    int witnessIndex = (int) currentSlot % (numberActiveWitness * singleRepeat);
    witnessIndex /= singleRepeat;
    logger.debug("currentSlot:" + currentSlot
        + ", witnessIndex:" + witnessIndex
        + ", currentActiveWitnesses size:" + numberActiveWitness);

    logger.info("currentSlot:" + currentSlot
            + ", witnessIndex" + witnessIndex
            + ", currentActiveWitnesses size:" + numberActiveWitness);

    final ByteString scheduledWitness = this.getActiveWitnesses().get(witnessIndex);
    // 1. scheduledWitness:26928c9af0651632157ef27a2cf17ca72c575a4d21, currentSlot:513396675
    logger.info("scheduledWitness:" + ByteArray.toHexString(scheduledWitness.toByteArray())
        + ", currentSlot:" + currentSlot);

    return scheduledWitness;
  }

  public long getHeadSlot() {
    logger.info("LatestBlockHeaderTimestamp: " + manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp());
    logger.info("TimeStamp: " + getGenesisBlock().getTimeStamp());
    logger.info("INTERVAL: " + ChainConstant.BLOCK_PRODUCED_INTERVAL);
    return (manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - getGenesisBlock()
        .getTimeStamp())
        / ChainConstant.BLOCK_PRODUCED_INTERVAL;
  }

  private Map<ByteString, Long> countVote(VotesStore votesStore) {
    final Map<ByteString, Long> countWitness = Maps.newHashMap();
    Iterator<Map.Entry<byte[], VotesWrapper>> dbIterator = votesStore.iterator();

    long sizeCount = 0;
    while (dbIterator.hasNext()) {
      Entry<byte[], VotesWrapper> next = dbIterator.next();
      VotesWrapper votes = next.getValue();

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
        // todo validate witness //active_witness
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
   * update witness.
   */
  public void updateWitness() {
    WitnessStore witnessStore = manager.getWitnessStore();
    VotesStore votesStore = manager.getVotesStore();
    AccountStore accountStore = manager.getAccountStore();

    tryRemoveThePowerOfTheGr();

    Map<ByteString, Long> countWitness = countVote(votesStore);

    //Only possible during the initialization phase
    if (countWitness.isEmpty()) {
      logger.info("No vote, no change to witness.");
    } else {
      List<ByteString> currentWits = getActiveWitnesses();

      List<ByteString> newWitnessAddressList = new ArrayList<>();
      witnessStore.getAllWitnesses().forEach(witnessWrapper -> {

        logger.info("getAddress: ",witnessWrapper.getAddress());
        logger.info("getLatestBlockNum: ",witnessWrapper.getLatestBlockNum());
        logger.info("getVoteCount: ",witnessWrapper.getVoteCount());
        logger.info("getTotalProduced: ",witnessWrapper.getTotalProduced());
        logger.info("getUrl: ",witnessWrapper.getUrl());
        logger.info("getTotalMissed: ",witnessWrapper.getTotalMissed());
        logger.info("getIsJobs: ",witnessWrapper.getIsJobs());
        logger.info("getLatestSlotNum: ",witnessWrapper.getLatestSlotNum());
        logger.info("getData: ",witnessWrapper.getData());
        newWitnessAddressList.add(witnessWrapper.getAddress());
      });

      countWitness.forEach((address, voteCount) -> {
        final WitnessWrapper witnessCapsule = witnessStore
            .get(StringUtil.createDbKey(address));
        if (null == witnessCapsule) {
          logger.warn("witnessCapsule is null.address is {}",
              StringUtil.createReadableString(address));
          return;
        }

        AccountWrapper witnessAccountWrapper = accountStore
            .get(StringUtil.createDbKey(address));
        if (witnessAccountWrapper == null) {
          logger.warn(
              "witnessAccount[" + StringUtil.createReadableString(address) + "] not exists");
        } else {
          witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() + voteCount);
          witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
          logger.info("address is {}  ,countVote is {}", witnessCapsule.createReadableString(),
              witnessCapsule.getVoteCount());
        }
      });

      sortWitness(newWitnessAddressList);
      if (newWitnessAddressList.size() > ChainConstant.MAX_ACTIVE_WITNESS_NUM) {
        setActiveWitnesses(newWitnessAddressList.subList(0, ChainConstant.MAX_ACTIVE_WITNESS_NUM));
      } else {
        setActiveWitnesses(newWitnessAddressList);
      }

      if (newWitnessAddressList.size() > ChainConstant.WITNESS_STANDBY_LENGTH) {
        payStandbyWitness(newWitnessAddressList.subList(0, ChainConstant.WITNESS_STANDBY_LENGTH));
      } else {
        payStandbyWitness(newWitnessAddressList);
      }

      List<ByteString> newWits = getActiveWitnesses();
      if (witnessSetChanged(currentWits, newWits)) {
        currentWits.forEach(address -> {
          WitnessWrapper witnessCapsule = getWitnesseByAddress(address);
          witnessCapsule.setIsJobs(false);
          witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
        });

        newWits.forEach(address -> {
          WitnessWrapper witnessCapsule = getWitnesseByAddress(address);
          witnessCapsule.setIsJobs(true);
          witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
        });
      }

      logger.info(
          "updateWitness,before:{} ", StringUtil.getAddressStringList(currentWits)
              + ",\nafter:{} " + StringUtil.getAddressStringList(newWits));
    }
  }

  public void tryRemoveThePowerOfTheGr(){
    if(manager.getDynamicPropertiesStore().getRemoveThePowerOfTheGr() == 1){

      WitnessStore witnessStore = manager.getWitnessStore();

      Args.getInstance().getGenesisBlock().getWitnesses().forEach(witnessInGenesisBlock -> {
        WitnessWrapper witnessCapsule = witnessStore.get(witnessInGenesisBlock.getAddress());
        witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() - witnessInGenesisBlock.getVoteCount());

        witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
      });

      manager.getDynamicPropertiesStore().saveRemoveThePowerOfTheGr(-1);
    }
  }

  private static boolean witnessSetChanged(List<ByteString> list1, List<ByteString> list2) {
    return !CollectionUtils.isEqualCollection(list1, list2);
  }


  public int calculateParticipationRate() {
    return manager.getDynamicPropertiesStore().calculateFilledSlotsCount();
  }

  public void dumpParticipationLog() {
    StringBuilder builder = new StringBuilder();
    int[] blockFilledSlots = manager.getDynamicPropertiesStore().getBlockFilledSlots();
    builder.append("dump participation log \n ").append("blockFilledSlots:")
        .append(Arrays.toString(blockFilledSlots)).append(",");
    long headSlot = getHeadSlot();
    builder.append("\n").append(" headSlot:").append(headSlot).append(",");

    List<ByteString> activeWitnesses = getActiveWitnesses();
    activeWitnesses.forEach(a -> {
      WitnessWrapper witnessCapsule = manager.getWitnessStore().get(a.toByteArray());
      builder.append("\n").append(" witness:").append(witnessCapsule.createReadableString())
          .append(",").
          append("latestBlockNum:").append(witnessCapsule.getLatestBlockNum()).append(",").
          append("LatestSlotNum:").append(witnessCapsule.getLatestSlotNum()).append(".");
    });
    logger.debug(builder.toString());
  }


  private void sortWitness(List<ByteString> list) {
    list.sort(Comparator.comparingLong((ByteString b) -> getWitnesseByAddress(b).getVoteCount())
        .reversed()
        .thenComparing(Comparator.comparingInt(ByteString::hashCode).reversed()));
  }

  private void payStandbyWitness(List<ByteString> list) {
    long voteSum = 0;
    long totalPay = manager.getDynamicPropertiesStore().getWitnessStandbyAllowance();
    for (ByteString b : list) {
      voteSum += getWitnesseByAddress(b).getVoteCount();
    }
    if (voteSum > 0) {
      for (ByteString b : list) {
        long pay = (long) (getWitnesseByAddress(b).getVoteCount() * ((double) totalPay / voteSum));
        AccountWrapper accountWrapper = manager.getAccountStore().get(b.toByteArray());
        accountWrapper.setAllowance(accountWrapper.getAllowance() + pay);
        manager.getAccountStore().put(accountWrapper.createDbKey(), accountWrapper);
      }
    }

  }

}
