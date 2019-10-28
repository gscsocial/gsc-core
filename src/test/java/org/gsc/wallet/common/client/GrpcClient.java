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

package org.gsc.wallet.common.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.AccountNetMessage;
import org.gsc.api.GrpcAPI.AccountPaginated;
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.GrpcAPI.BlockLimit;
import org.gsc.api.GrpcAPI.BlockList;
import org.gsc.api.GrpcAPI.BytesMessage;
import org.gsc.api.GrpcAPI.EmptyMessage;
import org.gsc.api.GrpcAPI.NodeList;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.GrpcAPI.PaginatedMessage;
import org.gsc.api.GrpcAPI.TransactionList;
import org.gsc.api.GrpcAPI.WitnessList;
import org.gsc.api.WalletExtensionGrpc;
import org.gsc.api.WalletGrpc;
import org.gsc.api.WalletConfirmedGrpc;
import org.gsc.utils.ByteArray;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.FreezeBalanceContract;
import org.gsc.protos.Contract.UnfreezeBalanceContract;
import org.gsc.protos.Contract.WithdrawBalanceContract;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;


public class GrpcClient {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelConfirmed = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletConfirmedGrpc.WalletConfirmedBlockingStub blockingStubConfirmed = null;
  private WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;

  /**
   * constructor.
   */

  public GrpcClient(String fullnode, String confirmednode) {
    if (!(fullnode.isEmpty())) {
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    }
    if (!(confirmednode.isEmpty())) {
      channelConfirmed = ManagedChannelBuilder.forTarget(confirmednode)
          .usePlaintext(true)
          .build();
      blockingStubConfirmed = WalletConfirmedGrpc.newBlockingStub(channelConfirmed);
      blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelConfirmed);
    }
  }

  /**
   * constructor.
   */

  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelConfirmed != null) {
      channelConfirmed.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * constructor.
   */

  public Account queryAccount(byte[] address) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    if (blockingStubConfirmed != null) {
      return blockingStubConfirmed.getAccount(request);
    } else {
      return blockingStubFull.getAccount(request);
    }
  }

  public Transaction createTransaction(Contract.AccountUpdateContract contract) {
    return blockingStubFull.updateAccount(contract);
  }

  public Transaction createTransaction(Contract.TransferContract contract) {
    return blockingStubFull.createTransaction(contract);
  }

  public Transaction createTransaction(FreezeBalanceContract contract) {
    return blockingStubFull.freezeBalance(contract);
  }

  public Transaction createTransaction(WithdrawBalanceContract contract) {
    return blockingStubFull.withdrawBalance(contract);
  }

  public Transaction createTransaction(UnfreezeBalanceContract contract) {
    return blockingStubFull.unfreezeBalance(contract);
  }

  public Transaction createTransferAssetTransaction(Contract.TransferAssetContract contract) {
    return blockingStubFull.transferAsset(contract);
  }

  public Transaction createParticipateAssetIssueTransaction(
      Contract.ParticipateAssetIssueContract contract) {
    return blockingStubFull.participateAssetIssue(contract);
  }

  public Transaction createAccount(Contract.AccountCreateContract contract) {
    return blockingStubFull.createAccount(contract);
  }

  public Transaction createAssetIssue(Contract.AssetIssueContract contract) {
    return blockingStubFull.createAssetIssue(contract);
  }

  public Transaction voteWitnessAccount(Contract.VoteWitnessContract contract) {
    return blockingStubFull.voteWitnessAccount(contract);
  }

  public Transaction createWitness(Contract.WitnessCreateContract contract) {
    return blockingStubFull.createWitness(contract);
  }

  public boolean broadcastTransaction(Transaction signaturedTransaction) {
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(signaturedTransaction);
    return response.getResult();
  }

  public Optional<AssetIssueList> getAssetIssueList() {
    if (blockingStubConfirmed != null) {
      AssetIssueList assetIssueList = blockingStubConfirmed
              .getAssetIssueList(EmptyMessage.newBuilder().build());
      return Optional.ofNullable(assetIssueList);
    } else {
      AssetIssueList assetIssueList = blockingStubFull
              .getAssetIssueList(EmptyMessage.newBuilder().build());
      return Optional.ofNullable(assetIssueList);
    }
  }

  /**
   * constructor.
   */

  public AccountNetMessage getAccountNet(byte[] address) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccountNet(request);
  }

  /**
   * constructor.
   */

  public Block getBlock(long blockNum) {
    if (blockNum < 0) {
      if (blockingStubConfirmed != null) {
        return blockingStubConfirmed.getNowBlock(EmptyMessage.newBuilder().build());
      } else {
        return blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
      }
    }
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    if (blockingStubConfirmed != null) {
      return blockingStubConfirmed.getBlockByNum(builder.build());
    } else {
      return blockingStubFull.getBlockByNum(builder.build());
    }
  }

  /*    public Optional<AccountList> listAccounts() {
        if(blockingStubConfirmed != null) {
            AccountList accountList = blockingStubConfirmed.listAccounts(
            EmptyMessage.newBuilder().build());
            return Optional.ofNullable(accountList);
        }else{
            AccountList accountList = blockingStubFull.listAccounts(
            EmptyMessage.newBuilder().build());
            return Optional.ofNullable(accountList);
        }
    }*/

  /**
   * constructor.
   */
  public Optional<WitnessList> listWitnesses() {
    if (blockingStubConfirmed != null) {
      WitnessList witnessList = blockingStubConfirmed.listWitnesses(
          EmptyMessage.newBuilder().build());
      return Optional.ofNullable(witnessList);
    } else {
      WitnessList witnessList = blockingStubFull.listWitnesses(
          EmptyMessage.newBuilder().build());
      return Optional.ofNullable(witnessList);
    }
  }

  /**
   * constructor.
   */

  public Optional<AssetIssueList> getAssetIssueList(long offset, long limit) {
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    if (blockingStubConfirmed != null) {
      AssetIssueList assetIssueList = blockingStubConfirmed
          .getPaginatedAssetIssueList(pageMessageBuilder.build());
      return Optional.ofNullable(assetIssueList);
    } else {
      AssetIssueList assetIssueList = blockingStubFull
          .getPaginatedAssetIssueList(pageMessageBuilder.build());
      return Optional.ofNullable(assetIssueList);
    }
  }

  public Optional<NodeList> listNodes() {
    NodeList nodeList = blockingStubFull
        .listNodes(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(nodeList);
  }


  public Optional<TransactionList> getTransactionsFromThis(byte[] address) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBs).build();
    AccountPaginated.Builder builder = AccountPaginated.newBuilder().setAccount(account);
    builder.setLimit(1000);
    builder.setOffset(0);
    TransactionList transactionList = blockingStubExtension
        .getTransactionsFromThis(builder.build());
    return Optional.ofNullable(transactionList);
  }

  public Optional<TransactionList> getTransactionsToThis(byte[] address) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBs).build();
    AccountPaginated.Builder builder = AccountPaginated.newBuilder().setAccount(account);
    builder.setLimit(1000);
    builder.setOffset(0);
    TransactionList transactionList = blockingStubExtension.getTransactionsToThis(builder.build());
    return Optional.ofNullable(transactionList);
  }

  public Optional<Block> getBlockById(String blockId) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(blockId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Block block = blockingStubFull.getBlockById(request);
    return Optional.ofNullable(block);
  }

  public Optional<BlockList> getBlockByLimitNext(long start, long end) {
    BlockLimit.Builder builder = BlockLimit.newBuilder();
    builder.setStartNum(start);
    builder.setEndNum(end);
    BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
    return Optional.ofNullable(blockList);
  }

  public Optional<BlockList> getBlockByLatestNum(long num) {
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(num).build();
    BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
    return Optional.ofNullable(blockList);
  }
}
