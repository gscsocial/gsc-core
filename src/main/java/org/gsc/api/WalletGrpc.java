package org.gsc.api;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.9.0)",
    comments = "Source: api/api.proto")
public final class WalletGrpc {

  private WalletGrpc() {}

  public static final String SERVICE_NAME = "protocol.Wallet";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetAccountMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> METHOD_GET_ACCOUNT = getGetAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> getGetAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> getGetAccountMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account, org.gsc.protos.Protocol.Account> getGetAccountMethod;
    if ((getGetAccountMethod = WalletGrpc.getGetAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAccountMethod = WalletGrpc.getGetAccountMethod) == null) {
          WalletGrpc.getGetAccountMethod = getGetAccountMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Protocol.Account, org.gsc.protos.Protocol.Account>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAccount"))
                  .build();
          }
        }
     }
     return getGetAccountMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetAccountByIdMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> METHOD_GET_ACCOUNT_BY_ID = getGetAccountByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> getGetAccountByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> getGetAccountByIdMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account, org.gsc.protos.Protocol.Account> getGetAccountByIdMethod;
    if ((getGetAccountByIdMethod = WalletGrpc.getGetAccountByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAccountByIdMethod = WalletGrpc.getGetAccountByIdMethod) == null) {
          WalletGrpc.getGetAccountByIdMethod = getGetAccountByIdMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Protocol.Account, org.gsc.protos.Protocol.Account>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAccountById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAccountById"))
                  .build();
          }
        }
     }
     return getGetAccountByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getCreateTransactionMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.TransferContract,
      org.gsc.protos.Protocol.Transaction> METHOD_CREATE_TRANSACTION = getCreateTransactionMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.TransferContract,
      org.gsc.protos.Protocol.Transaction> getCreateTransactionMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.TransferContract,
      org.gsc.protos.Protocol.Transaction> getCreateTransactionMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.TransferContract, org.gsc.protos.Protocol.Transaction> getCreateTransactionMethod;
    if ((getCreateTransactionMethod = WalletGrpc.getCreateTransactionMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateTransactionMethod = WalletGrpc.getCreateTransactionMethod) == null) {
          WalletGrpc.getCreateTransactionMethod = getCreateTransactionMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.TransferContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.TransferContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateTransaction"))
                  .build();
          }
        }
     }
     return getCreateTransactionMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getBroadcastTransactionMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Transaction,
      GrpcAPI.Return> METHOD_BROADCAST_TRANSACTION = getBroadcastTransactionMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Transaction,
      GrpcAPI.Return> getBroadcastTransactionMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Transaction,
      GrpcAPI.Return> getBroadcastTransactionMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Transaction, GrpcAPI.Return> getBroadcastTransactionMethod;
    if ((getBroadcastTransactionMethod = WalletGrpc.getBroadcastTransactionMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getBroadcastTransactionMethod = WalletGrpc.getBroadcastTransactionMethod) == null) {
          WalletGrpc.getBroadcastTransactionMethod = getBroadcastTransactionMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Protocol.Transaction, org.gsc.api.GrpcAPI.Return>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "BroadcastTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.Return.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("BroadcastTransaction"))
                  .build();
          }
        }
     }
     return getBroadcastTransactionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getUpdateAccountMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.AccountUpdateContract,
      org.gsc.protos.Protocol.Transaction> METHOD_UPDATE_ACCOUNT = getUpdateAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.AccountUpdateContract,
      org.gsc.protos.Protocol.Transaction> getUpdateAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.AccountUpdateContract,
      org.gsc.protos.Protocol.Transaction> getUpdateAccountMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.AccountUpdateContract, org.gsc.protos.Protocol.Transaction> getUpdateAccountMethod;
    if ((getUpdateAccountMethod = WalletGrpc.getUpdateAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateAccountMethod = WalletGrpc.getUpdateAccountMethod) == null) {
          WalletGrpc.getUpdateAccountMethod = getUpdateAccountMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.AccountUpdateContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.AccountUpdateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateAccount"))
                  .build();
          }
        }
     }
     return getUpdateAccountMethod;
  }




  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getVoteWitnessAccountMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.VoteWitnessContract,
      org.gsc.protos.Protocol.Transaction> METHOD_VOTE_WITNESS_ACCOUNT = getVoteWitnessAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.VoteWitnessContract,
      org.gsc.protos.Protocol.Transaction> getVoteWitnessAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.VoteWitnessContract,
      org.gsc.protos.Protocol.Transaction> getVoteWitnessAccountMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.VoteWitnessContract, org.gsc.protos.Protocol.Transaction> getVoteWitnessAccountMethod;
    if ((getVoteWitnessAccountMethod = WalletGrpc.getVoteWitnessAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getVoteWitnessAccountMethod = WalletGrpc.getVoteWitnessAccountMethod) == null) {
          WalletGrpc.getVoteWitnessAccountMethod = getVoteWitnessAccountMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.VoteWitnessContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "VoteWitnessAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.VoteWitnessContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("VoteWitnessAccount"))
                  .build();
          }
        }
     }
     return getVoteWitnessAccountMethod;
  }




  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getCreateAssetIssueMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.AssetIssueContract,
      org.gsc.protos.Protocol.Transaction> METHOD_CREATE_ASSET_ISSUE = getCreateAssetIssueMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.AssetIssueContract,
      org.gsc.protos.Protocol.Transaction> getCreateAssetIssueMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.AssetIssueContract,
      org.gsc.protos.Protocol.Transaction> getCreateAssetIssueMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.AssetIssueContract, org.gsc.protos.Protocol.Transaction> getCreateAssetIssueMethod;
    if ((getCreateAssetIssueMethod = WalletGrpc.getCreateAssetIssueMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateAssetIssueMethod = WalletGrpc.getCreateAssetIssueMethod) == null) {
          WalletGrpc.getCreateAssetIssueMethod = getCreateAssetIssueMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.AssetIssueContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateAssetIssue"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.AssetIssueContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateAssetIssue"))
                  .build();
          }
        }
     }
     return getCreateAssetIssueMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getUpdateWitnessMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.WitnessUpdateContract,
      org.gsc.protos.Protocol.Transaction> METHOD_UPDATE_WITNESS = getUpdateWitnessMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.WitnessUpdateContract,
      org.gsc.protos.Protocol.Transaction> getUpdateWitnessMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.WitnessUpdateContract,
      org.gsc.protos.Protocol.Transaction> getUpdateWitnessMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.WitnessUpdateContract, org.gsc.protos.Protocol.Transaction> getUpdateWitnessMethod;
    if ((getUpdateWitnessMethod = WalletGrpc.getUpdateWitnessMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateWitnessMethod = WalletGrpc.getUpdateWitnessMethod) == null) {
          WalletGrpc.getUpdateWitnessMethod = getUpdateWitnessMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.WitnessUpdateContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateWitness"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.WitnessUpdateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateWitness"))
                  .build();
          }
        }
     }
     return getUpdateWitnessMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getCreateAccountMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.AccountCreateContract,
      org.gsc.protos.Protocol.Transaction> METHOD_CREATE_ACCOUNT = getCreateAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.AccountCreateContract,
      org.gsc.protos.Protocol.Transaction> getCreateAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.AccountCreateContract,
      org.gsc.protos.Protocol.Transaction> getCreateAccountMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.AccountCreateContract, org.gsc.protos.Protocol.Transaction> getCreateAccountMethod;
    if ((getCreateAccountMethod = WalletGrpc.getCreateAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateAccountMethod = WalletGrpc.getCreateAccountMethod) == null) {
          WalletGrpc.getCreateAccountMethod = getCreateAccountMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.AccountCreateContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.AccountCreateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateAccount"))
                  .build();
          }
        }
     }
     return getCreateAccountMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getCreateWitnessMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.WitnessCreateContract,
      org.gsc.protos.Protocol.Transaction> METHOD_CREATE_WITNESS = getCreateWitnessMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.WitnessCreateContract,
      org.gsc.protos.Protocol.Transaction> getCreateWitnessMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.WitnessCreateContract,
      org.gsc.protos.Protocol.Transaction> getCreateWitnessMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.WitnessCreateContract, org.gsc.protos.Protocol.Transaction> getCreateWitnessMethod;
    if ((getCreateWitnessMethod = WalletGrpc.getCreateWitnessMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateWitnessMethod = WalletGrpc.getCreateWitnessMethod) == null) {
          WalletGrpc.getCreateWitnessMethod = getCreateWitnessMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.WitnessCreateContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateWitness"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.WitnessCreateContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateWitness"))
                  .build();
          }
        }
     }
     return getCreateWitnessMethod;
  }



  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getTransferAssetMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.TransferAssetContract,
      org.gsc.protos.Protocol.Transaction> METHOD_TRANSFER_ASSET = getTransferAssetMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.TransferAssetContract,
      org.gsc.protos.Protocol.Transaction> getTransferAssetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.TransferAssetContract,
      org.gsc.protos.Protocol.Transaction> getTransferAssetMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.TransferAssetContract, org.gsc.protos.Protocol.Transaction> getTransferAssetMethod;
    if ((getTransferAssetMethod = WalletGrpc.getTransferAssetMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getTransferAssetMethod = WalletGrpc.getTransferAssetMethod) == null) {
          WalletGrpc.getTransferAssetMethod = getTransferAssetMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.TransferAssetContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "TransferAsset"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.TransferAssetContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("TransferAsset"))
                  .build();
          }
        }
     }
     return getTransferAssetMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getParticipateAssetIssueMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.ParticipateAssetIssueContract,
      org.gsc.protos.Protocol.Transaction> METHOD_PARTICIPATE_ASSET_ISSUE = getParticipateAssetIssueMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.ParticipateAssetIssueContract,
      org.gsc.protos.Protocol.Transaction> getParticipateAssetIssueMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.ParticipateAssetIssueContract,
      org.gsc.protos.Protocol.Transaction> getParticipateAssetIssueMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.ParticipateAssetIssueContract, org.gsc.protos.Protocol.Transaction> getParticipateAssetIssueMethod;
    if ((getParticipateAssetIssueMethod = WalletGrpc.getParticipateAssetIssueMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getParticipateAssetIssueMethod = WalletGrpc.getParticipateAssetIssueMethod) == null) {
          WalletGrpc.getParticipateAssetIssueMethod = getParticipateAssetIssueMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.ParticipateAssetIssueContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ParticipateAssetIssue"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.ParticipateAssetIssueContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ParticipateAssetIssue"))
                  .build();
          }
        }
     }
     return getParticipateAssetIssueMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getFreezeBalanceMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.FreezeBalanceContract,
      org.gsc.protos.Protocol.Transaction> METHOD_FREEZE_BALANCE = getFreezeBalanceMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.FreezeBalanceContract,
      org.gsc.protos.Protocol.Transaction> getFreezeBalanceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.FreezeBalanceContract,
      org.gsc.protos.Protocol.Transaction> getFreezeBalanceMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.FreezeBalanceContract, org.gsc.protos.Protocol.Transaction> getFreezeBalanceMethod;
    if ((getFreezeBalanceMethod = WalletGrpc.getFreezeBalanceMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getFreezeBalanceMethod = WalletGrpc.getFreezeBalanceMethod) == null) {
          WalletGrpc.getFreezeBalanceMethod = getFreezeBalanceMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.FreezeBalanceContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "FreezeBalance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.FreezeBalanceContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("FreezeBalance"))
                  .build();
          }
        }
     }
     return getFreezeBalanceMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getUnfreezeBalanceMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.UnfreezeBalanceContract,
      org.gsc.protos.Protocol.Transaction> METHOD_UNFREEZE_BALANCE = getUnfreezeBalanceMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.UnfreezeBalanceContract,
      org.gsc.protos.Protocol.Transaction> getUnfreezeBalanceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.UnfreezeBalanceContract,
      org.gsc.protos.Protocol.Transaction> getUnfreezeBalanceMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.UnfreezeBalanceContract, org.gsc.protos.Protocol.Transaction> getUnfreezeBalanceMethod;
    if ((getUnfreezeBalanceMethod = WalletGrpc.getUnfreezeBalanceMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUnfreezeBalanceMethod = WalletGrpc.getUnfreezeBalanceMethod) == null) {
          WalletGrpc.getUnfreezeBalanceMethod = getUnfreezeBalanceMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.UnfreezeBalanceContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UnfreezeBalance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.UnfreezeBalanceContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UnfreezeBalance"))
                  .build();
          }
        }
     }
     return getUnfreezeBalanceMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getUnfreezeAssetMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.UnfreezeAssetContract,
      org.gsc.protos.Protocol.Transaction> METHOD_UNFREEZE_ASSET = getUnfreezeAssetMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.UnfreezeAssetContract,
      org.gsc.protos.Protocol.Transaction> getUnfreezeAssetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.UnfreezeAssetContract,
      org.gsc.protos.Protocol.Transaction> getUnfreezeAssetMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.UnfreezeAssetContract, org.gsc.protos.Protocol.Transaction> getUnfreezeAssetMethod;
    if ((getUnfreezeAssetMethod = WalletGrpc.getUnfreezeAssetMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUnfreezeAssetMethod = WalletGrpc.getUnfreezeAssetMethod) == null) {
          WalletGrpc.getUnfreezeAssetMethod = getUnfreezeAssetMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.UnfreezeAssetContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UnfreezeAsset"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.UnfreezeAssetContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UnfreezeAsset"))
                  .build();
          }
        }
     }
     return getUnfreezeAssetMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getWithdrawBalanceMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.WithdrawBalanceContract,
      org.gsc.protos.Protocol.Transaction> METHOD_WITHDRAW_BALANCE = getWithdrawBalanceMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.WithdrawBalanceContract,
      org.gsc.protos.Protocol.Transaction> getWithdrawBalanceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.WithdrawBalanceContract,
      org.gsc.protos.Protocol.Transaction> getWithdrawBalanceMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.WithdrawBalanceContract, org.gsc.protos.Protocol.Transaction> getWithdrawBalanceMethod;
    if ((getWithdrawBalanceMethod = WalletGrpc.getWithdrawBalanceMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getWithdrawBalanceMethod = WalletGrpc.getWithdrawBalanceMethod) == null) {
          WalletGrpc.getWithdrawBalanceMethod = getWithdrawBalanceMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.WithdrawBalanceContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "WithdrawBalance"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.WithdrawBalanceContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("WithdrawBalance"))
                  .build();
          }
        }
     }
     return getWithdrawBalanceMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getUpdateAssetMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Contract.UpdateAssetContract,
      org.gsc.protos.Protocol.Transaction> METHOD_UPDATE_ASSET = getUpdateAssetMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Contract.UpdateAssetContract,
      org.gsc.protos.Protocol.Transaction> getUpdateAssetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Contract.UpdateAssetContract,
      org.gsc.protos.Protocol.Transaction> getUpdateAssetMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Contract.UpdateAssetContract, org.gsc.protos.Protocol.Transaction> getUpdateAssetMethod;
    if ((getUpdateAssetMethod = WalletGrpc.getUpdateAssetMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getUpdateAssetMethod = WalletGrpc.getUpdateAssetMethod) == null) {
          WalletGrpc.getUpdateAssetMethod = getUpdateAssetMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Contract.UpdateAssetContract, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "UpdateAsset"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.UpdateAssetContract.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("UpdateAsset"))
                  .build();
          }
        }
     }
     return getUpdateAssetMethod;
  }

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getListNodesMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.NodeList> METHOD_LIST_NODES = getListNodesMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.NodeList> getListNodesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.NodeList> getListNodesMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage, GrpcAPI.NodeList> getListNodesMethod;
    if ((getListNodesMethod = WalletGrpc.getListNodesMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getListNodesMethod = WalletGrpc.getListNodesMethod) == null) {
          WalletGrpc.getListNodesMethod = getListNodesMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.NodeList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ListNodes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NodeList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ListNodes"))
                  .build();
          }
        }
     }
     return getListNodesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetAssetIssueByAccountMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      GrpcAPI.AssetIssueList> METHOD_GET_ASSET_ISSUE_BY_ACCOUNT = getGetAssetIssueByAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      GrpcAPI.AssetIssueList> getGetAssetIssueByAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      GrpcAPI.AssetIssueList> getGetAssetIssueByAccountMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account, GrpcAPI.AssetIssueList> getGetAssetIssueByAccountMethod;
    if ((getGetAssetIssueByAccountMethod = WalletGrpc.getGetAssetIssueByAccountMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAssetIssueByAccountMethod = WalletGrpc.getGetAssetIssueByAccountMethod) == null) {
          WalletGrpc.getGetAssetIssueByAccountMethod = getGetAssetIssueByAccountMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Protocol.Account, org.gsc.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAssetIssueByAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAssetIssueByAccount"))
                  .build();
          }
        }
     }
     return getGetAssetIssueByAccountMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetAccountNetMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      GrpcAPI.AccountNetMessage> METHOD_GET_ACCOUNT_NET = getGetAccountNetMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      GrpcAPI.AccountNetMessage> getGetAccountNetMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      GrpcAPI.AccountNetMessage> getGetAccountNetMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account, GrpcAPI.AccountNetMessage> getGetAccountNetMethod;
    if ((getGetAccountNetMethod = WalletGrpc.getGetAccountNetMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAccountNetMethod = WalletGrpc.getGetAccountNetMethod) == null) {
          WalletGrpc.getGetAccountNetMethod = getGetAccountNetMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Protocol.Account, org.gsc.api.GrpcAPI.AccountNetMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAccountNet"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.AccountNetMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAccountNet"))
                  .build();
          }
        }
     }
     return getGetAccountNetMethod;
  }

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetAssetIssueByNameMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Contract.AssetIssueContract> METHOD_GET_ASSET_ISSUE_BY_NAME = getGetAssetIssueByNameMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Contract.AssetIssueContract> getGetAssetIssueByNameMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Contract.AssetIssueContract> getGetAssetIssueByNameMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.BytesMessage, org.gsc.protos.Contract.AssetIssueContract> getGetAssetIssueByNameMethod;
    if ((getGetAssetIssueByNameMethod = WalletGrpc.getGetAssetIssueByNameMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAssetIssueByNameMethod = WalletGrpc.getGetAssetIssueByNameMethod) == null) {
          WalletGrpc.getGetAssetIssueByNameMethod = getGetAssetIssueByNameMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Contract.AssetIssueContract>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAssetIssueByName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.AssetIssueContract.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAssetIssueByName"))
                  .build();
          }
        }
     }
     return getGetAssetIssueByNameMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetNowBlockMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      org.gsc.protos.Protocol.Block> METHOD_GET_NOW_BLOCK = getGetNowBlockMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      org.gsc.protos.Protocol.Block> getGetNowBlockMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      org.gsc.protos.Protocol.Block> getGetNowBlockMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage, org.gsc.protos.Protocol.Block> getGetNowBlockMethod;
    if ((getGetNowBlockMethod = WalletGrpc.getGetNowBlockMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetNowBlockMethod = WalletGrpc.getGetNowBlockMethod) == null) {
          WalletGrpc.getGetNowBlockMethod = getGetNowBlockMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetNowBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetNowBlock"))
                  .build();
          }
        }
     }
     return getGetNowBlockMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetBlockByNumMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.NumberMessage,
      org.gsc.protos.Protocol.Block> METHOD_GET_BLOCK_BY_NUM = getGetBlockByNumMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.NumberMessage,
      org.gsc.protos.Protocol.Block> getGetBlockByNumMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.NumberMessage,
      org.gsc.protos.Protocol.Block> getGetBlockByNumMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.NumberMessage, org.gsc.protos.Protocol.Block> getGetBlockByNumMethod;
    if ((getGetBlockByNumMethod = WalletGrpc.getGetBlockByNumMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByNumMethod = WalletGrpc.getGetBlockByNumMethod) == null) {
          WalletGrpc.getGetBlockByNumMethod = getGetBlockByNumMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.NumberMessage, org.gsc.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockByNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockByNum"))
                  .build();
          }
        }
     }
     return getGetBlockByNumMethod;
  }

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetTransactionCountByBlockNumMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.NumberMessage,
      GrpcAPI.NumberMessage> METHOD_GET_TRANSACTION_COUNT_BY_BLOCK_NUM = getGetTransactionCountByBlockNumMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.NumberMessage,
      GrpcAPI.NumberMessage> getGetTransactionCountByBlockNumMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.NumberMessage,
      GrpcAPI.NumberMessage> getGetTransactionCountByBlockNumMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.NumberMessage, GrpcAPI.NumberMessage> getGetTransactionCountByBlockNumMethod;
    if ((getGetTransactionCountByBlockNumMethod = WalletGrpc.getGetTransactionCountByBlockNumMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionCountByBlockNumMethod = WalletGrpc.getGetTransactionCountByBlockNumMethod) == null) {
          WalletGrpc.getGetTransactionCountByBlockNumMethod = getGetTransactionCountByBlockNumMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.NumberMessage, org.gsc.api.GrpcAPI.NumberMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionCountByBlockNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionCountByBlockNum"))
                  .build();
          }
        }
     }
     return getGetTransactionCountByBlockNumMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetBlockByIdMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Block> METHOD_GET_BLOCK_BY_ID = getGetBlockByIdMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Block> getGetBlockByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Block> getGetBlockByIdMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.BytesMessage, org.gsc.protos.Protocol.Block> getGetBlockByIdMethod;
    if ((getGetBlockByIdMethod = WalletGrpc.getGetBlockByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByIdMethod = WalletGrpc.getGetBlockByIdMethod) == null) {
          WalletGrpc.getGetBlockByIdMethod = getGetBlockByIdMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockById"))
                  .build();
          }
        }
     }
     return getGetBlockByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetBlockByLimitNextMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.BlockLimit,
      GrpcAPI.BlockList> METHOD_GET_BLOCK_BY_LIMIT_NEXT = getGetBlockByLimitNextMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.BlockLimit,
      GrpcAPI.BlockList> getGetBlockByLimitNextMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.BlockLimit,
      GrpcAPI.BlockList> getGetBlockByLimitNextMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.BlockLimit, GrpcAPI.BlockList> getGetBlockByLimitNextMethod;
    if ((getGetBlockByLimitNextMethod = WalletGrpc.getGetBlockByLimitNextMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByLimitNextMethod = WalletGrpc.getGetBlockByLimitNextMethod) == null) {
          WalletGrpc.getGetBlockByLimitNextMethod = getGetBlockByLimitNextMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BlockLimit, org.gsc.api.GrpcAPI.BlockList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockByLimitNext"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BlockLimit.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BlockList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockByLimitNext"))
                  .build();
          }
        }
     }
     return getGetBlockByLimitNextMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetBlockByLatestNumMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.NumberMessage,
      GrpcAPI.BlockList> METHOD_GET_BLOCK_BY_LATEST_NUM = getGetBlockByLatestNumMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.NumberMessage,
      GrpcAPI.BlockList> getGetBlockByLatestNumMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.NumberMessage,
      GrpcAPI.BlockList> getGetBlockByLatestNumMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.NumberMessage, GrpcAPI.BlockList> getGetBlockByLatestNumMethod;
    if ((getGetBlockByLatestNumMethod = WalletGrpc.getGetBlockByLatestNumMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetBlockByLatestNumMethod = WalletGrpc.getGetBlockByLatestNumMethod) == null) {
          WalletGrpc.getGetBlockByLatestNumMethod = getGetBlockByLatestNumMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.NumberMessage, org.gsc.api.GrpcAPI.BlockList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetBlockByLatestNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BlockList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetBlockByLatestNum"))
                  .build();
          }
        }
     }
     return getGetBlockByLatestNumMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetTransactionByIdMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Transaction> METHOD_GET_TRANSACTION_BY_ID = getGetTransactionByIdMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Transaction> getGetTransactionByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Transaction> getGetTransactionByIdMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.BytesMessage, org.gsc.protos.Protocol.Transaction> getGetTransactionByIdMethod;
    if ((getGetTransactionByIdMethod = WalletGrpc.getGetTransactionByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionByIdMethod = WalletGrpc.getGetTransactionByIdMethod) == null) {
          WalletGrpc.getGetTransactionByIdMethod = getGetTransactionByIdMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionById"))
                  .build();
          }
        }
     }
     return getGetTransactionByIdMethod;
  }






  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getListWitnessesMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.WitnessList> METHOD_LIST_WITNESSES = getListWitnessesMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.WitnessList> getListWitnessesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.WitnessList> getListWitnessesMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage, GrpcAPI.WitnessList> getListWitnessesMethod;
    if ((getListWitnessesMethod = WalletGrpc.getListWitnessesMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getListWitnessesMethod = WalletGrpc.getListWitnessesMethod) == null) {
          WalletGrpc.getListWitnessesMethod = getListWitnessesMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.WitnessList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "ListWitnesses"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.WitnessList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("ListWitnesses"))
                  .build();
          }
        }
     }
     return getListWitnessesMethod;
  }






  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetAssetIssueListMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.AssetIssueList> METHOD_GET_ASSET_ISSUE_LIST = getGetAssetIssueListMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.AssetIssueList> getGetAssetIssueListMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.AssetIssueList> getGetAssetIssueListMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage, GrpcAPI.AssetIssueList> getGetAssetIssueListMethod;
    if ((getGetAssetIssueListMethod = WalletGrpc.getGetAssetIssueListMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetAssetIssueListMethod = WalletGrpc.getGetAssetIssueListMethod) == null) {
          WalletGrpc.getGetAssetIssueListMethod = getGetAssetIssueListMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetAssetIssueList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetAssetIssueList"))
                  .build();
          }
        }
     }
     return getGetAssetIssueListMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetPaginatedAssetIssueListMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.PaginatedMessage,
      GrpcAPI.AssetIssueList> METHOD_GET_PAGINATED_ASSET_ISSUE_LIST = getGetPaginatedAssetIssueListMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.PaginatedMessage,
      GrpcAPI.AssetIssueList> getGetPaginatedAssetIssueListMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.PaginatedMessage,
      GrpcAPI.AssetIssueList> getGetPaginatedAssetIssueListMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.PaginatedMessage, GrpcAPI.AssetIssueList> getGetPaginatedAssetIssueListMethod;
    if ((getGetPaginatedAssetIssueListMethod = WalletGrpc.getGetPaginatedAssetIssueListMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetPaginatedAssetIssueListMethod = WalletGrpc.getGetPaginatedAssetIssueListMethod) == null) {
          WalletGrpc.getGetPaginatedAssetIssueListMethod = getGetPaginatedAssetIssueListMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.PaginatedMessage, org.gsc.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetPaginatedAssetIssueList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.PaginatedMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetPaginatedAssetIssueList"))
                  .build();
          }
        }
     }
     return getGetPaginatedAssetIssueListMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getTotalTransactionMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.NumberMessage> METHOD_TOTAL_TRANSACTION = getTotalTransactionMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.NumberMessage> getTotalTransactionMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.NumberMessage> getTotalTransactionMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage, GrpcAPI.NumberMessage> getTotalTransactionMethod;
    if ((getTotalTransactionMethod = WalletGrpc.getTotalTransactionMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getTotalTransactionMethod = WalletGrpc.getTotalTransactionMethod) == null) {
          WalletGrpc.getTotalTransactionMethod = getTotalTransactionMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.NumberMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "TotalTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("TotalTransaction"))
                  .build();
          }
        }
     }
     return getTotalTransactionMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetNextMaintenanceTimeMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.NumberMessage> METHOD_GET_NEXT_MAINTENANCE_TIME = getGetNextMaintenanceTimeMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.NumberMessage> getGetNextMaintenanceTimeMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.NumberMessage> getGetNextMaintenanceTimeMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage, GrpcAPI.NumberMessage> getGetNextMaintenanceTimeMethod;
    if ((getGetNextMaintenanceTimeMethod = WalletGrpc.getGetNextMaintenanceTimeMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetNextMaintenanceTimeMethod = WalletGrpc.getGetNextMaintenanceTimeMethod) == null) {
          WalletGrpc.getGetNextMaintenanceTimeMethod = getGetNextMaintenanceTimeMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.NumberMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetNextMaintenanceTime"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetNextMaintenanceTime"))
                  .build();
          }
        }
     }
     return getGetNextMaintenanceTimeMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetTransactionSignMethod()} instead.
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Protocol.TransactionSign,
      org.gsc.protos.Protocol.Transaction> METHOD_GET_TRANSACTION_SIGN = getGetTransactionSignMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Protocol.TransactionSign,
      org.gsc.protos.Protocol.Transaction> getGetTransactionSignMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Protocol.TransactionSign,
      org.gsc.protos.Protocol.Transaction> getGetTransactionSignMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Protocol.TransactionSign, org.gsc.protos.Protocol.Transaction> getGetTransactionSignMethod;
    if ((getGetTransactionSignMethod = WalletGrpc.getGetTransactionSignMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionSignMethod = WalletGrpc.getGetTransactionSignMethod) == null) {
          WalletGrpc.getGetTransactionSignMethod = getGetTransactionSignMethod =
              io.grpc.MethodDescriptor.<org.gsc.protos.Protocol.TransactionSign, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionSign"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.TransactionSign.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionSign"))
                  .build();
          }
        }
     }
     return getGetTransactionSignMethod;
  }


  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getCreateAddressMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      GrpcAPI.BytesMessage> METHOD_CREATE_ADDRESS = getCreateAddressMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      GrpcAPI.BytesMessage> getCreateAddressMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      GrpcAPI.BytesMessage> getCreateAddressMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.BytesMessage, GrpcAPI.BytesMessage> getCreateAddressMethod;
    if ((getCreateAddressMethod = WalletGrpc.getCreateAddressMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getCreateAddressMethod = WalletGrpc.getCreateAddressMethod) == null) {
          WalletGrpc.getCreateAddressMethod = getCreateAddressMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.api.GrpcAPI.BytesMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "CreateAddress"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("CreateAddress"))
                  .build();
          }
        }
     }
     return getCreateAddressMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getEasyTransferMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.EasyTransferMessage,
      GrpcAPI.EasyTransferResponse> METHOD_EASY_TRANSFER = getEasyTransferMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.EasyTransferMessage,
      GrpcAPI.EasyTransferResponse> getEasyTransferMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.EasyTransferMessage,
      GrpcAPI.EasyTransferResponse> getEasyTransferMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.EasyTransferMessage, GrpcAPI.EasyTransferResponse> getEasyTransferMethod;
    if ((getEasyTransferMethod = WalletGrpc.getEasyTransferMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getEasyTransferMethod = WalletGrpc.getEasyTransferMethod) == null) {
          WalletGrpc.getEasyTransferMethod = getEasyTransferMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EasyTransferMessage, org.gsc.api.GrpcAPI.EasyTransferResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "EasyTransfer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EasyTransferMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EasyTransferResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("EasyTransfer"))
                  .build();
          }
        }
     }
     return getEasyTransferMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGenerateAddressMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.AddressPrKeyPairMessage> METHOD_GENERATE_ADDRESS = getGenerateAddressMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.AddressPrKeyPairMessage> getGenerateAddressMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage,
      GrpcAPI.AddressPrKeyPairMessage> getGenerateAddressMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.EmptyMessage, GrpcAPI.AddressPrKeyPairMessage> getGenerateAddressMethod;
    if ((getGenerateAddressMethod = WalletGrpc.getGenerateAddressMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGenerateAddressMethod = WalletGrpc.getGenerateAddressMethod) == null) {
          WalletGrpc.getGenerateAddressMethod = getGenerateAddressMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.AddressPrKeyPairMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GenerateAddress"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.AddressPrKeyPairMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GenerateAddress"))
                  .build();
          }
        }
     }
     return getGenerateAddressMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @Deprecated // Use {@link #getGetTransactionInfoByIdMethod()} instead.
  public static final io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.TransactionInfo> METHOD_GET_TRANSACTION_INFO_BY_ID = getGetTransactionInfoByIdMethod();

  private static volatile io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.TransactionInfo> getGetTransactionInfoByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.TransactionInfo> getGetTransactionInfoByIdMethod() {
    io.grpc.MethodDescriptor<GrpcAPI.BytesMessage, org.gsc.protos.Protocol.TransactionInfo> getGetTransactionInfoByIdMethod;
    if ((getGetTransactionInfoByIdMethod = WalletGrpc.getGetTransactionInfoByIdMethod) == null) {
      synchronized (WalletGrpc.class) {
        if ((getGetTransactionInfoByIdMethod = WalletGrpc.getGetTransactionInfoByIdMethod) == null) {
          WalletGrpc.getGetTransactionInfoByIdMethod = getGetTransactionInfoByIdMethod =
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.TransactionInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.Wallet", "GetTransactionInfoById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.TransactionInfo.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletMethodDescriptorSupplier("GetTransactionInfoById"))
                  .build();
          }
        }
     }
     return getGetTransactionInfoByIdMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WalletStub newStub(io.grpc.Channel channel) {
    return new WalletStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WalletBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new WalletBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WalletFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new WalletFutureStub(channel);
  }

  /**
   */
  public static abstract class WalletImplBase implements io.grpc.BindableService {

    /**
     */
    public void getAccount(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Account> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountMethod(), responseObserver);
    }

    /**
     */
    public void getAccountById(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Account> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountByIdMethod(), responseObserver);
    }

    /**
     * <pre>
     *Please use CreateTransaction2 instead of this function.
     * </pre>
     */
    public void createTransaction(org.gsc.protos.Contract.TransferContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateTransactionMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateTransaction.
     * </pre>
     */

    /**
     */
    public void broadcastTransaction(org.gsc.protos.Protocol.Transaction request,
        io.grpc.stub.StreamObserver<GrpcAPI.Return> responseObserver) {
      asyncUnimplementedUnaryCall(getBroadcastTransactionMethod(), responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateAccount2 instead of this function.
     * </pre>
     */
    public void updateAccount(org.gsc.protos.Contract.AccountUpdateContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAccountMethod(), responseObserver);
    }

    /**

    public void setAccountId(org.gsc.protos.Contract.SetAccountIdContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getSetAccountIdMethod(), responseObserver);
    }
     */

    /**
     * <pre>
     *Use this function instead of UpdateAccount.
     * </pre>

    public void updateAccount2(org.gsc.protos.Contract.AccountUpdateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAccount2Method(), responseObserver);
    }
     */
    /**
     * <pre>
     *Please use VoteWitnessAccount2 instead of this function.
     * </pre>
     */
    public void voteWitnessAccount(org.gsc.protos.Contract.VoteWitnessContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getVoteWitnessAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     *modify the consume_user_resource_percent
     * </pre>

    public void updateSetting(org.gsc.protos.Contract.UpdateSettingContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateSettingMethod(), responseObserver);
    }
     */
    /**
     * <pre>
     *Use this function instead of VoteWitnessAccount.
     * </pre>

    public void voteWitnessAccount2(org.gsc.protos.Contract.VoteWitnessContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getVoteWitnessAccount2Method(), responseObserver);
    }
     */
    /**
     * <pre>
     *Please use CreateAssetIssue2 instead of this function.
     * </pre>
     */
    public void createAssetIssue(org.gsc.protos.Contract.AssetIssueContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAssetIssueMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateAssetIssue.
     * </pre>

    public void createAssetIssue2(org.gsc.protos.Contract.AssetIssueContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAssetIssue2Method(), responseObserver);
    }
     */
    /**
     * <pre>
     *Please use UpdateWitness2 instead of this function.
     * </pre>
     */
    public void updateWitness(org.gsc.protos.Contract.WitnessUpdateContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateWitnessMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateWitness.
     * </pre>

    public void updateWitness2(org.gsc.protos.Contract.WitnessUpdateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateWitness2Method(), responseObserver);
    }
     */
    /**
     * <pre>
     *Please use CreateAccount2 instead of this function.
     * </pre>
     */
    public void createAccount(org.gsc.protos.Contract.AccountCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateAccount.
     * </pre>

    public void createAccount2(org.gsc.protos.Contract.AccountCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAccount2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use CreateWitness2 instead of this function.
     * </pre>
     */
    public void createWitness(org.gsc.protos.Contract.WitnessCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateWitnessMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateWitness.
     * </pre>

    public void createWitness2(org.gsc.protos.Contract.WitnessCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateWitness2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use TransferAsset2 instead of this function.
     * </pre>
     */
    public void transferAsset(org.gsc.protos.Contract.TransferAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getTransferAssetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of TransferAsset.
     * </pre>

    public void transferAsset2(org.gsc.protos.Contract.TransferAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getTransferAsset2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use ParticipateAssetIssue2 instead of this function.
     * </pre>
     */
    public void participateAssetIssue(org.gsc.protos.Contract.ParticipateAssetIssueContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getParticipateAssetIssueMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of ParticipateAssetIssue.
     * </pre>

    public void participateAssetIssue2(org.gsc.protos.Contract.ParticipateAssetIssueContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getParticipateAssetIssue2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use FreezeBalance2 instead of this function.
     * </pre>
     */
    public void freezeBalance(org.gsc.protos.Contract.FreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getFreezeBalanceMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of FreezeBalance.
     * </pre>

    public void freezeBalance2(org.gsc.protos.Contract.FreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getFreezeBalance2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use UnfreezeBalance2 instead of this function.
     * </pre>
     */
    public void unfreezeBalance(org.gsc.protos.Contract.UnfreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUnfreezeBalanceMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeBalance.
     * </pre>

    public void unfreezeBalance2(org.gsc.protos.Contract.UnfreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUnfreezeBalance2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use UnfreezeAsset2 instead of this function.
     * </pre>
     */
    public void unfreezeAsset(org.gsc.protos.Contract.UnfreezeAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUnfreezeAssetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeAsset.
     * </pre>

    public void unfreezeAsset2(org.gsc.protos.Contract.UnfreezeAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUnfreezeAsset2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use WithdrawBalance2 instead of this function.
     * </pre>
     */
    public void withdrawBalance(org.gsc.protos.Contract.WithdrawBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getWithdrawBalanceMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of WithdrawBalance.
     * </pre>

    public void withdrawBalance2(org.gsc.protos.Contract.WithdrawBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getWithdrawBalance2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateAsset2 instead of this function.
     * </pre>
     */
    public void updateAsset(org.gsc.protos.Contract.UpdateAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAssetMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAsset.
     * </pre>

    public void updateAsset2(org.gsc.protos.Contract.UpdateAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateAsset2Method(), responseObserver);
    }

    /**

    public void proposalCreate(org.gsc.protos.Contract.ProposalCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getProposalCreateMethod(), responseObserver);
    }

    /**

    public void proposalApprove(org.gsc.protos.Contract.ProposalApproveContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getProposalApproveMethod(), responseObserver);
    }

    /**

    public void proposalDelete(org.gsc.protos.Contract.ProposalDeleteContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getProposalDeleteMethod(), responseObserver);
    }

    /**

    public void buyStorage(org.gsc.protos.Contract.BuyStorageContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getBuyStorageMethod(), responseObserver);
    }

    /**

    public void buyStorageBytes(org.gsc.protos.Contract.BuyStorageBytesContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getBuyStorageBytesMethod(), responseObserver);
    }

    /**

    public void sellStorage(org.gsc.protos.Contract.SellStorageContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getSellStorageMethod(), responseObserver);
    }

    /**
     */
    public void listNodes(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.NodeList> responseObserver) {
      asyncUnimplementedUnaryCall(getListNodesMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueByAccount(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueByAccountMethod(), responseObserver);
    }

    /**
     */
    public void getAccountNet(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<GrpcAPI.AccountNetMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountNetMethod(), responseObserver);
    }

    /**

    public void getAccountResource(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AccountResourceMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountResourceMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueByName(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueByNameMethod(), responseObserver);
    }

    /**
     * <pre>
     *Please use GetNowBlock2 instead of this function.
     * </pre>
     */
    public void getNowBlock(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block> responseObserver) {
      asyncUnimplementedUnaryCall(getGetNowBlockMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetNowBlock.
     * </pre>

    public void getNowBlock2(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetNowBlock2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByNum2 instead of this function.
     * </pre>
     */
    public void getBlockByNum(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByNumMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByNum.
     * </pre>

    public void getBlockByNum2(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByNum2Method(), responseObserver);
    }

    /**
     */
    public void getTransactionCountByBlockNum(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.NumberMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionCountByBlockNumMethod(), responseObserver);
    }

    /**
     */
    public void getBlockById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByIdMethod(), responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByLimitNext2 instead of this function.
     * </pre>
     */
    public void getBlockByLimitNext(org.gsc.api.GrpcAPI.BlockLimit request,
        io.grpc.stub.StreamObserver<GrpcAPI.BlockList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByLimitNextMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLimitNext.
     * </pre>

    public void getBlockByLimitNext2(org.gsc.api.GrpcAPI.BlockLimit request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockListExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByLimitNext2Method(), responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByLatestNum2 instead of this function.
     * </pre>
     */
    public void getBlockByLatestNum(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.BlockList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByLatestNumMethod(), responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLatestNum.
     * </pre>

    public void getBlockByLatestNum2(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockListExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByLatestNum2Method(), responseObserver);
    }

    /**
     */
    public void getTransactionById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionByIdMethod(), responseObserver);
    }

    /**

    public void deployContract(org.gsc.protos.Contract.CreateSmartContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getDeployContractMethod(), responseObserver);
    }

    /**

    public void getContract(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.SmartContract> responseObserver) {
      asyncUnimplementedUnaryCall(getGetContractMethod(), responseObserver);
    }

    /**

    public void triggerContract(org.gsc.protos.Contract.TriggerSmartContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getTriggerContractMethod(), responseObserver);
    }

    /**
     */
    public void listWitnesses(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.WitnessList> responseObserver) {
      asyncUnimplementedUnaryCall(getListWitnessesMethod(), responseObserver);
    }

    /**

    public void listProposals(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.ProposalList> responseObserver) {
      asyncUnimplementedUnaryCall(getListProposalsMethod(), responseObserver);
    }

    /**

    public void getProposalById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Proposal> responseObserver) {
      asyncUnimplementedUnaryCall(getGetProposalByIdMethod(), responseObserver);
    }

    /**

    public void getChainParameters(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.ChainParameters> responseObserver) {
      asyncUnimplementedUnaryCall(getGetChainParametersMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueList(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueListMethod(), responseObserver);
    }

    /**
     */
    public void getPaginatedAssetIssueList(org.gsc.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetPaginatedAssetIssueListMethod(), responseObserver);
    }

    /**
     */
    public void totalTransaction(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.NumberMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getTotalTransactionMethod(), responseObserver);
    }

    /**
     */
    public void getNextMaintenanceTime(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.NumberMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGetNextMaintenanceTimeMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Please use GetTransactionSign2 instead of this function.
     * </pre>
     */
    public void getTransactionSign(org.gsc.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionSignMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Use this function instead of GetTransactionSign.
     * </pre>

    public void getTransactionSign2(org.gsc.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionSign2Method(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void createAddress(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.BytesMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateAddressMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransfer(org.gsc.api.GrpcAPI.EasyTransferMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getEasyTransferMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>

    public void easyTransferByPrivate(org.gsc.api.GrpcAPI.EasyTransferByPrivateMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getEasyTransferByPrivateMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void generateAddress(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.AddressPrKeyPairMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGenerateAddressMethod(), responseObserver);
    }

    /**
     */
    public void getTransactionInfoById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.TransactionInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionInfoByIdMethod(), responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Protocol.Account,
                org.gsc.protos.Protocol.Account>(
                  this, METHODID_GET_ACCOUNT)))
          .addMethod(
            getGetAccountByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Protocol.Account,
                org.gsc.protos.Protocol.Account>(
                  this, METHODID_GET_ACCOUNT_BY_ID)))
          .addMethod(
            getCreateTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.TransferContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_CREATE_TRANSACTION)))
         /* .addMethod(
            getCreateTransaction2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.TransferContract,
                org.gsc.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_CREATE_TRANSACTION2)))
                  */
          .addMethod(
            getBroadcastTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Protocol.Transaction,
                org.gsc.api.GrpcAPI.Return>(
                  this, METHODID_BROADCAST_TRANSACTION)))
          .addMethod(
            getUpdateAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.AccountUpdateContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_UPDATE_ACCOUNT)))
          /*.addMethod(
            getSetAccountIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.SetAccountIdContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_SET_ACCOUNT_ID)))
          .addMethod(
            getUpdateAccount2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.AccountUpdateContract,
                org.gsc.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_UPDATE_ACCOUNT2)))
                  */
          .addMethod(
            getVoteWitnessAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.VoteWitnessContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_VOTE_WITNESS_ACCOUNT)))
         /* .addMethod(
            getUpdateSettingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.UpdateSettingContract,
                org.gsc.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_UPDATE_SETTING)))
                  */
         /* .addMethod(
            getVoteWitnessAccount2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.VoteWitnessContract,
                org.gsc.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_VOTE_WITNESS_ACCOUNT2)))
                  */
          .addMethod(
            getCreateAssetIssueMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.AssetIssueContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_CREATE_ASSET_ISSUE)))
         /* .addMethod(
            getCreateAssetIssue2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.AssetIssueContract,
                org.gsc.api.GrpcAPI.TransactionExtention>(
                  this, METHODID_CREATE_ASSET_ISSUE2)))
                  */
          .addMethod(
            getUpdateWitnessMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.WitnessUpdateContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_UPDATE_WITNESS)))

          .addMethod(
            getCreateAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.AccountCreateContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_CREATE_ACCOUNT)))

          .addMethod(
            getCreateWitnessMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.WitnessCreateContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_CREATE_WITNESS)))

          .addMethod(
            getTransferAssetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.TransferAssetContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_TRANSFER_ASSET)))

          .addMethod(
            getParticipateAssetIssueMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.ParticipateAssetIssueContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_PARTICIPATE_ASSET_ISSUE)))

          .addMethod(
            getFreezeBalanceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.FreezeBalanceContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_FREEZE_BALANCE)))

          .addMethod(
            getUnfreezeBalanceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.UnfreezeBalanceContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_UNFREEZE_BALANCE)))

          .addMethod(
            getUnfreezeAssetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.UnfreezeAssetContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_UNFREEZE_ASSET)))

          .addMethod(
            getWithdrawBalanceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.WithdrawBalanceContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_WITHDRAW_BALANCE)))

          .addMethod(
            getUpdateAssetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Contract.UpdateAssetContract,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_UPDATE_ASSET)))




          .addMethod(
            getListNodesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.api.GrpcAPI.NodeList>(
                  this, METHODID_LIST_NODES)))
          .addMethod(
            getGetAssetIssueByAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Protocol.Account,
                org.gsc.api.GrpcAPI.AssetIssueList>(
                  this, METHODID_GET_ASSET_ISSUE_BY_ACCOUNT)))
          .addMethod(
            getGetAccountNetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Protocol.Account,
                org.gsc.api.GrpcAPI.AccountNetMessage>(
                  this, METHODID_GET_ACCOUNT_NET)))

          .addMethod(
            getGetAssetIssueByNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Contract.AssetIssueContract>(
                  this, METHODID_GET_ASSET_ISSUE_BY_NAME)))
          .addMethod(
            getGetNowBlockMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.protos.Protocol.Block>(
                  this, METHODID_GET_NOW_BLOCK)))

          .addMethod(
            getGetBlockByNumMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.NumberMessage,
                org.gsc.protos.Protocol.Block>(
                  this, METHODID_GET_BLOCK_BY_NUM)))

          .addMethod(
            getGetTransactionCountByBlockNumMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.NumberMessage,
                org.gsc.api.GrpcAPI.NumberMessage>(
                  this, METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM)))
          .addMethod(
            getGetBlockByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Protocol.Block>(
                  this, METHODID_GET_BLOCK_BY_ID)))
          .addMethod(
            getGetBlockByLimitNextMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BlockLimit,
                org.gsc.api.GrpcAPI.BlockList>(
                  this, METHODID_GET_BLOCK_BY_LIMIT_NEXT)))

          .addMethod(
            getGetBlockByLatestNumMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.NumberMessage,
                org.gsc.api.GrpcAPI.BlockList>(
                  this, METHODID_GET_BLOCK_BY_LATEST_NUM)))

          .addMethod(
            getGetTransactionByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_GET_TRANSACTION_BY_ID)))



          .addMethod(
            getListWitnessesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.api.GrpcAPI.WitnessList>(
                  this, METHODID_LIST_WITNESSES)))



          .addMethod(
            getGetAssetIssueListMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.api.GrpcAPI.AssetIssueList>(
                  this, METHODID_GET_ASSET_ISSUE_LIST)))
          .addMethod(
            getGetPaginatedAssetIssueListMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.PaginatedMessage,
                org.gsc.api.GrpcAPI.AssetIssueList>(
                  this, METHODID_GET_PAGINATED_ASSET_ISSUE_LIST)))
          .addMethod(
            getTotalTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.api.GrpcAPI.NumberMessage>(
                  this, METHODID_TOTAL_TRANSACTION)))
          .addMethod(
            getGetNextMaintenanceTimeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.api.GrpcAPI.NumberMessage>(
                  this, METHODID_GET_NEXT_MAINTENANCE_TIME)))
          .addMethod(
            getGetTransactionSignMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.protos.Protocol.TransactionSign,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_GET_TRANSACTION_SIGN)))

          .addMethod(
            getCreateAddressMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.api.GrpcAPI.BytesMessage>(
                  this, METHODID_CREATE_ADDRESS)))
          .addMethod(
            getEasyTransferMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EasyTransferMessage,
                org.gsc.api.GrpcAPI.EasyTransferResponse>(
                  this, METHODID_EASY_TRANSFER)))

          .addMethod(
            getGenerateAddressMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.api.GrpcAPI.AddressPrKeyPairMessage>(
                  this, METHODID_GENERATE_ADDRESS)))
          .addMethod(
            getGetTransactionInfoByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Protocol.TransactionInfo>(
                  this, METHODID_GET_TRANSACTION_INFO_BY_ID)))
          .build();
    }
  }

  /**
   */
  public static final class WalletStub extends io.grpc.stub.AbstractStub<WalletStub> {
    private WalletStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected WalletStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletStub(channel, callOptions);
    }

    /**
     */
    public void getAccount(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Account> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAccountById(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Account> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use CreateTransaction2 instead of this function.
     * </pre>
     */
    public void createTransaction(org.gsc.protos.Contract.TransferContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateTransaction.
     * </pre>

    public void createTransaction2(org.gsc.protos.Contract.TransferContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateTransaction2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void broadcastTransaction(org.gsc.protos.Protocol.Transaction request,
        io.grpc.stub.StreamObserver<GrpcAPI.Return> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBroadcastTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateAccount2 instead of this function.
     * </pre>
     */
    public void updateAccount(org.gsc.protos.Contract.AccountUpdateContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void setAccountId(org.gsc.protos.Contract.SetAccountIdContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetAccountIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAccount.
     * </pre>

    public void updateAccount2(org.gsc.protos.Contract.AccountUpdateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAccount2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use VoteWitnessAccount2 instead of this function.
     * </pre>
     */
    public void voteWitnessAccount(org.gsc.protos.Contract.VoteWitnessContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getVoteWitnessAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *modify the consume_user_resource_percent
     * </pre>

    public void updateSetting(org.gsc.protos.Contract.UpdateSettingContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateSettingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of VoteWitnessAccount.
     * </pre>

    public void voteWitnessAccount2(org.gsc.protos.Contract.VoteWitnessContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getVoteWitnessAccount2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use CreateAssetIssue2 instead of this function.
     * </pre>
     */
    public void createAssetIssue(org.gsc.protos.Contract.AssetIssueContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAssetIssueMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateAssetIssue.
     * </pre>

    public void createAssetIssue2(org.gsc.protos.Contract.AssetIssueContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAssetIssue2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateWitness2 instead of this function.
     * </pre>
     */
    public void updateWitness(org.gsc.protos.Contract.WitnessUpdateContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateWitnessMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateWitness.
     * </pre>

    public void updateWitness2(org.gsc.protos.Contract.WitnessUpdateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateWitness2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use CreateAccount2 instead of this function.
     * </pre>
     */
    public void createAccount(org.gsc.protos.Contract.AccountCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateAccount.
     * </pre>

    public void createAccount2(org.gsc.protos.Contract.AccountCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAccount2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use CreateWitness2 instead of this function.
     * </pre>
     */
    public void createWitness(org.gsc.protos.Contract.WitnessCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateWitnessMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of CreateWitness.
     * </pre>

    public void createWitness2(org.gsc.protos.Contract.WitnessCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateWitness2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use TransferAsset2 instead of this function.
     * </pre>
     */
    public void transferAsset(org.gsc.protos.Contract.TransferAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransferAssetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of TransferAsset.
     * </pre>

    public void transferAsset2(org.gsc.protos.Contract.TransferAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransferAsset2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use ParticipateAssetIssue2 instead of this function.
     * </pre>
     */
    public void participateAssetIssue(org.gsc.protos.Contract.ParticipateAssetIssueContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getParticipateAssetIssueMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of ParticipateAssetIssue.
     * </pre>

    public void participateAssetIssue2(org.gsc.protos.Contract.ParticipateAssetIssueContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getParticipateAssetIssue2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use FreezeBalance2 instead of this function.
     * </pre>
     */
    public void freezeBalance(org.gsc.protos.Contract.FreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFreezeBalanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of FreezeBalance.
     * </pre>

    public void freezeBalance2(org.gsc.protos.Contract.FreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFreezeBalance2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UnfreezeBalance2 instead of this function.
     * </pre>
     */
    public void unfreezeBalance(org.gsc.protos.Contract.UnfreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnfreezeBalanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeBalance.
     * </pre>

    public void unfreezeBalance2(org.gsc.protos.Contract.UnfreezeBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnfreezeBalance2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UnfreezeAsset2 instead of this function.
     * </pre>
     */
    public void unfreezeAsset(org.gsc.protos.Contract.UnfreezeAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnfreezeAssetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeAsset.
     * </pre>

    public void unfreezeAsset2(org.gsc.protos.Contract.UnfreezeAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnfreezeAsset2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use WithdrawBalance2 instead of this function.
     * </pre>
     */
    public void withdrawBalance(org.gsc.protos.Contract.WithdrawBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWithdrawBalanceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of WithdrawBalance.
     * </pre>

    public void withdrawBalance2(org.gsc.protos.Contract.WithdrawBalanceContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWithdrawBalance2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use UpdateAsset2 instead of this function.
     * </pre>
     */
    public void updateAsset(org.gsc.protos.Contract.UpdateAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAssetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAsset.
     * </pre>

    public void updateAsset2(org.gsc.protos.Contract.UpdateAssetContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateAsset2Method(), getCallOptions()), request, responseObserver);
    }

    /**

    public void proposalCreate(org.gsc.protos.Contract.ProposalCreateContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getProposalCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void proposalApprove(org.gsc.protos.Contract.ProposalApproveContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getProposalApproveMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void proposalDelete(org.gsc.protos.Contract.ProposalDeleteContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getProposalDeleteMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void buyStorage(org.gsc.protos.Contract.BuyStorageContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBuyStorageMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void buyStorageBytes(org.gsc.protos.Contract.BuyStorageBytesContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBuyStorageBytesMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void sellStorage(org.gsc.protos.Contract.SellStorageContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSellStorageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listNodes(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.NodeList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListNodesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueByAccount(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueByAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAccountNet(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<GrpcAPI.AccountNetMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountNetMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void getAccountResource(org.gsc.protos.Protocol.Account request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AccountResourceMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueByName(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueByNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use GetNowBlock2 instead of this function.
     * </pre>
     */
    public void getNowBlock(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetNowBlockMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetNowBlock.
     * </pre>

    public void getNowBlock2(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetNowBlock2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByNum2 instead of this function.
     * </pre>
     */
    public void getBlockByNum(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByNumMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByNum.
     * </pre>

    public void getBlockByNum2(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByNum2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionCountByBlockNum(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.NumberMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionCountByBlockNumMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getBlockById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByLimitNext2 instead of this function.
     * </pre>
     */
    public void getBlockByLimitNext(org.gsc.api.GrpcAPI.BlockLimit request,
        io.grpc.stub.StreamObserver<GrpcAPI.BlockList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByLimitNextMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLimitNext.
     * </pre>

    public void getBlockByLimitNext2(org.gsc.api.GrpcAPI.BlockLimit request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockListExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByLimitNext2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Please use GetBlockByLatestNum2 instead of this function.
     * </pre>
     */
    public void getBlockByLatestNum(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.BlockList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByLatestNumMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLatestNum.
     * </pre>

    public void getBlockByLatestNum2(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockListExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByLatestNum2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void deployContract(org.gsc.protos.Contract.CreateSmartContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeployContractMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void getContract(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.SmartContract> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetContractMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void triggerContract(org.gsc.protos.Contract.TriggerSmartContract request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTriggerContractMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listWitnesses(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.WitnessList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListWitnessesMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void listProposals(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.ProposalList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListProposalsMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void getProposalById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Proposal> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetProposalByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**

    public void getChainParameters(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.ChainParameters> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetChainParametersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueList(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getPaginatedAssetIssueList(org.gsc.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetPaginatedAssetIssueListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void totalTransaction(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.NumberMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTotalTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getNextMaintenanceTime(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.NumberMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetNextMaintenanceTimeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Please use GetTransactionSign2 instead of this function.
     * </pre>
     */
    public void getTransactionSign(org.gsc.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionSignMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Use this function instead of GetTransactionSign.
     * </pre>

    public void getTransactionSign2(org.gsc.protos.Protocol.TransactionSign request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.TransactionExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionSign2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void createAddress(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.BytesMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateAddressMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void easyTransfer(org.gsc.api.GrpcAPI.EasyTransferMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEasyTransferMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>

    public void easyTransferByPrivate(org.gsc.api.GrpcAPI.EasyTransferByPrivateMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.EasyTransferResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEasyTransferByPrivateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void generateAddress(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<GrpcAPI.AddressPrKeyPairMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGenerateAddressMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionInfoById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.TransactionInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionInfoByIdMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class WalletBlockingStub extends io.grpc.stub.AbstractStub<WalletBlockingStub> {
    private WalletBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected WalletBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.gsc.protos.Protocol.Account getAccount(org.gsc.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Protocol.Account getAccountById(org.gsc.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountByIdMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use CreateTransaction2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction createTransaction(org.gsc.protos.Contract.TransferContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateTransactionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateTransaction.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention createTransaction2(org.gsc.protos.Contract.TransferContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateTransaction2Method(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.Return broadcastTransaction(org.gsc.protos.Protocol.Transaction request) {
      return blockingUnaryCall(
          getChannel(), getBroadcastTransactionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UpdateAccount2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction updateAccount(org.gsc.protos.Contract.AccountUpdateContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAccountMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.protos.Protocol.Transaction setAccountId(org.gsc.protos.Contract.SetAccountIdContract request) {
      return blockingUnaryCall(
          getChannel(), getSetAccountIdMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAccount.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention updateAccount2(org.gsc.protos.Contract.AccountUpdateContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAccount2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use VoteWitnessAccount2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction voteWitnessAccount(org.gsc.protos.Contract.VoteWitnessContract request) {
      return blockingUnaryCall(
          getChannel(), getVoteWitnessAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *modify the consume_user_resource_percent
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention updateSetting(org.gsc.protos.Contract.UpdateSettingContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateSettingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of VoteWitnessAccount.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention voteWitnessAccount2(org.gsc.protos.Contract.VoteWitnessContract request) {
      return blockingUnaryCall(
          getChannel(), getVoteWitnessAccount2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use CreateAssetIssue2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction createAssetIssue(org.gsc.protos.Contract.AssetIssueContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateAssetIssueMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateAssetIssue.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention createAssetIssue2(org.gsc.protos.Contract.AssetIssueContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateAssetIssue2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UpdateWitness2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction updateWitness(org.gsc.protos.Contract.WitnessUpdateContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateWitnessMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateWitness.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention updateWitness2(org.gsc.protos.Contract.WitnessUpdateContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateWitness2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use CreateAccount2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction createAccount(org.gsc.protos.Contract.AccountCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateAccount.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention createAccount2(org.gsc.protos.Contract.AccountCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateAccount2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use CreateWitness2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction createWitness(org.gsc.protos.Contract.WitnessCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateWitnessMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateWitness.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention createWitness2(org.gsc.protos.Contract.WitnessCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getCreateWitness2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use TransferAsset2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction transferAsset(org.gsc.protos.Contract.TransferAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getTransferAssetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of TransferAsset.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention transferAsset2(org.gsc.protos.Contract.TransferAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getTransferAsset2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use ParticipateAssetIssue2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction participateAssetIssue(org.gsc.protos.Contract.ParticipateAssetIssueContract request) {
      return blockingUnaryCall(
          getChannel(), getParticipateAssetIssueMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of ParticipateAssetIssue.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention participateAssetIssue2(org.gsc.protos.Contract.ParticipateAssetIssueContract request) {
      return blockingUnaryCall(
          getChannel(), getParticipateAssetIssue2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use FreezeBalance2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction freezeBalance(org.gsc.protos.Contract.FreezeBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getFreezeBalanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of FreezeBalance.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention freezeBalance2(org.gsc.protos.Contract.FreezeBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getFreezeBalance2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UnfreezeBalance2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction unfreezeBalance(org.gsc.protos.Contract.UnfreezeBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getUnfreezeBalanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeBalance.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention unfreezeBalance2(org.gsc.protos.Contract.UnfreezeBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getUnfreezeBalance2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UnfreezeAsset2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction unfreezeAsset(org.gsc.protos.Contract.UnfreezeAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getUnfreezeAssetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeAsset.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention unfreezeAsset2(org.gsc.protos.Contract.UnfreezeAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getUnfreezeAsset2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use WithdrawBalance2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction withdrawBalance(org.gsc.protos.Contract.WithdrawBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getWithdrawBalanceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of WithdrawBalance.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention withdrawBalance2(org.gsc.protos.Contract.WithdrawBalanceContract request) {
      return blockingUnaryCall(
          getChannel(), getWithdrawBalance2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use UpdateAsset2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction updateAsset(org.gsc.protos.Contract.UpdateAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAssetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAsset.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention updateAsset2(org.gsc.protos.Contract.UpdateAssetContract request) {
      return blockingUnaryCall(
          getChannel(), getUpdateAsset2Method(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.TransactionExtention proposalCreate(org.gsc.protos.Contract.ProposalCreateContract request) {
      return blockingUnaryCall(
          getChannel(), getProposalCreateMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.TransactionExtention proposalApprove(org.gsc.protos.Contract.ProposalApproveContract request) {
      return blockingUnaryCall(
          getChannel(), getProposalApproveMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.TransactionExtention proposalDelete(org.gsc.protos.Contract.ProposalDeleteContract request) {
      return blockingUnaryCall(
          getChannel(), getProposalDeleteMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.TransactionExtention buyStorage(org.gsc.protos.Contract.BuyStorageContract request) {
      return blockingUnaryCall(
          getChannel(), getBuyStorageMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.TransactionExtention buyStorageBytes(org.gsc.protos.Contract.BuyStorageBytesContract request) {
      return blockingUnaryCall(
          getChannel(), getBuyStorageBytesMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.TransactionExtention sellStorage(org.gsc.protos.Contract.SellStorageContract request) {
      return blockingUnaryCall(
          getChannel(), getSellStorageMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.NodeList listNodes(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListNodesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.AssetIssueList getAssetIssueByAccount(org.gsc.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueByAccountMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.AccountNetMessage getAccountNet(org.gsc.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountNetMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.AccountResourceMessage getAccountResource(org.gsc.protos.Protocol.Account request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountResourceMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Contract.AssetIssueContract getAssetIssueByName(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueByNameMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use GetNowBlock2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Block getNowBlock(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetNowBlockMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of GetNowBlock.
     * </pre>

    public org.gsc.api.GrpcAPI.BlockExtention getNowBlock2(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetNowBlock2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use GetBlockByNum2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Block getBlockByNum(org.gsc.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByNumMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByNum.
     * </pre>

    public org.gsc.api.GrpcAPI.BlockExtention getBlockByNum2(org.gsc.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByNum2Method(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.NumberMessage getTransactionCountByBlockNum(org.gsc.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionCountByBlockNumMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Protocol.Block getBlockById(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByIdMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use GetBlockByLimitNext2 instead of this function.
     * </pre>
     */
    public org.gsc.api.GrpcAPI.BlockList getBlockByLimitNext(org.gsc.api.GrpcAPI.BlockLimit request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByLimitNextMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLimitNext.
     * </pre>

    public org.gsc.api.GrpcAPI.BlockListExtention getBlockByLimitNext2(org.gsc.api.GrpcAPI.BlockLimit request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByLimitNext2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Please use GetBlockByLatestNum2 instead of this function.
     * </pre>
     */
    public org.gsc.api.GrpcAPI.BlockList getBlockByLatestNum(org.gsc.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByLatestNumMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLatestNum.
     * </pre>

    public org.gsc.api.GrpcAPI.BlockListExtention getBlockByLatestNum2(org.gsc.api.GrpcAPI.NumberMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByLatestNum2Method(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Protocol.Transaction getTransactionById(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionByIdMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.TransactionExtention deployContract(org.gsc.protos.Contract.CreateSmartContract request) {
      return blockingUnaryCall(
          getChannel(), getDeployContractMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.protos.Protocol.SmartContract getContract(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetContractMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.TransactionExtention triggerContract(org.gsc.protos.Contract.TriggerSmartContract request) {
      return blockingUnaryCall(
          getChannel(), getTriggerContractMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.WitnessList listWitnesses(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListWitnessesMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.api.GrpcAPI.ProposalList listProposals(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListProposalsMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.protos.Protocol.Proposal getProposalById(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetProposalByIdMethod(), getCallOptions(), request);
    }

    /**

    public org.gsc.protos.Protocol.ChainParameters getChainParameters(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetChainParametersMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.AssetIssueList getAssetIssueList(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueListMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.AssetIssueList getPaginatedAssetIssueList(org.gsc.api.GrpcAPI.PaginatedMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetPaginatedAssetIssueListMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.NumberMessage totalTransaction(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getTotalTransactionMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.NumberMessage getNextMaintenanceTime(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetNextMaintenanceTimeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Please use GetTransactionSign2 instead of this function.
     * </pre>
     */
    public org.gsc.protos.Protocol.Transaction getTransactionSign(org.gsc.protos.Protocol.TransactionSign request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionSignMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Use this function instead of GetTransactionSign.
     * </pre>

    public org.gsc.api.GrpcAPI.TransactionExtention getTransactionSign2(org.gsc.protos.Protocol.TransactionSign request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionSign2Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public org.gsc.api.GrpcAPI.BytesMessage createAddress(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getCreateAddressMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public org.gsc.api.GrpcAPI.EasyTransferResponse easyTransfer(org.gsc.api.GrpcAPI.EasyTransferMessage request) {
      return blockingUnaryCall(
          getChannel(), getEasyTransferMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>

    public org.gsc.api.GrpcAPI.EasyTransferResponse easyTransferByPrivate(org.gsc.api.GrpcAPI.EasyTransferByPrivateMessage request) {
      return blockingUnaryCall(
          getChannel(), getEasyTransferByPrivateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public org.gsc.api.GrpcAPI.AddressPrKeyPairMessage generateAddress(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getGenerateAddressMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Protocol.TransactionInfo getTransactionInfoById(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionInfoByIdMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class WalletFutureStub extends io.grpc.stub.AbstractStub<WalletFutureStub> {
    private WalletFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected WalletFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Account> getAccount(
        org.gsc.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Account> getAccountById(
        org.gsc.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountByIdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use CreateTransaction2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> createTransaction(
        org.gsc.protos.Contract.TransferContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateTransactionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateTransaction.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> createTransaction2(
        org.gsc.protos.Contract.TransferContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateTransaction2Method(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.Return> broadcastTransaction(
        org.gsc.protos.Protocol.Transaction request) {
      return futureUnaryCall(
          getChannel().newCall(getBroadcastTransactionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UpdateAccount2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> updateAccount(
        org.gsc.protos.Contract.AccountUpdateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAccountMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> setAccountId(
        org.gsc.protos.Contract.SetAccountIdContract request) {
      return futureUnaryCall(
          getChannel().newCall(getSetAccountIdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAccount.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> updateAccount2(
        org.gsc.protos.Contract.AccountUpdateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAccount2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use VoteWitnessAccount2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> voteWitnessAccount(
        org.gsc.protos.Contract.VoteWitnessContract request) {
      return futureUnaryCall(
          getChannel().newCall(getVoteWitnessAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *modify the consume_user_resource_percent
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> updateSetting(
        org.gsc.protos.Contract.UpdateSettingContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateSettingMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of VoteWitnessAccount.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> voteWitnessAccount2(
        org.gsc.protos.Contract.VoteWitnessContract request) {
      return futureUnaryCall(
          getChannel().newCall(getVoteWitnessAccount2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use CreateAssetIssue2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> createAssetIssue(
        org.gsc.protos.Contract.AssetIssueContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAssetIssueMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateAssetIssue.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> createAssetIssue2(
        org.gsc.protos.Contract.AssetIssueContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAssetIssue2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UpdateWitness2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> updateWitness(
        org.gsc.protos.Contract.WitnessUpdateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateWitnessMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateWitness.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> updateWitness2(
        org.gsc.protos.Contract.WitnessUpdateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateWitness2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use CreateAccount2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> createAccount(
        org.gsc.protos.Contract.AccountCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateAccount.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> createAccount2(
        org.gsc.protos.Contract.AccountCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAccount2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use CreateWitness2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> createWitness(
        org.gsc.protos.Contract.WitnessCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateWitnessMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of CreateWitness.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> createWitness2(
        org.gsc.protos.Contract.WitnessCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateWitness2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use TransferAsset2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> transferAsset(
        org.gsc.protos.Contract.TransferAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getTransferAssetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of TransferAsset.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> transferAsset2(
        org.gsc.protos.Contract.TransferAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getTransferAsset2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use ParticipateAssetIssue2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> participateAssetIssue(
        org.gsc.protos.Contract.ParticipateAssetIssueContract request) {
      return futureUnaryCall(
          getChannel().newCall(getParticipateAssetIssueMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of ParticipateAssetIssue.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> participateAssetIssue2(
        org.gsc.protos.Contract.ParticipateAssetIssueContract request) {
      return futureUnaryCall(
          getChannel().newCall(getParticipateAssetIssue2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use FreezeBalance2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> freezeBalance(
        org.gsc.protos.Contract.FreezeBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getFreezeBalanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of FreezeBalance.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> freezeBalance2(
        org.gsc.protos.Contract.FreezeBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getFreezeBalance2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UnfreezeBalance2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> unfreezeBalance(
        org.gsc.protos.Contract.UnfreezeBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUnfreezeBalanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeBalance.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> unfreezeBalance2(
        org.gsc.protos.Contract.UnfreezeBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUnfreezeBalance2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UnfreezeAsset2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> unfreezeAsset(
        org.gsc.protos.Contract.UnfreezeAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUnfreezeAssetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UnfreezeAsset.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> unfreezeAsset2(
        org.gsc.protos.Contract.UnfreezeAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUnfreezeAsset2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use WithdrawBalance2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> withdrawBalance(
        org.gsc.protos.Contract.WithdrawBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getWithdrawBalanceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of WithdrawBalance.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> withdrawBalance2(
        org.gsc.protos.Contract.WithdrawBalanceContract request) {
      return futureUnaryCall(
          getChannel().newCall(getWithdrawBalance2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use UpdateAsset2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> updateAsset(
        org.gsc.protos.Contract.UpdateAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAssetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of UpdateAsset.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> updateAsset2(
        org.gsc.protos.Contract.UpdateAssetContract request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateAsset2Method(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> proposalCreate(
        org.gsc.protos.Contract.ProposalCreateContract request) {
      return futureUnaryCall(
          getChannel().newCall(getProposalCreateMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> proposalApprove(
        org.gsc.protos.Contract.ProposalApproveContract request) {
      return futureUnaryCall(
          getChannel().newCall(getProposalApproveMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> proposalDelete(
        org.gsc.protos.Contract.ProposalDeleteContract request) {
      return futureUnaryCall(
          getChannel().newCall(getProposalDeleteMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> buyStorage(
        org.gsc.protos.Contract.BuyStorageContract request) {
      return futureUnaryCall(
          getChannel().newCall(getBuyStorageMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> buyStorageBytes(
        org.gsc.protos.Contract.BuyStorageBytesContract request) {
      return futureUnaryCall(
          getChannel().newCall(getBuyStorageBytesMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> sellStorage(
        org.gsc.protos.Contract.SellStorageContract request) {
      return futureUnaryCall(
          getChannel().newCall(getSellStorageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.NodeList> listNodes(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getListNodesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.AssetIssueList> getAssetIssueByAccount(
        org.gsc.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueByAccountMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.AccountNetMessage> getAccountNet(
        org.gsc.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountNetMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.AccountResourceMessage> getAccountResource(
        org.gsc.protos.Protocol.Account request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountResourceMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Contract.AssetIssueContract> getAssetIssueByName(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueByNameMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use GetNowBlock2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Block> getNowBlock(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetNowBlockMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of GetNowBlock.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.BlockExtention> getNowBlock2(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetNowBlock2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use GetBlockByNum2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Block> getBlockByNum(
        org.gsc.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByNumMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByNum.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.BlockExtention> getBlockByNum2(
        org.gsc.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByNum2Method(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.NumberMessage> getTransactionCountByBlockNum(
        org.gsc.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionCountByBlockNumMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Block> getBlockById(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByIdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use GetBlockByLimitNext2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.BlockList> getBlockByLimitNext(
        org.gsc.api.GrpcAPI.BlockLimit request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByLimitNextMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLimitNext.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.BlockListExtention> getBlockByLimitNext2(
        org.gsc.api.GrpcAPI.BlockLimit request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByLimitNext2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Please use GetBlockByLatestNum2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.BlockList> getBlockByLatestNum(
        org.gsc.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByLatestNumMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Use this function instead of GetBlockByLatestNum.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.BlockListExtention> getBlockByLatestNum2(
        org.gsc.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByLatestNum2Method(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> getTransactionById(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionByIdMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> deployContract(
        org.gsc.protos.Contract.CreateSmartContract request) {
      return futureUnaryCall(
          getChannel().newCall(getDeployContractMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.SmartContract> getContract(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetContractMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> triggerContract(
        org.gsc.protos.Contract.TriggerSmartContract request) {
      return futureUnaryCall(
          getChannel().newCall(getTriggerContractMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.WitnessList> listWitnesses(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getListWitnessesMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.ProposalList> listProposals(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getListProposalsMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Proposal> getProposalById(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetProposalByIdMethod(), getCallOptions()), request);
    }

    /**

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.ChainParameters> getChainParameters(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetChainParametersMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.AssetIssueList> getAssetIssueList(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.AssetIssueList> getPaginatedAssetIssueList(
        org.gsc.api.GrpcAPI.PaginatedMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetPaginatedAssetIssueListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.NumberMessage> totalTransaction(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getTotalTransactionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.NumberMessage> getNextMaintenanceTime(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetNextMaintenanceTimeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Please use GetTransactionSign2 instead of this function.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> getTransactionSign(
        org.gsc.protos.Protocol.TransactionSign request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionSignMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     *Use this function instead of GetTransactionSign.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.TransactionExtention> getTransactionSign2(
        org.gsc.protos.Protocol.TransactionSign request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionSign2Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.BytesMessage> createAddress(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateAddressMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.EasyTransferResponse> easyTransfer(
        org.gsc.api.GrpcAPI.EasyTransferMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getEasyTransferMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>

    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.EasyTransferResponse> easyTransferByPrivate(
        org.gsc.api.GrpcAPI.EasyTransferByPrivateMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getEasyTransferByPrivateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<GrpcAPI.AddressPrKeyPairMessage> generateAddress(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGenerateAddressMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.TransactionInfo> getTransactionInfoById(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionInfoByIdMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_ACCOUNT = 0;
  private static final int METHODID_GET_ACCOUNT_BY_ID = 1;
  private static final int METHODID_CREATE_TRANSACTION = 2;
  private static final int METHODID_CREATE_TRANSACTION2 = 3;
  private static final int METHODID_BROADCAST_TRANSACTION = 4;
  private static final int METHODID_UPDATE_ACCOUNT = 5;
  private static final int METHODID_SET_ACCOUNT_ID = 6;
  private static final int METHODID_UPDATE_ACCOUNT2 = 7;
  private static final int METHODID_VOTE_WITNESS_ACCOUNT = 8;
  private static final int METHODID_UPDATE_SETTING = 9;
  private static final int METHODID_VOTE_WITNESS_ACCOUNT2 = 10;
  private static final int METHODID_CREATE_ASSET_ISSUE = 11;
  private static final int METHODID_CREATE_ASSET_ISSUE2 = 12;
  private static final int METHODID_UPDATE_WITNESS = 13;
  private static final int METHODID_UPDATE_WITNESS2 = 14;
  private static final int METHODID_CREATE_ACCOUNT = 15;
  private static final int METHODID_CREATE_ACCOUNT2 = 16;
  private static final int METHODID_CREATE_WITNESS = 17;
  private static final int METHODID_CREATE_WITNESS2 = 18;
  private static final int METHODID_TRANSFER_ASSET = 19;
  private static final int METHODID_TRANSFER_ASSET2 = 20;
  private static final int METHODID_PARTICIPATE_ASSET_ISSUE = 21;
  private static final int METHODID_PARTICIPATE_ASSET_ISSUE2 = 22;
  private static final int METHODID_FREEZE_BALANCE = 23;
  private static final int METHODID_FREEZE_BALANCE2 = 24;
  private static final int METHODID_UNFREEZE_BALANCE = 25;
  private static final int METHODID_UNFREEZE_BALANCE2 = 26;
  private static final int METHODID_UNFREEZE_ASSET = 27;
  private static final int METHODID_UNFREEZE_ASSET2 = 28;
  private static final int METHODID_WITHDRAW_BALANCE = 29;
  private static final int METHODID_WITHDRAW_BALANCE2 = 30;
  private static final int METHODID_UPDATE_ASSET = 31;
  private static final int METHODID_UPDATE_ASSET2 = 32;
  private static final int METHODID_PROPOSAL_CREATE = 33;
  private static final int METHODID_PROPOSAL_APPROVE = 34;
  private static final int METHODID_PROPOSAL_DELETE = 35;
  private static final int METHODID_BUY_STORAGE = 36;
  private static final int METHODID_BUY_STORAGE_BYTES = 37;
  private static final int METHODID_SELL_STORAGE = 38;
  private static final int METHODID_LIST_NODES = 39;
  private static final int METHODID_GET_ASSET_ISSUE_BY_ACCOUNT = 40;
  private static final int METHODID_GET_ACCOUNT_NET = 41;
  private static final int METHODID_GET_ACCOUNT_RESOURCE = 42;
  private static final int METHODID_GET_ASSET_ISSUE_BY_NAME = 43;
  private static final int METHODID_GET_NOW_BLOCK = 44;
  private static final int METHODID_GET_NOW_BLOCK2 = 45;
  private static final int METHODID_GET_BLOCK_BY_NUM = 46;
  private static final int METHODID_GET_BLOCK_BY_NUM2 = 47;
  private static final int METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM = 48;
  private static final int METHODID_GET_BLOCK_BY_ID = 49;
  private static final int METHODID_GET_BLOCK_BY_LIMIT_NEXT = 50;
  private static final int METHODID_GET_BLOCK_BY_LIMIT_NEXT2 = 51;
  private static final int METHODID_GET_BLOCK_BY_LATEST_NUM = 52;
  private static final int METHODID_GET_BLOCK_BY_LATEST_NUM2 = 53;
  private static final int METHODID_GET_TRANSACTION_BY_ID = 54;
  private static final int METHODID_DEPLOY_CONTRACT = 55;
  private static final int METHODID_GET_CONTRACT = 56;
  private static final int METHODID_TRIGGER_CONTRACT = 57;
  private static final int METHODID_LIST_WITNESSES = 58;
  private static final int METHODID_LIST_PROPOSALS = 59;
  private static final int METHODID_GET_PROPOSAL_BY_ID = 60;
  private static final int METHODID_GET_CHAIN_PARAMETERS = 61;
  private static final int METHODID_GET_ASSET_ISSUE_LIST = 62;
  private static final int METHODID_GET_PAGINATED_ASSET_ISSUE_LIST = 63;
  private static final int METHODID_TOTAL_TRANSACTION = 64;
  private static final int METHODID_GET_NEXT_MAINTENANCE_TIME = 65;
  private static final int METHODID_GET_TRANSACTION_SIGN = 66;
  private static final int METHODID_GET_TRANSACTION_SIGN2 = 67;
  private static final int METHODID_CREATE_ADDRESS = 68;
  private static final int METHODID_EASY_TRANSFER = 69;
  private static final int METHODID_EASY_TRANSFER_BY_PRIVATE = 70;
  private static final int METHODID_GENERATE_ADDRESS = 71;
  private static final int METHODID_GET_TRANSACTION_INFO_BY_ID = 72;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final WalletImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(WalletImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_ACCOUNT:
          serviceImpl.getAccount((org.gsc.protos.Protocol.Account) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Account>) responseObserver);
          break;
        case METHODID_GET_ACCOUNT_BY_ID:
          serviceImpl.getAccountById((org.gsc.protos.Protocol.Account) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Account>) responseObserver);
          break;
        case METHODID_CREATE_TRANSACTION:
          serviceImpl.createTransaction((org.gsc.protos.Contract.TransferContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_BROADCAST_TRANSACTION:
          serviceImpl.broadcastTransaction((org.gsc.protos.Protocol.Transaction) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.Return>) responseObserver);
          break;
        case METHODID_UPDATE_ACCOUNT:
          serviceImpl.updateAccount((org.gsc.protos.Contract.AccountUpdateContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;


        case METHODID_VOTE_WITNESS_ACCOUNT:
          serviceImpl.voteWitnessAccount((org.gsc.protos.Contract.VoteWitnessContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;


        case METHODID_CREATE_ASSET_ISSUE:
          serviceImpl.createAssetIssue((org.gsc.protos.Contract.AssetIssueContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_UPDATE_WITNESS:
          serviceImpl.updateWitness((org.gsc.protos.Contract.WitnessUpdateContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_CREATE_ACCOUNT:
          serviceImpl.createAccount((org.gsc.protos.Contract.AccountCreateContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_CREATE_WITNESS:
          serviceImpl.createWitness((org.gsc.protos.Contract.WitnessCreateContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_TRANSFER_ASSET:
          serviceImpl.transferAsset((org.gsc.protos.Contract.TransferAssetContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_PARTICIPATE_ASSET_ISSUE:
          serviceImpl.participateAssetIssue((org.gsc.protos.Contract.ParticipateAssetIssueContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_FREEZE_BALANCE:
          serviceImpl.freezeBalance((org.gsc.protos.Contract.FreezeBalanceContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_UNFREEZE_BALANCE:
          serviceImpl.unfreezeBalance((org.gsc.protos.Contract.UnfreezeBalanceContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_UNFREEZE_ASSET:
          serviceImpl.unfreezeAsset((org.gsc.protos.Contract.UnfreezeAssetContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_WITHDRAW_BALANCE:
          serviceImpl.withdrawBalance((org.gsc.protos.Contract.WithdrawBalanceContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_UPDATE_ASSET:
          serviceImpl.updateAsset((org.gsc.protos.Contract.UpdateAssetContract) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;



        case METHODID_LIST_NODES:
          serviceImpl.listNodes((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.NodeList>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_BY_ACCOUNT:
          serviceImpl.getAssetIssueByAccount((org.gsc.protos.Protocol.Account) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_ACCOUNT_NET:
          serviceImpl.getAccountNet((org.gsc.protos.Protocol.Account) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.AccountNetMessage>) responseObserver);
          break;

        case METHODID_GET_ASSET_ISSUE_BY_NAME:
          serviceImpl.getAssetIssueByName((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Contract.AssetIssueContract>) responseObserver);
          break;
        case METHODID_GET_NOW_BLOCK:
          serviceImpl.getNowBlock((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block>) responseObserver);
          break;

        case METHODID_GET_BLOCK_BY_NUM:
          serviceImpl.getBlockByNum((org.gsc.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block>) responseObserver);
          break;

        case METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM:
          serviceImpl.getTransactionCountByBlockNum((org.gsc.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.NumberMessage>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_ID:
          serviceImpl.getBlockById((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_LIMIT_NEXT:
          serviceImpl.getBlockByLimitNext((org.gsc.api.GrpcAPI.BlockLimit) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.BlockList>) responseObserver);
          break;

        case METHODID_GET_BLOCK_BY_LATEST_NUM:
          serviceImpl.getBlockByLatestNum((org.gsc.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.BlockList>) responseObserver);
          break;

        case METHODID_GET_TRANSACTION_BY_ID:
          serviceImpl.getTransactionById((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;


        case METHODID_LIST_WITNESSES:
          serviceImpl.listWitnesses((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.WitnessList>) responseObserver);
          break;

        case METHODID_GET_ASSET_ISSUE_LIST:
          serviceImpl.getAssetIssueList((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_PAGINATED_ASSET_ISSUE_LIST:
          serviceImpl.getPaginatedAssetIssueList((org.gsc.api.GrpcAPI.PaginatedMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_TOTAL_TRANSACTION:
          serviceImpl.totalTransaction((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.NumberMessage>) responseObserver);
          break;
        case METHODID_GET_NEXT_MAINTENANCE_TIME:
          serviceImpl.getNextMaintenanceTime((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.NumberMessage>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_SIGN:
          serviceImpl.getTransactionSign((org.gsc.protos.Protocol.TransactionSign) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;

        case METHODID_CREATE_ADDRESS:
          serviceImpl.createAddress((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.BytesMessage>) responseObserver);
          break;
        case METHODID_EASY_TRANSFER:
          serviceImpl.easyTransfer((org.gsc.api.GrpcAPI.EasyTransferMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.EasyTransferResponse>) responseObserver);
          break;

        case METHODID_GENERATE_ADDRESS:
          serviceImpl.generateAddress((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<GrpcAPI.AddressPrKeyPairMessage>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_INFO_BY_ID:
          serviceImpl.getTransactionInfoById((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.TransactionInfo>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class WalletBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WalletBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.gsc.api.GrpcAPI.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Wallet");
    }
  }

  private static final class WalletFileDescriptorSupplier
      extends WalletBaseDescriptorSupplier {
    WalletFileDescriptorSupplier() {}
  }

  private static final class WalletMethodDescriptorSupplier
      extends WalletBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    WalletMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (WalletGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WalletFileDescriptorSupplier())
              .addMethod(getGetAccountMethod())
              .addMethod(getGetAccountByIdMethod())
              .addMethod(getCreateTransactionMethod())

              .addMethod(getBroadcastTransactionMethod())
              .addMethod(getUpdateAccountMethod())

              .addMethod(getVoteWitnessAccountMethod())

              .addMethod(getCreateAssetIssueMethod())

              .addMethod(getUpdateWitnessMethod())

              .addMethod(getCreateAccountMethod())

              .addMethod(getCreateWitnessMethod())

              .addMethod(getTransferAssetMethod())

              .addMethod(getParticipateAssetIssueMethod())

              .addMethod(getFreezeBalanceMethod())

              .addMethod(getUnfreezeBalanceMethod())

              .addMethod(getUnfreezeAssetMethod())

              .addMethod(getWithdrawBalanceMethod())

              .addMethod(getUpdateAssetMethod())


              .addMethod(getListNodesMethod())
              .addMethod(getGetAssetIssueByAccountMethod())
              .addMethod(getGetAccountNetMethod())

              .addMethod(getGetAssetIssueByNameMethod())
              .addMethod(getGetNowBlockMethod())

              .addMethod(getGetBlockByNumMethod())

              .addMethod(getGetTransactionCountByBlockNumMethod())
              .addMethod(getGetBlockByIdMethod())
              .addMethod(getGetBlockByLimitNextMethod())

              .addMethod(getGetBlockByLatestNumMethod())

              .addMethod(getGetTransactionByIdMethod())

              .addMethod(getListWitnessesMethod())

              .addMethod(getGetAssetIssueListMethod())
              .addMethod(getGetPaginatedAssetIssueListMethod())
              .addMethod(getTotalTransactionMethod())
              .addMethod(getGetNextMaintenanceTimeMethod())
              .addMethod(getGetTransactionSignMethod())

              .addMethod(getCreateAddressMethod())
              .addMethod(getEasyTransferMethod())

              .addMethod(getGenerateAddressMethod())
              .addMethod(getGetTransactionInfoByIdMethod())
              .build();
        }
      }
    }
    return result;
  }
}
