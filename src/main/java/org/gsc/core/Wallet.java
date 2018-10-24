/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.gsc.core;

import com.google.common.primitives.Longs;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.gsc.core.exception.*;
import org.gsc.core.operator.Operator;
import org.gsc.core.operator.OperatorFactory;
import org.gsc.core.wrapper.*;
import org.gsc.db.*;
import org.gsc.protos.Contract;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.gsc.api.GrpcAPI;
import org.gsc.api.GrpcAPI.AccountNetMessage;
import org.gsc.api.GrpcAPI.AccountResourceMessage;
import org.gsc.api.GrpcAPI.Address;
import org.gsc.api.GrpcAPI.AssetIssueList;
import org.gsc.api.GrpcAPI.BlockList;
import org.gsc.api.GrpcAPI.ExchangeList;
import org.gsc.api.GrpcAPI.Node;
import org.gsc.api.GrpcAPI.NodeList;
import org.gsc.api.GrpcAPI.NumberMessage;
import org.gsc.api.GrpcAPI.ProposalList;
import org.gsc.api.GrpcAPI.Return;
import org.gsc.api.GrpcAPI.Return.response_code;
import org.gsc.api.GrpcAPI.TransactionExtention.Builder;
import org.gsc.api.GrpcAPI.WitnessList;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.Hash;
import org.gsc.common.overlay.discover.node.NodeHandler;
import org.gsc.common.overlay.discover.node.NodeManager;
import org.gsc.common.overlay.message.Message;
import org.gsc.runtime.Runtime;
import org.gsc.runtime.vm.program.ProgramResult;
import org.gsc.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.gsc.common.storage.DepositImpl;
import org.gsc.common.utils.Base58;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.utils.Utils;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.config.Parameter.ChainConstant;
import org.gsc.config.Parameter.ChainParameters;
import org.gsc.config.args.Args;
import org.gsc.net.message.TransactionMessage;
import org.gsc.net.node.NodeImpl;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Contract.CreateSmartContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Contract.TriggerSmartContract;
import org.gsc.protos.Protocol;
import org.gsc.protos.Protocol.Account;
import org.gsc.protos.Protocol.Block;
import org.gsc.protos.Protocol.Exchange;
import org.gsc.protos.Protocol.Proposal;
import org.gsc.protos.Protocol.SmartContract;
import org.gsc.protos.Protocol.SmartContract.ABI;
import org.gsc.protos.Protocol.SmartContract.ABI.Entry.StateMutabilityType;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.gsc.protos.Protocol.Transaction.Result.code;
import org.gsc.protos.Protocol.TransactionSign;

@Slf4j
@Component
public class Wallet {

  @Getter
  private final ECKey ecKey;
  @Autowired
  private NodeImpl p2pNode;
  @Autowired
  private Manager dbManager;
  @Autowired
  private NodeManager nodeManager;
  private static String addressPreFixString = Constant.ADD_PRE_FIX_STRING_MAINNET;  //default testnet
  private static byte addressPreFixByte = Constant.ADD_PRE_FIX_BYTE_MAINNET;

  /**
   * Creates a new Wallet with a random ECKey.
   */
  public Wallet() {
    this.ecKey = new ECKey(Utils.getRandom());
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */
  public Wallet(final ECKey ecKey) {
    this.ecKey = ecKey;
    logger.info("wallet address: {}", ByteArray.toHexString(this.ecKey.getAddress()));
  }

  public static boolean isConstant(ABI abi, TriggerSmartContract triggerSmartContract)
      throws ContractValidateException {
    try {
      boolean constant = isConstant(abi, getSelector(triggerSmartContract.getData().toByteArray()));
      if (constant) {
        if (!Args.getInstance().isSupportConstant()) {
          throw new ContractValidateException("this node don't support constant");
        }
      }
      return constant;
    } catch (ContractValidateException e) {
      throw e;
    } catch (Exception e) {
      return false;
    }
  }

  public byte[] getAddress() {
    return ecKey.getAddress();
  }

  public static String getAddressPreFixString() {
    return addressPreFixString;
  }

  public static void setAddressPreFixString(String addressPreFixString) {
    Wallet.addressPreFixString = addressPreFixString;
  }

  public static byte getAddressPreFixByte() {
    return addressPreFixByte;
  }

  public static void setAddressPreFixByte(byte addressPreFixByte) {
    Wallet.addressPreFixByte = addressPreFixByte;
  }

  public static boolean addressValid(byte[] address) {
    if (ArrayUtils.isEmpty(address)) {
      logger.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != Constant.ADDRESS_SIZE / 2) {
      logger.warn(
          "Warning: Address length need " + Constant.ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }
    if (address[0] != addressPreFixByte) {
      logger.warn("Warning: Address need prefix with " + addressPreFixByte + " but "
          + address[0] + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }

  private static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(decodeData);
    byte[] hash1 = Sha256Hash.hash(hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  public static byte[] generateContractAddress(Transaction trx) {

    CreateSmartContract contract = ContractWrapper.getSmartContractFromTransaction(trx);
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    TransactionWrapper trxCap = new TransactionWrapper(trx);
    byte[] txRawDataHash = trxCap.getTransactionId().getBytes();

    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);

  }

  public static byte[] generateContractAddress(byte[] ownerAddress, byte[] txRawDataHash) {

    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);

  }

  public static byte[] generateContractAddress(byte[] transactionRootId, long nonce) {
    byte[] nonceBytes = Longs.toByteArray(nonce);
    byte[] combined = new byte[transactionRootId.length + nonceBytes.length];
    System.arraycopy(transactionRootId, 0, combined, 0, transactionRootId.length);
    System.arraycopy(nonceBytes, 0, combined, transactionRootId.length, nonceBytes.length);

    return Hash.sha3omit12(combined);
  }

  public static byte[] decodeFromBase58Check(String addressBase58) {
    if (StringUtils.isEmpty(addressBase58)) {
      logger.warn("Warning: Address is empty !!");
      return null;
    }
    byte[] address = decode58Check(addressBase58);
    if (address == null) {
      return null;
    }

    if (!addressValid(address)) {
      return null;
    }

    return address;
  }


  public Account getAccount(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountWrapper accountWrapper = accountStore.get(account.getAddress().toByteArray());
    if (accountWrapper == null) {
      return null;
    }
    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountWrapper);

    EnergyProcessor energyProcessor = new EnergyProcessor(dbManager);
    energyProcessor.updateUsage(accountWrapper);

    long genesisTimeStamp = dbManager.getGenesisBlock().getTimeStamp();
    accountWrapper.setLatestConsumeTime(genesisTimeStamp
        + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountWrapper.getLatestConsumeTime());
    accountWrapper.setLatestConsumeFreeTime(genesisTimeStamp
        + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountWrapper.getLatestConsumeFreeTime());
    accountWrapper.setLatestConsumeTimeForEnergy(genesisTimeStamp
        + ChainConstant.BLOCK_PRODUCED_INTERVAL * accountWrapper.getLatestConsumeTimeForEnergy());

    return accountWrapper.getInstance();
  }


  public Account getAccountById(Account account) {
    AccountStore accountStore = dbManager.getAccountStore();
    AccountIdIndexStore accountIdIndexStore = dbManager.getAccountIdIndexStore();
    byte[] address = accountIdIndexStore.get(account.getAccountId());
    if (address == null) {
      return null;
    }
    AccountWrapper accountWrapper = accountStore.get(address);
    if (accountWrapper == null) {
      return null;
    }
    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountWrapper);

    EnergyProcessor energyProcessor = new EnergyProcessor(dbManager);
    energyProcessor.updateUsage(accountWrapper);

    return accountWrapper.getInstance();
  }

  /**
   * Create a transaction.
   */
  /*public Transaction createTransaction(byte[] address, String to, long amount) {
    long balance = getBalance(address);
    return new TransactionWrapper(address, to, amount, balance, utxoStore).getInstance();
  } */

  /**
   * Create a transaction by contract.
   */
  @Deprecated
  public Transaction createTransaction(TransferContract contract) {
    AccountStore accountStore = dbManager.getAccountStore();
    return new TransactionWrapper(contract, accountStore).getInstance();
  }


  public TransactionWrapper createTransactionCapsule(com.google.protobuf.Message message,
                                                     ContractType contractType) throws ContractValidateException {
    TransactionWrapper trx = new TransactionWrapper(message, contractType);

    // do
    if(contractType == ContractType.WitnessCreateContract){
      Any any = trx.getInstance().getRawData().getContract(0).getParameter();
      try {
        Contract.WitnessCreateContract witnessCreateContract = any.unpack(Contract.WitnessCreateContract.class);
        ByteString ownerAddress = witnessCreateContract.getOwnerAddress();
        //this.

      } catch (InvalidProtocolBufferException e) {
        e.printStackTrace();
        logger.error("Generate WitnessCreateContract error!");
      }
    }

    if (contractType != ContractType.CreateSmartContract
        && contractType != ContractType.TriggerSmartContract) {
      List<Operator> actList = OperatorFactory.createActuator(trx, dbManager);
      for (Operator act : actList) {
        act.validate();
      }
    }

    if (contractType == ContractType.CreateSmartContract) {

      CreateSmartContract contract = ContractWrapper
          .getSmartContractFromTransaction(trx.getInstance());
      long percent = contract.getNewContract().getConsumeUserResourcePercent();
      if (percent < 0 || percent > 100) {
        throw new ContractValidateException("percent must be >= 0 and <= 100");
      }
    }

    try {
      BlockWrapper headBlock = null;
      List<BlockWrapper> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
      if (CollectionUtils.isEmpty(blockList)) {
        throw new HeaderNotFound("latest block not found");
      } else {
        headBlock = blockList.get(0);
      }
      trx.setReference(headBlock.getNum(), headBlock.getBlockId().getBytes());
      long expiration = headBlock.getTimeStamp() + Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;
      trx.setExpiration(expiration);
      trx.setTimestamp();
    } catch (HeaderNotFound headerNotFound) {
      headerNotFound.printStackTrace();
    }
    return trx;
  }

  /**
   * Broadcast a transaction.
   */
  public GrpcAPI.Return broadcastTransaction(Transaction signaturedTransaction) {
    GrpcAPI.Return.Builder builder = GrpcAPI.Return.newBuilder();

    try {
      TransactionWrapper trx = new TransactionWrapper(signaturedTransaction);
      logger.info("broadcast refblockhash ={}",Hex.toHexString(trx.getInstance()
              .getRawData().getRefBlockHash().toByteArray()));
      logger.info("broadcast refblockbytes={}",ByteArray.toLong(trx.getInstance()
              .getRawData().getRefBlockBytes().toByteArray()));
      Message message = new TransactionMessage(signaturedTransaction);

      if (dbManager.isTooManyPending()) {
        logger.debug(
            "Manager is busy, pending transaction count:{}, discard the new coming transaction",
            (dbManager.getPendingTransactions().size() + PendingManager.getTmpTransactions()
                .size()));
        return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
      }

      if (dbManager.isGeneratingBlock()) {
        logger.debug("Manager is generating block, discard the new coming transaction");
        return builder.setResult(false).setCode(response_code.SERVER_BUSY).build();
      }

      if (dbManager.getTransactionIdCache().getIfPresent(trx.getTransactionId()) != null) {
        logger.debug("This transaction has been processed, discard the transaction");
        return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR).build();
      } else {
        dbManager.getTransactionIdCache().put(trx.getTransactionId(), true);
      }
      if (dbManager.getDynamicPropertiesStore().supportVM()) {
        trx.resetResult();
      }
      dbManager.pushTransaction(trx);
      p2pNode.broadcast(message);

      return builder.setResult(true).setCode(response_code.SUCCESS).build();
    } catch (ValidateSignatureException e) {
      logger.info(e.getMessage());
      return builder.setResult(false).setCode(response_code.SIGERROR)
          .setMessage(ByteString.copyFromUtf8("validate signature error"))
          .build();
    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      return builder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract validate error : " + e.getMessage()))
          .build();
    } catch (ContractExeException e) {
      logger.info(e.getMessage());
      return builder.setResult(false).setCode(response_code.CONTRACT_EXE_ERROR)
          .setMessage(ByteString.copyFromUtf8("contract execute error : " + e.getMessage()))
          .build();
    } catch (AccountResourceInsufficientException e) {
      logger.info(e.getMessage());
      return builder.setResult(false).setCode(response_code.BANDWITH_ERROR)
          .setMessage(ByteString.copyFromUtf8("AccountResourceInsufficient error"))
          .build();
    } catch (DupTransactionException e) {
      logger.info("dup trans" + e.getMessage());
      return builder.setResult(false).setCode(response_code.DUP_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("dup transaction"))
          .build();
    } catch (TaposException e) {
      logger.info("tapos error" + e.getMessage());
      return builder.setResult(false).setCode(response_code.TAPOS_ERROR)
          .setMessage(ByteString.copyFromUtf8("Tapos check error"))
          .build();
    } catch (TooBigTransactionException e) {
      logger.info("transaction error" + e.getMessage());
      return builder.setResult(false).setCode(response_code.TOO_BIG_TRANSACTION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction size is too big"))
          .build();
    } catch (TransactionExpirationException e) {
      logger.info("transaction expired" + e.getMessage());
      return builder.setResult(false).setCode(response_code.TRANSACTION_EXPIRATION_ERROR)
          .setMessage(ByteString.copyFromUtf8("transaction expired"))
          .build();
    } catch (Exception e) {
      logger.info("exception caught" + e.getMessage());
      return builder.setResult(false).setCode(response_code.OTHER_ERROR)
          .setMessage(ByteString.copyFromUtf8("other error : " + e.getMessage()))
          .build();
    }
  }

  public TransactionWrapper getTransactionSign(TransactionSign transactionSign) {
    byte[] privateKey = transactionSign.getPrivateKey().toByteArray();
    TransactionWrapper trx = new TransactionWrapper(transactionSign.getTransaction());
    trx.sign(privateKey);
    return trx;
  }

  public byte[] pass2Key(byte[] passPhrase) {
    return Sha256Hash.hash(passPhrase);
  }

  public byte[] createAdresss(byte[] passPhrase) {
    byte[] privateKey = pass2Key(passPhrase);
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    return ecKey.getAddress();
  }

  public Block getNowBlock() {
    List<BlockWrapper> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
    if (CollectionUtils.isEmpty(blockList)) {
      return null;
    } else {
      return blockList.get(0).getInstance();
    }
  }

  public Block getBlockByNum(long blockNum) {
    try {
      return dbManager.getBlockByNum(blockNum).getInstance();
    } catch (StoreException e) {
      logger.info(e.getMessage());
      return null;
    }
  }

  public WitnessList getWitnessList() {
    WitnessList.Builder builder = WitnessList.newBuilder();
    List<WitnessWrapper> witnessCapsuleList = dbManager.getWitnessStore().getAllWitnesses();
    witnessCapsuleList
        .forEach(witnessCapsule -> builder.addWitnesses(witnessCapsule.getInstance()));
    return builder.build();
  }

  public ProposalList getProposalList() {
    ProposalList.Builder builder = ProposalList.newBuilder();
    List<ProposalWrapper> proposalWrapperList = dbManager.getProposalStore().getAllProposals();
    proposalWrapperList
        .forEach(proposalCapsule -> builder.addProposals(proposalCapsule.getInstance()));
    return builder.build();
  }

  public ExchangeList getExchangeList() {
    ExchangeList.Builder builder = ExchangeList.newBuilder();
    List<ExchangeWrapper> exchangeWrapperList = dbManager.getExchangeStore().getAllExchanges();
    exchangeWrapperList
        .forEach(exchangeCapsule -> builder.addExchanges(exchangeCapsule.getInstance()));
    return builder.build();
  }

  public Protocol.ChainParameters getChainParameters() {
    Protocol.ChainParameters.Builder builder = Protocol.ChainParameters.newBuilder();
    DynamicPropertiesStore dynamicPropertiesStore = dbManager.getDynamicPropertiesStore();

    Protocol.ChainParameters.ChainParameter.Builder builder1
        = Protocol.ChainParameters.ChainParameter.newBuilder();

    builder.addChainParameter(builder1
        .setKey(ChainParameters.MAINTENANCE_TIME_INTERVAL.name())
        .setValue(
            dynamicPropertiesStore.getMaintenanceTimeInterval())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.ACCOUNT_UPGRADE_COST.name())
        .setValue(
            dynamicPropertiesStore.getAccountUpgradeCost())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.CREATE_ACCOUNT_FEE.name())
        .setValue(
            dynamicPropertiesStore.getCreateAccountFee())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.TRANSACTION_FEE.name())
        .setValue(
            dynamicPropertiesStore.getTransactionFee())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.ASSET_ISSUE_FEE.name())
        .setValue(
            dynamicPropertiesStore.getAssetIssueFee())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.WITNESS_PAY_PER_BLOCK.name())
        .setValue(
            dynamicPropertiesStore.getWitnessPayPerBlock())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.WITNESS_STANDBY_ALLOWANCE.name())
        .setValue(
            dynamicPropertiesStore.getWitnessStandbyAllowance())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT.name())
        .setValue(
            dynamicPropertiesStore.getCreateNewAccountFeeInSystemContract())
        .build());
    builder.addChainParameter(builder1
        .setKey(ChainParameters.CREATE_NEW_ACCOUNT_BANDWIDTH_RATE.name())
        .setValue(
            dynamicPropertiesStore.getCreateNewAccountBandwidthRate())
        .build());

    builder.addChainParameter(builder1
        .setKey(ChainParameters.ALLOW_CREATION_OF_CONTRACTS.name())
        .setValue(
            dynamicPropertiesStore.getAllowCreationOfContracts())
        .build());

    builder.addChainParameter(builder1
        .setKey(ChainParameters.REMOVE_THE_POWER_OF_THE_GR.name())
        .setValue(
            dynamicPropertiesStore.getRemoveThePowerOfTheGr())
        .build());

    builder.addChainParameter(builder1
        .setKey(ChainParameters.ENERGY_FEE.name())
        .setValue(
            dynamicPropertiesStore.getEnergyFee())
        .build());

    builder.addChainParameter(builder1
        .setKey(ChainParameters.EXCHANGE_CREATE_FEE.name())
        .setValue(
            dynamicPropertiesStore.getExchangeCreateFee())
        .build());

    builder.addChainParameter(builder1
        .setKey(ChainParameters.MAX_CPU_TIME_OF_ONE_TX.name())
        .setValue(
            dynamicPropertiesStore.getMaxCpuTimeOfOneTX())
        .build());

    return builder.build();
  }

  public AssetIssueList getAssetIssueList() {
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    dbManager.getAssetIssueStore().getAllAssetIssues()
        .forEach(issueCapsule -> builder.addAssetIssue(issueCapsule.getInstance()));
    return builder.build();
  }


  public AssetIssueList getAssetIssueList(long offset, long limit) {
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    List<AssetIssueWrapper> assetIssueList = dbManager.getAssetIssueStore()
        .getAssetIssuesPaginated(offset, limit);

    if (CollectionUtils.isEmpty(assetIssueList)) {
      return null;
    }

    assetIssueList.forEach(issueCapsule -> builder.addAssetIssue(issueCapsule.getInstance()));
    return builder.build();
  }

  public AssetIssueList getAssetIssueByAccount(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.isEmpty()) {
      return null;
    }
    List<AssetIssueWrapper> assetIssueWrapperList = dbManager.getAssetIssueStore()
        .getAllAssetIssues();
    AssetIssueList.Builder builder = AssetIssueList.newBuilder();
    assetIssueWrapperList.stream()
        .filter(assetIssueCapsule -> assetIssueCapsule.getOwnerAddress().equals(accountAddress))
        .forEach(issueCapsule -> {
          builder.addAssetIssue(issueCapsule.getInstance());
        });
    return builder.build();
  }

  public AccountNetMessage getAccountNet(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.isEmpty()) {
      return null;
    }
    AccountNetMessage.Builder builder = AccountNetMessage.newBuilder();
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(accountAddress.toByteArray());
    if (accountWrapper == null) {
      return null;
    }

    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountWrapper);

    long netLimit = processor.calculateGlobalNetLimit(accountWrapper.getFrozenBalance());
    long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
    long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
    long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    Map<String, Long> assetNetLimitMap = new HashMap<>();
    accountWrapper.getAllFreeAssetNetUsage().keySet().forEach(asset -> {
      byte[] key = ByteArray.fromString(asset);
      assetNetLimitMap.put(asset, dbManager.getAssetIssueStore().get(key).getFreeAssetNetLimit());
    });

    builder.setFreeNetUsed(accountWrapper.getFreeNetUsage())
        .setFreeNetLimit(freeNetLimit)
        .setNetUsed(accountWrapper.getNetUsage())
        .setNetLimit(netLimit)
        .setTotalNetLimit(totalNetLimit)
        .setTotalNetWeight(totalNetWeight)
        .putAllAssetNetUsed(accountWrapper.getAllFreeAssetNetUsage())
        .putAllAssetNetLimit(assetNetLimitMap);
    return builder.build();
  }

  public AccountResourceMessage getAccountResource(ByteString accountAddress) {
    if (accountAddress == null || accountAddress.isEmpty()) {
      return null;
    }
    AccountResourceMessage.Builder builder = AccountResourceMessage.newBuilder();
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(accountAddress.toByteArray());
    if (accountWrapper == null) {
      return null;
    }

    BandwidthProcessor processor = new BandwidthProcessor(dbManager);
    processor.updateUsage(accountWrapper);

    EnergyProcessor energyProcessor = new EnergyProcessor(dbManager);
    energyProcessor.updateUsage(accountWrapper);

    long netLimit = processor.calculateGlobalNetLimit(accountWrapper.getFrozenBalance());
    long freeNetLimit = dbManager.getDynamicPropertiesStore().getFreeNetLimit();
    long totalNetLimit = dbManager.getDynamicPropertiesStore().getTotalNetLimit();
    long totalNetWeight = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
    long energyLimit = energyProcessor
        .calculateGlobalEnergyLimit(accountWrapper.getEnergyFrozenBalance());
    long totalEnergyLimit = dbManager.getDynamicPropertiesStore().getTotalEnergyLimit();
    long totalEnergyWeight = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();

    long storageLimit = accountWrapper.getAccountResource().getStorageLimit();
    long storageUsage = accountWrapper.getAccountResource().getStorageUsage();

    Map<String, Long> assetNetLimitMap = new HashMap<>();
    accountWrapper.getAllFreeAssetNetUsage().keySet().forEach(asset -> {
      byte[] key = ByteArray.fromString(asset);
      assetNetLimitMap.put(asset, dbManager.getAssetIssueStore().get(key).getFreeAssetNetLimit());
    });

    builder.setFreeNetUsed(accountWrapper.getFreeNetUsage())
        .setFreeNetLimit(freeNetLimit)
        .setNetUsed(accountWrapper.getNetUsage())
        .setNetLimit(netLimit)
        .setTotalNetLimit(totalNetLimit)
        .setTotalNetWeight(totalNetWeight)
        .setEnergyLimit(energyLimit)
        .setEnergyUsed(accountWrapper.getAccountResource().getEnergyUsage())
        .setTotalEnergyLimit(totalEnergyLimit)
        .setTotalEnergyWeight(totalEnergyWeight)
        .setStorageLimit(storageLimit)
        .setStorageUsed(storageUsage)
        .putAllAssetNetUsed(accountWrapper.getAllFreeAssetNetUsage())
        .putAllAssetNetLimit(assetNetLimitMap);
    return builder.build();
  }

  public AssetIssueContract getAssetIssueByName(ByteString assetName) {
    if (assetName == null || assetName.isEmpty()) {
      return null;
    }
    AssetIssueWrapper assetIssueWrapper = dbManager.getAssetIssueStore()
        .get(assetName.toByteArray());
    return assetIssueWrapper != null ? assetIssueWrapper.getInstance() : null;
  }

  public NumberMessage totalTransaction() {
    NumberMessage.Builder builder = NumberMessage.newBuilder()
        .setNum(dbManager.getTransactionStore().getTotalTransactions());
    return builder.build();
  }

  public NumberMessage getNextMaintenanceTime() {
    NumberMessage.Builder builder = NumberMessage.newBuilder()
        .setNum(dbManager.getDynamicPropertiesStore().getNextMaintenanceTime());
    return builder.build();
  }

  public Block getBlockById(ByteString BlockId) {
    if (Objects.isNull(BlockId)) {
      return null;
    }
    Block block = null;
    try {
      block = dbManager.getBlockStore().get(BlockId.toByteArray()).getInstance();
    } catch (StoreException e) {
    }
    return block;
  }

  public BlockList getBlocksByLimitNext(long number, long limit) {
    if (limit <= 0) {
      return null;
    }
    BlockList.Builder blockListBuilder = BlockList.newBuilder();
    dbManager.getBlockStore().getLimitNumber(number, limit).forEach(
        blockCapsule -> blockListBuilder.addBlock(blockCapsule.getInstance()));
    return blockListBuilder.build();
  }

  public BlockList getBlockByLatestNum(long getNum) {
    BlockList.Builder blockListBuilder = BlockList.newBuilder();
    dbManager.getBlockStore().getBlockByLatestNum(getNum).forEach(
        blockCapsule -> blockListBuilder.addBlock(blockCapsule.getInstance()));
    return blockListBuilder.build();
  }

  public Transaction getTransactionById(ByteString transactionId) {
    if (Objects.isNull(transactionId)) {
      return null;
    }
    TransactionWrapper transactionCapsule = null;
    try {
      transactionCapsule = dbManager.getTransactionStore()
          .get(transactionId.toByteArray());
    } catch (StoreException e) {
    }
    if (transactionCapsule != null) {
      return transactionCapsule.getInstance();
    }
    return null;
  }

  public Proposal getProposalById(ByteString proposalId) {
    if (Objects.isNull(proposalId)) {
      return null;
    }
    ProposalWrapper proposalWrapper = null;
    try {
      proposalWrapper = dbManager.getProposalStore()
          .get(proposalId.toByteArray());
    } catch (StoreException e) {
    }
    if (proposalWrapper != null) {
      return proposalWrapper.getInstance();
    }
    return null;
  }

  public Exchange getExchangeById(ByteString exchangeId) {
    if (Objects.isNull(exchangeId)) {
      return null;
    }
    ExchangeWrapper exchangeWrapper = null;
    try {
      exchangeWrapper = dbManager.getExchangeStore()
          .get(exchangeId.toByteArray());
    } catch (StoreException e) {
    }
    if (exchangeWrapper != null) {
      return exchangeWrapper.getInstance();
    }
    return null;
  }


  public NodeList listNodes() {
    List<NodeHandler> handlerList = nodeManager.dumpActiveNodes();

    Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
    for (NodeHandler handler : handlerList) {
      String key = handler.getNode().getHexId() + handler.getNode().getHost();
      System.out.println("key: " + key);
      System.out.println("handler: " + handler);
      nodeHandlerMap.put(key, handler);
    }

    NodeList.Builder nodeListBuilder = NodeList.newBuilder();

    nodeHandlerMap.entrySet().stream()
        .forEach(v -> {
          org.gsc.common.overlay.discover.node.Node node = v.getValue().getNode();
          nodeListBuilder.addNodes(Node.newBuilder().setAddress(
              Address.newBuilder()
                  .setHost(ByteString.copyFrom(ByteArray.fromString(node.getHost())))
                  .setPort(node.getPort())));
        });
    return nodeListBuilder.build();
  }

  public Transaction deployContract(CreateSmartContract createSmartContract,
      TransactionWrapper trxCap) {

    // do nothing, so can add some useful function later
    // trxcap contract para cacheUnpackValue has value

    return trxCap.getInstance();
  }

  public Transaction deployContract1(CreateSmartContract createSmartContract,
                                    TransactionWrapper trxCap, DepositImpl deposit,
                                    BlockWrapper block, Builder builder,Return.Builder retBuilder)
          throws TransactionTraceException, ContractValidateException, ContractExeException {

    // do nothing, so can add some useful function later
    // trxcap contract para cacheUnpackValue has value

    Transaction.Builder transactionBuilder = trxCap.getInstance().toBuilder();
    Transaction.raw.Builder rawBuilder = trxCap.getInstance().getRawData()
            .toBuilder();

    //rawBuilder.setFeeLimit(feeLimit);
    rawBuilder.setFeeLimit(10000000L);

    transactionBuilder.setRawData(rawBuilder);
    Transaction trx = transactionBuilder.build();

    TransactionWrapper transactionWrapper = new TransactionWrapper(trx);
    TransactionTrace trace = new TransactionTrace(transactionWrapper, deposit.getDbManager());

    Runtime runtime = new Runtime(trace, block, deposit,
            new ProgramInvokeFactoryImpl());
    runtime.execute();
    runtime.go();
    runtime.finalization();
    // init
    //trace.init();
    //exec
    //trace.exec(runtime);

    if (runtime.getResult().getException() != null) {
//          runtime.getResult().getException().printStackTrace();
      throw new RuntimeException("Runtime exe failed!");
    }

    ProgramResult result = runtime.getResult();
    TransactionResultWrapper ret = new TransactionResultWrapper();

    builder.addConstantResult(ByteString.copyFrom(result.getHReturn()));
    ret.setStatus(0, code.SUCESS);
    if (StringUtils.isNoneEmpty(runtime.getRuntimeError())) {
      ret.setStatus(0, code.FAILED);
      retBuilder.setMessage(ByteString.copyFromUtf8(runtime.getRuntimeError())).build();
    }
    transactionWrapper.setResult(ret);
    return transactionWrapper.getInstance();
  }

  public Transaction triggerContract(TriggerSmartContract triggerSmartContract,
                                     TransactionWrapper trxCap, Builder builder,
                                     Return.Builder retBuilder)
      throws ContractValidateException, ContractExeException, HeaderNotFound {

    ContractStore contractStore = dbManager.getContractStore();
    byte[] contractAddress = triggerSmartContract.getContractAddress().toByteArray();
    SmartContract.ABI abi = contractStore.getABI(contractAddress);
    if (abi == null) {
      throw new ContractValidateException("No contract or not a smart contract");
    }

    byte[] selector = getSelector(triggerSmartContract.getData().toByteArray());

    if (!isConstant(abi, selector)) {
      return trxCap.getInstance();
    } else {
      if (!Args.getInstance().isSupportConstant()) {
        throw new ContractValidateException("this node don't support constant");
      }
      DepositImpl deposit = DepositImpl.createRoot(dbManager);

      Block headBlock;
      List<BlockWrapper> blockWrapperList = dbManager.getBlockStore().getBlockByLatestNum(1);
      if (CollectionUtils.isEmpty(blockWrapperList)) {
        throw new HeaderNotFound("latest block not found");
      } else {
        headBlock = blockWrapperList.get(0).getInstance();
      }

      Runtime runtime = new Runtime(trxCap.getInstance(), new BlockWrapper(headBlock), deposit,
          new ProgramInvokeFactoryImpl());
      runtime.execute();
      runtime.go();
      runtime.finalization();
      // TODO exception
      if (runtime.getResult().getException() != null) {
//          runtime.getResult().getException().printStackTrace();
        throw new RuntimeException("Runtime exe failed!");
      }

      ProgramResult result = runtime.getResult();
      TransactionResultWrapper ret = new TransactionResultWrapper();

      builder.addConstantResult(ByteString.copyFrom(result.getHReturn()));
      ret.setStatus(0, code.SUCESS);
      if (StringUtils.isNoneEmpty(runtime.getRuntimeError())) {
        ret.setStatus(0, code.FAILED);
        retBuilder.setMessage(ByteString.copyFromUtf8(runtime.getRuntimeError())).build();
      }
      trxCap.setResult(ret);
      return trxCap.getInstance();
    }
  }

  public SmartContract getContract(GrpcAPI.BytesMessage bytesMessage) {
    byte[] address = bytesMessage.getValue().toByteArray();
    AccountWrapper accountWrapper = dbManager.getAccountStore().get(address);
    if (accountWrapper == null) {
      logger.error(
          "Get contract failed, the account is not exist or the account does not have code hash!");
      return null;
    }

    ContractWrapper contractWrapper = dbManager.getContractStore()
        .get(bytesMessage.getValue().toByteArray());
    if (Objects.nonNull(contractWrapper)) {
      return contractWrapper.getInstance();
    }
    return null;
  }

  private static byte[] getSelector(byte[] data) {
    if (data == null ||
        data.length < 4) {
      return null;
    }

    byte[] ret = new byte[4];
    System.arraycopy(data, 0, ret, 0, 4);
    return ret;
  }

  private static boolean isConstant(SmartContract.ABI abi, byte[] selector)  {

    if (selector == null || selector.length != 4 ||  abi.getEntrysList().size() == 0) {
      return false;
    }

    for (int i = 0; i < abi.getEntrysCount(); i++) {
      ABI.Entry entry = abi.getEntrys(i);
      if (entry.getType() != ABI.Entry.EntryType.Function) {
        continue;
      }

      int inputCount = entry.getInputsCount();
      StringBuffer sb = new StringBuffer();
      sb.append(entry.getName());
      sb.append("(");
      for (int k = 0; k < inputCount; k++) {
        ABI.Entry.Param param = entry.getInputs(k);
        sb.append(param.getType());
        if (k + 1 < inputCount) {
          sb.append(",");
        }
      }
      sb.append(")");

      byte[] funcSelector = new byte[4];
      System.arraycopy(Hash.sha3(sb.toString().getBytes()), 0, funcSelector, 0, 4);
      if (Arrays.equals(funcSelector, selector)) {
        if (entry.getConstant() == true || entry.getStateMutability()
            .equals(StateMutabilityType.View)) {
          return true;
        } else {
          return false;
        }
      }
    }

    return false;
  }

  public static CreateSmartContract createSmartContract(String contractName, byte[] address,ABI.Builder abiBuilder,
                                                        byte[] byteCode, long value,long consumeUserResourcePercent) {

    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(address));
    builder.setAbi(abiBuilder);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);

    if (value != 0) {
      builder.setCallValue(value);
    }
    if (!ArrayUtils.isEmpty(byteCode)) {
      builder.setBytecode(ByteString.copyFrom(byteCode));
    }

    return CreateSmartContract.newBuilder().setOwnerAddress(ByteString.copyFrom(address)).
            setNewContract(builder.build()).build();
  }

  public static Runtime processTransactionAndReturnRuntime(Transaction trx,
                                                           DepositImpl deposit, BlockWrapper block)
          throws TransactionTraceException, ContractExeException, ContractValidateException, ReceiptCheckErrException {

    TransactionWrapper trxCap = new TransactionWrapper(trx);
    TransactionTrace trace = new TransactionTrace(trxCap, deposit.getDbManager());
    Runtime runtime = new Runtime(trace, block, deposit,
            new ProgramInvokeFactoryImpl());

    // init
    trace.init();
    //exec
    trace.exec(runtime);

    return runtime;
  }

}