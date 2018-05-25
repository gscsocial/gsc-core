package org.gsc.db;

import lombok.Getter;
import org.gsc.common.exception.BalanceInsufficientException;
import org.gsc.core.wrapper.AccountWrapper;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
public class Manager {

  @Autowired
  private AccountStore accountStore;
  @Autowired
  private TransactionStore transactionStore;
  @Autowired
  private BlockStore blockStore;

  @Autowired
  private ProducerStore prodStore;

  @Autowired
  private AssetIssueStore assetIssueStore;
//  @Autowired
//  private DynamicPropertiesStore dynamicPropertiesStore;
  @Autowired
  private BlockIndexStore blockIndexStore;
//  @Autowired
//  private AccountIndexStore accountIndexStore;
  @Autowired
  private ProducerScheduleStore prodScheduleStore;
//  @Autowired
//  private RecentBlockStore recentBlockStore;
//  @Autowired
//  private VotesStore votesStore;
//
//  // for network
//  @Autowired
//  private PeersStore peersStore;


  @Autowired
  private ForkDatabase forkDB;


  /**
   * judge balance.
   */
  public void adjustBalance(byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    AccountWrapper account = getAccountStore().get(accountAddress);
    long balance = account.getBalance();
    if (amount == 0) {
      return;
    }

    if (amount < 0 && balance < -amount) {
      throw new BalanceInsufficientException(accountAddress + " Insufficient");
    }
    account.setBalance(Math.addExact(balance, amount));
    this.getAccountStore().put(account.getAddress().toByteArray(), account);
  }

  public long getHeadBlockTimeStamp() {
    return 0L;
    //TODO
    //return getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
  }

}
