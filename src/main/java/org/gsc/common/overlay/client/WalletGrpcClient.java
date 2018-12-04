package org.gsc.common.overlay.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.*;
import org.gsc.api.WalletGrpc;
import org.gsc.protos.Contract;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Transaction;
import org.spongycastle.util.encoders.Hex;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class WalletGrpcClient{

  private final ManagedChannel channel;
  private final WalletGrpc.WalletBlockingStub walletBlockingStub;

  public WalletGrpcClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext(true)
        .build();
    walletBlockingStub = WalletGrpc.newBlockingStub(channel);
  }

  public WalletGrpcClient(String host) {
    channel = ManagedChannelBuilder.forTarget(host)
        .usePlaintext(true)
        .build();
    walletBlockingStub = WalletGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public Account queryAccount(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    return walletBlockingStub.getAccount(request);
  }

  public Account getAccountById(byte[] accountId){
    ByteString accountIdBS = ByteString.copyFrom(accountId);
    Account request = Account.newBuilder().setAccountId(accountIdBS).build();
    return walletBlockingStub.getAccountById(request);
  }

  public AccountNetMessage getAccountNet(byte[] address){
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    return walletBlockingStub.getAccountNet(request);
  }

  public AccountResourceMessage getAccountResource(byte[] address){
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    return walletBlockingStub.getAccountResource(request);
  }

  public Transaction createTransferTransaction(Contract.TransferContract contract) {
    return walletBlockingStub.createTransaction(contract);
  }

  public Transaction createFreezeBalanceTransaction(Contract.FreezeBalanceContract contract) {
    return walletBlockingStub.freezeBalance(contract);
  }

  public Transaction createTransferAssetTransaction(Contract.TransferAssetContract contract) {
    return walletBlockingStub.transferAsset(contract);
  }

  public Transaction createParticipateAssetIssueTransaction(
      Contract.ParticipateAssetIssueContract contract) {
    return walletBlockingStub.participateAssetIssue(contract);
  }

  public Transaction createWitnessUpdateTransaction(ByteString ownerAddress, ByteString url){
    Contract.WitnessUpdateContract request = Contract.WitnessUpdateContract.newBuilder()
            .setOwnerAddress(ownerAddress).setUpdateUrl(url).build();
    Transaction response = walletBlockingStub.updateWitness(request);
    if (request != null){
      return response;
    }
    return null;
  }

  /**
   *   bytes origin_address = 1;
   *   bytes contract_address = 2;
   *   ABI abi = 3;
   *   bytes bytecode = 4;
   *   int64 call_value = 5;
   *   int64 consume_user_resource_percent = 6;
   *   string name = 7;
   * @return
   */
  public Transaction deployContract(String contractName,
                            String originAddress,
                            String abiStr, String byteCode, long callValue, long consumeUserResourcePercent){
    Protocol.SmartContract.ABI abi = RPCUtils.jsonStr2ABI(abiStr);

    Protocol.SmartContract.Builder smartContract = Protocol.SmartContract.newBuilder();
    smartContract.setOriginAddress(ByteString.copyFrom(Hex.decode(originAddress)));
    // smartContract.setContractAddress();
    smartContract.setName(contractName);
    smartContract.setAbi(abi);
    smartContract.setCallValue(callValue);
    smartContract.setBytecode(ByteString.copyFrom(Hex.decode(byteCode)));
    smartContract.setConsumeUserResourcePercent(consumeUserResourcePercent);
    Contract.CreateSmartContract request = Contract.CreateSmartContract.newBuilder()
            .setNewContract(smartContract).setOwnerAddress(ByteString.copyFrom(Hex.decode(originAddress))).build();
    TransactionExtention response = walletBlockingStub.deployContract(request);
    if (response.getResult().getResult()){
      return response.getTransaction();
    }
    return null;
  }



  public Return easyTransfer(long amount, ByteString toAddress, ByteString passPhrase){
    EasyTransferMessage request = EasyTransferMessage.newBuilder().setAmount(amount)
            .setToAddress(toAddress)
            .setPassPhrase(passPhrase).build();
    EasyTransferResponse response = walletBlockingStub.easyTransfer(request);
    if (response != null){
      return response.getResult();
    }
    return null;
  }

  public Transaction createAssetIssue(AssetIssueContract contract) {
    return walletBlockingStub.createAssetIssue(contract);
  }

  public Transaction voteWitnessAccount(Contract.VoteWitnessContract contract) {
    return walletBlockingStub.voteWitnessAccount(contract);
  }

  public Transaction createWitness(Contract.WitnessCreateContract contract) {
    return walletBlockingStub.createWitness(contract);
  }

  public boolean broadcastTransaction(Transaction signaturedTransaction) {
    Return response = walletBlockingStub.broadcastTransaction(signaturedTransaction);
    return response.getResult();
  }

  public Block getBlock(long blockNum) {
    if (blockNum < 0) {
      return walletBlockingStub.getNowBlock(EmptyMessage.newBuilder().build());
    }
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return walletBlockingStub.getBlockByNum(builder.build());
  }

  public Block getNowBlock(){
    EmptyMessage emptyMessage = EmptyMessage.newBuilder().build();
    Block block = walletBlockingStub.getNowBlock(emptyMessage);
    return block;
  }

  public Block getBlockByNum(int number){
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(number).build();
    return  walletBlockingStub.getBlockByNum(numberMessage);
  }

  public Optional<BlockList> getBlockByLatestNum(int latestNum){
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(latestNum).build();
    BlockList blockList = walletBlockingStub.getBlockByLatestNum(numberMessage);
    if (blockList != null){
      return Optional.of(blockList);
    }
    return Optional.empty();
  }

  public Optional<BlockList> getBlockByLimitNext(int startNum, int endNum){
    BlockLimit request = BlockLimit.newBuilder().setStartNum(startNum).setEndNum(endNum).build();
    BlockList response = walletBlockingStub.getBlockByLimitNext(request);
    if (response != null){
      return Optional.of(response);
    }
    return Optional.empty();
  }

  public NumberMessage getTransactionCountByBlockNum(int blockNum){
    NumberMessage request = NumberMessage.newBuilder().setNum(blockNum).build();
    NumberMessage response = walletBlockingStub.getTransactionCountByBlockNum(request);
    if (response != null){
      return response;
    }
    return null;
  }

  public NumberMessage totalTransaction(){
    EmptyMessage request = EmptyMessage.newBuilder().build();
    NumberMessage response = walletBlockingStub.totalTransaction(request);
    if (response != null){
      return response;
    }
    return null;
  }

  public Optional<NodeList> listNodes() {
    NodeList nodeList = walletBlockingStub
        .listNodes(EmptyMessage.newBuilder().build());
    if (nodeList != null) {
      return Optional.of(nodeList);
    }
    return Optional.empty();
  }

  public Optional<GrpcAPI.VoteStatistics> getWitnessVoteStatistics(){
    GrpcAPI.VoteStatistics voteStatistics = walletBlockingStub.getWitnessVoteStatistics(EmptyMessage.newBuilder().build());

    if (voteStatistics != null){
      return Optional.of(voteStatistics);
    }
    return Optional.empty();
  }

  public Optional<AssetIssueList> getAssetIssueList(){
    EmptyMessage request = EmptyMessage.newBuilder().build();
    AssetIssueList assetIssueList = walletBlockingStub.getAssetIssueList(request);
    if (assetIssueList != null){
      return Optional.of(assetIssueList);
    }
    return Optional.empty();
  }

  public Optional<AssetIssueList> getPaginatedAssetIssueList(int offset, int limit){
    PaginatedMessage paginatedMessage = PaginatedMessage.newBuilder().setOffset(offset).setLimit(limit).build();
    AssetIssueList assetIssueList = walletBlockingStub.getPaginatedAssetIssueList(paginatedMessage);
    if (assetIssueList != null){
      return Optional.of(assetIssueList);
    }
    return Optional.empty();
  }

  public Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    AssetIssueList assetIssueList = walletBlockingStub
        .getAssetIssueByAccount(request);
    if (assetIssueList != null) {
      return Optional.of(assetIssueList);
    }
    return Optional.empty();
  }

  public AssetIssueContract getAssetIssueByName(String assetName) {
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    return walletBlockingStub.getAssetIssueByName(request);
  }

  public Transaction withdrawBalance(Contract.WithdrawBalanceContract withdrawBalanceContract){
    Transaction transaction = walletBlockingStub.withdrawBalance(withdrawBalanceContract);
    if (transaction != null){
      return transaction;
    }
    return null;
  }
}
