package org.gsc.db;

import org.springframework.beans.factory.annotation.Autowired;

public class Manager {

  @Autowired
  private AccountStore accountStore;
  @Autowired
  private TransactionStore transactionStore;
  @Autowired
  private BlockStore blockStore;

  @Autowired
  private ProducerStore prodStore;

//  @Autowired
//  private AssetIssueStore assetIssueStore;
//  @Autowired
//  private DynamicPropertiesStore dynamicPropertiesStore;
//  @Autowired
//  private BlockIndexStore blockIndexStore;
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

}
