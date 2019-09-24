package org.gsc.api;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.9.0)",
    comments = "Source: api/api.proto")
public final class WalletConfirmedGrpc {

  private WalletConfirmedGrpc() {}

  public static final String SERVICE_NAME = "protocol.WalletConfirmed";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAccountMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> METHOD_GET_ACCOUNT = getGetAccountMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> getGetAccountMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> getGetAccountMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account, org.gsc.protos.Protocol.Account> getGetAccountMethod;
    if ((getGetAccountMethod = WalletConfirmedGrpc.getGetAccountMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetAccountMethod = WalletConfirmedGrpc.getGetAccountMethod) == null) {
          WalletConfirmedGrpc.getGetAccountMethod = getGetAccountMethod = 
              io.grpc.MethodDescriptor.<org.gsc.protos.Protocol.Account, org.gsc.protos.Protocol.Account>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetAccount"))
                  .build();
          }
        }
     }
     return getGetAccountMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAccountByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> METHOD_GET_ACCOUNT_BY_ID = getGetAccountByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> getGetAccountByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account,
      org.gsc.protos.Protocol.Account> getGetAccountByIdMethod() {
    io.grpc.MethodDescriptor<org.gsc.protos.Protocol.Account, org.gsc.protos.Protocol.Account> getGetAccountByIdMethod;
    if ((getGetAccountByIdMethod = WalletConfirmedGrpc.getGetAccountByIdMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetAccountByIdMethod = WalletConfirmedGrpc.getGetAccountByIdMethod) == null) {
          WalletConfirmedGrpc.getGetAccountByIdMethod = getGetAccountByIdMethod = 
              io.grpc.MethodDescriptor.<org.gsc.protos.Protocol.Account, org.gsc.protos.Protocol.Account>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetAccountById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Account.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetAccountById"))
                  .build();
          }
        }
     }
     return getGetAccountByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListWitnessesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.WitnessList> METHOD_LIST_WITNESSES = getListWitnessesMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.WitnessList> getListWitnessesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.WitnessList> getListWitnessesMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.WitnessList> getListWitnessesMethod;
    if ((getListWitnessesMethod = WalletConfirmedGrpc.getListWitnessesMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getListWitnessesMethod = WalletConfirmedGrpc.getListWitnessesMethod) == null) {
          WalletConfirmedGrpc.getListWitnessesMethod = getListWitnessesMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.WitnessList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "ListWitnesses"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.WitnessList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("ListWitnesses"))
                  .build();
          }
        }
     }
     return getListWitnessesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssetIssueListMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.AssetIssueList> METHOD_GET_ASSET_ISSUE_LIST = getGetAssetIssueListMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.AssetIssueList> getGetAssetIssueListMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.AssetIssueList> getGetAssetIssueListMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.AssetIssueList> getGetAssetIssueListMethod;
    if ((getGetAssetIssueListMethod = WalletConfirmedGrpc.getGetAssetIssueListMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetAssetIssueListMethod = WalletConfirmedGrpc.getGetAssetIssueListMethod) == null) {
          WalletConfirmedGrpc.getGetAssetIssueListMethod = getGetAssetIssueListMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetAssetIssueList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetAssetIssueList"))
                  .build();
          }
        }
     }
     return getGetAssetIssueListMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetPaginatedAssetIssueListMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.PaginatedMessage,
      org.gsc.api.GrpcAPI.AssetIssueList> METHOD_GET_PAGINATED_ASSET_ISSUE_LIST = getGetPaginatedAssetIssueListMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.PaginatedMessage,
      org.gsc.api.GrpcAPI.AssetIssueList> getGetPaginatedAssetIssueListMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.PaginatedMessage,
      org.gsc.api.GrpcAPI.AssetIssueList> getGetPaginatedAssetIssueListMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.PaginatedMessage, org.gsc.api.GrpcAPI.AssetIssueList> getGetPaginatedAssetIssueListMethod;
    if ((getGetPaginatedAssetIssueListMethod = WalletConfirmedGrpc.getGetPaginatedAssetIssueListMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetPaginatedAssetIssueListMethod = WalletConfirmedGrpc.getGetPaginatedAssetIssueListMethod) == null) {
          WalletConfirmedGrpc.getGetPaginatedAssetIssueListMethod = getGetPaginatedAssetIssueListMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.PaginatedMessage, org.gsc.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetPaginatedAssetIssueList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.PaginatedMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetPaginatedAssetIssueList"))
                  .build();
          }
        }
     }
     return getGetPaginatedAssetIssueListMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssetIssueByNameMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Contract.AssetIssueContract> METHOD_GET_ASSET_ISSUE_BY_NAME = getGetAssetIssueByNameMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Contract.AssetIssueContract> getGetAssetIssueByNameMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Contract.AssetIssueContract> getGetAssetIssueByNameMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Contract.AssetIssueContract> getGetAssetIssueByNameMethod;
    if ((getGetAssetIssueByNameMethod = WalletConfirmedGrpc.getGetAssetIssueByNameMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetAssetIssueByNameMethod = WalletConfirmedGrpc.getGetAssetIssueByNameMethod) == null) {
          WalletConfirmedGrpc.getGetAssetIssueByNameMethod = getGetAssetIssueByNameMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Contract.AssetIssueContract>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetAssetIssueByName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.AssetIssueContract.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetAssetIssueByName"))
                  .build();
          }
        }
     }
     return getGetAssetIssueByNameMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssetIssueListByNameMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.api.GrpcAPI.AssetIssueList> METHOD_GET_ASSET_ISSUE_LIST_BY_NAME = getGetAssetIssueListByNameMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.api.GrpcAPI.AssetIssueList> getGetAssetIssueListByNameMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.api.GrpcAPI.AssetIssueList> getGetAssetIssueListByNameMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.api.GrpcAPI.AssetIssueList> getGetAssetIssueListByNameMethod;
    if ((getGetAssetIssueListByNameMethod = WalletConfirmedGrpc.getGetAssetIssueListByNameMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetAssetIssueListByNameMethod = WalletConfirmedGrpc.getGetAssetIssueListByNameMethod) == null) {
          WalletConfirmedGrpc.getGetAssetIssueListByNameMethod = getGetAssetIssueListByNameMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.api.GrpcAPI.AssetIssueList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetAssetIssueListByName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.AssetIssueList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetAssetIssueListByName"))
                  .build();
          }
        }
     }
     return getGetAssetIssueListByNameMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetAssetIssueByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Contract.AssetIssueContract> METHOD_GET_ASSET_ISSUE_BY_ID = getGetAssetIssueByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Contract.AssetIssueContract> getGetAssetIssueByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Contract.AssetIssueContract> getGetAssetIssueByIdMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Contract.AssetIssueContract> getGetAssetIssueByIdMethod;
    if ((getGetAssetIssueByIdMethod = WalletConfirmedGrpc.getGetAssetIssueByIdMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetAssetIssueByIdMethod = WalletConfirmedGrpc.getGetAssetIssueByIdMethod) == null) {
          WalletConfirmedGrpc.getGetAssetIssueByIdMethod = getGetAssetIssueByIdMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Contract.AssetIssueContract>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetAssetIssueById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Contract.AssetIssueContract.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetAssetIssueById"))
                  .build();
          }
        }
     }
     return getGetAssetIssueByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetNowBlockMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.protos.Protocol.Block> METHOD_GET_NOW_BLOCK = getGetNowBlockMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.protos.Protocol.Block> getGetNowBlockMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.protos.Protocol.Block> getGetNowBlockMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.protos.Protocol.Block> getGetNowBlockMethod;
    if ((getGetNowBlockMethod = WalletConfirmedGrpc.getGetNowBlockMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetNowBlockMethod = WalletConfirmedGrpc.getGetNowBlockMethod) == null) {
          WalletConfirmedGrpc.getGetNowBlockMethod = getGetNowBlockMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetNowBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetNowBlock"))
                  .build();
          }
        }
     }
     return getGetNowBlockMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetNowBlock2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.BlockExtention> METHOD_GET_NOW_BLOCK2 = getGetNowBlock2Method();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.BlockExtention> getGetNowBlock2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.BlockExtention> getGetNowBlock2Method() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.BlockExtention> getGetNowBlock2Method;
    if ((getGetNowBlock2Method = WalletConfirmedGrpc.getGetNowBlock2Method) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetNowBlock2Method = WalletConfirmedGrpc.getGetNowBlock2Method) == null) {
          WalletConfirmedGrpc.getGetNowBlock2Method = getGetNowBlock2Method = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.BlockExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetNowBlock2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BlockExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetNowBlock2"))
                  .build();
          }
        }
     }
     return getGetNowBlock2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBlockByNumMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage,
      org.gsc.protos.Protocol.Block> METHOD_GET_BLOCK_BY_NUM = getGetBlockByNumMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage,
      org.gsc.protos.Protocol.Block> getGetBlockByNumMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage,
      org.gsc.protos.Protocol.Block> getGetBlockByNumMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage, org.gsc.protos.Protocol.Block> getGetBlockByNumMethod;
    if ((getGetBlockByNumMethod = WalletConfirmedGrpc.getGetBlockByNumMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetBlockByNumMethod = WalletConfirmedGrpc.getGetBlockByNumMethod) == null) {
          WalletConfirmedGrpc.getGetBlockByNumMethod = getGetBlockByNumMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.NumberMessage, org.gsc.protos.Protocol.Block>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetBlockByNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Block.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetBlockByNum"))
                  .build();
          }
        }
     }
     return getGetBlockByNumMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetBlockByNum2Method()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage,
      org.gsc.api.GrpcAPI.BlockExtention> METHOD_GET_BLOCK_BY_NUM2 = getGetBlockByNum2Method();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage,
      org.gsc.api.GrpcAPI.BlockExtention> getGetBlockByNum2Method;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage,
      org.gsc.api.GrpcAPI.BlockExtention> getGetBlockByNum2Method() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage, org.gsc.api.GrpcAPI.BlockExtention> getGetBlockByNum2Method;
    if ((getGetBlockByNum2Method = WalletConfirmedGrpc.getGetBlockByNum2Method) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetBlockByNum2Method = WalletConfirmedGrpc.getGetBlockByNum2Method) == null) {
          WalletConfirmedGrpc.getGetBlockByNum2Method = getGetBlockByNum2Method = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.NumberMessage, org.gsc.api.GrpcAPI.BlockExtention>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetBlockByNum2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BlockExtention.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetBlockByNum2"))
                  .build();
          }
        }
     }
     return getGetBlockByNum2Method;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionCountByBlockNumMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage,
      org.gsc.api.GrpcAPI.NumberMessage> METHOD_GET_TRANSACTION_COUNT_BY_BLOCK_NUM = getGetTransactionCountByBlockNumMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage,
      org.gsc.api.GrpcAPI.NumberMessage> getGetTransactionCountByBlockNumMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage,
      org.gsc.api.GrpcAPI.NumberMessage> getGetTransactionCountByBlockNumMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.NumberMessage, org.gsc.api.GrpcAPI.NumberMessage> getGetTransactionCountByBlockNumMethod;
    if ((getGetTransactionCountByBlockNumMethod = WalletConfirmedGrpc.getGetTransactionCountByBlockNumMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetTransactionCountByBlockNumMethod = WalletConfirmedGrpc.getGetTransactionCountByBlockNumMethod) == null) {
          WalletConfirmedGrpc.getGetTransactionCountByBlockNumMethod = getGetTransactionCountByBlockNumMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.NumberMessage, org.gsc.api.GrpcAPI.NumberMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetTransactionCountByBlockNum"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.NumberMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetTransactionCountByBlockNum"))
                  .build();
          }
        }
     }
     return getGetTransactionCountByBlockNumMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetDelegatedResourceMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.DelegatedResourceMessage,
      org.gsc.api.GrpcAPI.DelegatedResourceList> METHOD_GET_DELEGATED_RESOURCE = getGetDelegatedResourceMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.DelegatedResourceMessage,
      org.gsc.api.GrpcAPI.DelegatedResourceList> getGetDelegatedResourceMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.DelegatedResourceMessage,
      org.gsc.api.GrpcAPI.DelegatedResourceList> getGetDelegatedResourceMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.DelegatedResourceMessage, org.gsc.api.GrpcAPI.DelegatedResourceList> getGetDelegatedResourceMethod;
    if ((getGetDelegatedResourceMethod = WalletConfirmedGrpc.getGetDelegatedResourceMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetDelegatedResourceMethod = WalletConfirmedGrpc.getGetDelegatedResourceMethod) == null) {
          WalletConfirmedGrpc.getGetDelegatedResourceMethod = getGetDelegatedResourceMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.DelegatedResourceMessage, org.gsc.api.GrpcAPI.DelegatedResourceList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetDelegatedResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.DelegatedResourceMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.DelegatedResourceList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetDelegatedResource"))
                  .build();
          }
        }
     }
     return getGetDelegatedResourceMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetDelegatedResourceAccountIndexMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.DelegatedResourceAccountIndex> METHOD_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX = getGetDelegatedResourceAccountIndexMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.DelegatedResourceAccountIndex> getGetDelegatedResourceAccountIndexMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.DelegatedResourceAccountIndex> getGetDelegatedResourceAccountIndexMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.DelegatedResourceAccountIndex> getGetDelegatedResourceAccountIndexMethod;
    if ((getGetDelegatedResourceAccountIndexMethod = WalletConfirmedGrpc.getGetDelegatedResourceAccountIndexMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetDelegatedResourceAccountIndexMethod = WalletConfirmedGrpc.getGetDelegatedResourceAccountIndexMethod) == null) {
          WalletConfirmedGrpc.getGetDelegatedResourceAccountIndexMethod = getGetDelegatedResourceAccountIndexMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.DelegatedResourceAccountIndex>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetDelegatedResourceAccountIndex"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.DelegatedResourceAccountIndex.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetDelegatedResourceAccountIndex"))
                  .build();
          }
        }
     }
     return getGetDelegatedResourceAccountIndexMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetExchangeByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Exchange> METHOD_GET_EXCHANGE_BY_ID = getGetExchangeByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Exchange> getGetExchangeByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Exchange> getGetExchangeByIdMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.Exchange> getGetExchangeByIdMethod;
    if ((getGetExchangeByIdMethod = WalletConfirmedGrpc.getGetExchangeByIdMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetExchangeByIdMethod = WalletConfirmedGrpc.getGetExchangeByIdMethod) == null) {
          WalletConfirmedGrpc.getGetExchangeByIdMethod = getGetExchangeByIdMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.Exchange>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetExchangeById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Exchange.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetExchangeById"))
                  .build();
          }
        }
     }
     return getGetExchangeByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getListExchangesMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.ExchangeList> METHOD_LIST_EXCHANGES = getListExchangesMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.ExchangeList> getListExchangesMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.ExchangeList> getListExchangesMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.ExchangeList> getListExchangesMethod;
    if ((getListExchangesMethod = WalletConfirmedGrpc.getListExchangesMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getListExchangesMethod = WalletConfirmedGrpc.getListExchangesMethod) == null) {
          WalletConfirmedGrpc.getListExchangesMethod = getListExchangesMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.ExchangeList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "ListExchanges"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.ExchangeList.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("ListExchanges"))
                  .build();
          }
        }
     }
     return getListExchangesMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Transaction> METHOD_GET_TRANSACTION_BY_ID = getGetTransactionByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Transaction> getGetTransactionByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.Transaction> getGetTransactionByIdMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.Transaction> getGetTransactionByIdMethod;
    if ((getGetTransactionByIdMethod = WalletConfirmedGrpc.getGetTransactionByIdMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetTransactionByIdMethod = WalletConfirmedGrpc.getGetTransactionByIdMethod) == null) {
          WalletConfirmedGrpc.getGetTransactionByIdMethod = getGetTransactionByIdMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.Transaction>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetTransactionById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.Transaction.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetTransactionById"))
                  .build();
          }
        }
     }
     return getGetTransactionByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGetTransactionInfoByIdMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.TransactionInfo> METHOD_GET_TRANSACTION_INFO_BY_ID = getGetTransactionInfoByIdMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.TransactionInfo> getGetTransactionInfoByIdMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage,
      org.gsc.protos.Protocol.TransactionInfo> getGetTransactionInfoByIdMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.TransactionInfo> getGetTransactionInfoByIdMethod;
    if ((getGetTransactionInfoByIdMethod = WalletConfirmedGrpc.getGetTransactionInfoByIdMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGetTransactionInfoByIdMethod = WalletConfirmedGrpc.getGetTransactionInfoByIdMethod) == null) {
          WalletConfirmedGrpc.getGetTransactionInfoByIdMethod = getGetTransactionInfoByIdMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.BytesMessage, org.gsc.protos.Protocol.TransactionInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GetTransactionInfoById"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.BytesMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.protos.Protocol.TransactionInfo.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GetTransactionInfoById"))
                  .build();
          }
        }
     }
     return getGetTransactionInfoByIdMethod;
  }
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getGenerateAddressMethod()} instead. 
  public static final io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.AddressPrKeyPairMessage> METHOD_GENERATE_ADDRESS = getGenerateAddressMethod();

  private static volatile io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.AddressPrKeyPairMessage> getGenerateAddressMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage,
      org.gsc.api.GrpcAPI.AddressPrKeyPairMessage> getGenerateAddressMethod() {
    io.grpc.MethodDescriptor<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.AddressPrKeyPairMessage> getGenerateAddressMethod;
    if ((getGenerateAddressMethod = WalletConfirmedGrpc.getGenerateAddressMethod) == null) {
      synchronized (WalletConfirmedGrpc.class) {
        if ((getGenerateAddressMethod = WalletConfirmedGrpc.getGenerateAddressMethod) == null) {
          WalletConfirmedGrpc.getGenerateAddressMethod = getGenerateAddressMethod = 
              io.grpc.MethodDescriptor.<org.gsc.api.GrpcAPI.EmptyMessage, org.gsc.api.GrpcAPI.AddressPrKeyPairMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "protocol.WalletConfirmed", "GenerateAddress"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.EmptyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.gsc.api.GrpcAPI.AddressPrKeyPairMessage.getDefaultInstance()))
                  .setSchemaDescriptor(new WalletConfirmedMethodDescriptorSupplier("GenerateAddress"))
                  .build();
          }
        }
     }
     return getGenerateAddressMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WalletConfirmedStub newStub(io.grpc.Channel channel) {
    return new WalletConfirmedStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WalletConfirmedBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new WalletConfirmedBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WalletConfirmedFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new WalletConfirmedFutureStub(channel);
  }

  /**
   */
  public static abstract class WalletConfirmedImplBase implements io.grpc.BindableService {

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
     */
    public void listWitnesses(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.WitnessList> responseObserver) {
      asyncUnimplementedUnaryCall(getListWitnessesMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueList(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueListMethod(), responseObserver);
    }

    /**
     */
    public void getPaginatedAssetIssueList(org.gsc.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetPaginatedAssetIssueListMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueByName(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueByNameMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueListByName(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueListByNameMethod(), responseObserver);
    }

    /**
     */
    public void getAssetIssueById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAssetIssueByIdMethod(), responseObserver);
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
     */
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
     */
    public void getBlockByNum2(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByNum2Method(), responseObserver);
    }

    /**
     */
    public void getTransactionCountByBlockNum(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.NumberMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionCountByBlockNumMethod(), responseObserver);
    }

    /**
     */
    public void getDelegatedResource(org.gsc.api.GrpcAPI.DelegatedResourceMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.DelegatedResourceList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetDelegatedResourceMethod(), responseObserver);
    }

    /**
     */
    public void getDelegatedResourceAccountIndex(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.DelegatedResourceAccountIndex> responseObserver) {
      asyncUnimplementedUnaryCall(getGetDelegatedResourceAccountIndexMethod(), responseObserver);
    }

    /**
     */
    public void getExchangeById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Exchange> responseObserver) {
      asyncUnimplementedUnaryCall(getGetExchangeByIdMethod(), responseObserver);
    }

    /**
     */
    public void listExchanges(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.ExchangeList> responseObserver) {
      asyncUnimplementedUnaryCall(getListExchangesMethod(), responseObserver);
    }

    /**
     */
    public void getTransactionById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionByIdMethod(), responseObserver);
    }

    /**
     */
    public void getTransactionInfoById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.TransactionInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionInfoByIdMethod(), responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void generateAddress(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AddressPrKeyPairMessage> responseObserver) {
      asyncUnimplementedUnaryCall(getGenerateAddressMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
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
            getGetAssetIssueByNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Contract.AssetIssueContract>(
                  this, METHODID_GET_ASSET_ISSUE_BY_NAME)))
          .addMethod(
            getGetAssetIssueListByNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.api.GrpcAPI.AssetIssueList>(
                  this, METHODID_GET_ASSET_ISSUE_LIST_BY_NAME)))
          .addMethod(
            getGetAssetIssueByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Contract.AssetIssueContract>(
                  this, METHODID_GET_ASSET_ISSUE_BY_ID)))
          .addMethod(
            getGetNowBlockMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.protos.Protocol.Block>(
                  this, METHODID_GET_NOW_BLOCK)))
          .addMethod(
            getGetNowBlock2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.api.GrpcAPI.BlockExtention>(
                  this, METHODID_GET_NOW_BLOCK2)))
          .addMethod(
            getGetBlockByNumMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.NumberMessage,
                org.gsc.protos.Protocol.Block>(
                  this, METHODID_GET_BLOCK_BY_NUM)))
          .addMethod(
            getGetBlockByNum2Method(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.NumberMessage,
                org.gsc.api.GrpcAPI.BlockExtention>(
                  this, METHODID_GET_BLOCK_BY_NUM2)))
          .addMethod(
            getGetTransactionCountByBlockNumMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.NumberMessage,
                org.gsc.api.GrpcAPI.NumberMessage>(
                  this, METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM)))
          .addMethod(
            getGetDelegatedResourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.DelegatedResourceMessage,
                org.gsc.api.GrpcAPI.DelegatedResourceList>(
                  this, METHODID_GET_DELEGATED_RESOURCE)))
          .addMethod(
            getGetDelegatedResourceAccountIndexMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Protocol.DelegatedResourceAccountIndex>(
                  this, METHODID_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX)))
          .addMethod(
            getGetExchangeByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Protocol.Exchange>(
                  this, METHODID_GET_EXCHANGE_BY_ID)))
          .addMethod(
            getListExchangesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.api.GrpcAPI.ExchangeList>(
                  this, METHODID_LIST_EXCHANGES)))
          .addMethod(
            getGetTransactionByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Protocol.Transaction>(
                  this, METHODID_GET_TRANSACTION_BY_ID)))
          .addMethod(
            getGetTransactionInfoByIdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.BytesMessage,
                org.gsc.protos.Protocol.TransactionInfo>(
                  this, METHODID_GET_TRANSACTION_INFO_BY_ID)))
          .addMethod(
            getGenerateAddressMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.gsc.api.GrpcAPI.EmptyMessage,
                org.gsc.api.GrpcAPI.AddressPrKeyPairMessage>(
                  this, METHODID_GENERATE_ADDRESS)))
          .build();
    }
  }

  /**
   */
  public static final class WalletConfirmedStub extends io.grpc.stub.AbstractStub<WalletConfirmedStub> {
    private WalletConfirmedStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletConfirmedStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletConfirmedStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletConfirmedStub(channel, callOptions);
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
     */
    public void listWitnesses(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.WitnessList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListWitnessesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueList(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getPaginatedAssetIssueList(org.gsc.api.GrpcAPI.PaginatedMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetPaginatedAssetIssueListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueByName(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueByNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueListByName(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AssetIssueList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueListByNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAssetIssueById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Contract.AssetIssueContract> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAssetIssueByIdMethod(), getCallOptions()), request, responseObserver);
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
     */
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
     */
    public void getBlockByNum2(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockExtention> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByNum2Method(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionCountByBlockNum(org.gsc.api.GrpcAPI.NumberMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.NumberMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionCountByBlockNumMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDelegatedResource(org.gsc.api.GrpcAPI.DelegatedResourceMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.DelegatedResourceList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetDelegatedResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDelegatedResourceAccountIndex(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.DelegatedResourceAccountIndex> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetDelegatedResourceAccountIndexMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getExchangeById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Exchange> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetExchangeByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listExchanges(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.ExchangeList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListExchangesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTransactionInfoById(org.gsc.api.GrpcAPI.BytesMessage request,
        io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.TransactionInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionInfoByIdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public void generateAddress(org.gsc.api.GrpcAPI.EmptyMessage request,
        io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AddressPrKeyPairMessage> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGenerateAddressMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class WalletConfirmedBlockingStub extends io.grpc.stub.AbstractStub<WalletConfirmedBlockingStub> {
    private WalletConfirmedBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletConfirmedBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletConfirmedBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletConfirmedBlockingStub(channel, callOptions);
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
     */
    public org.gsc.api.GrpcAPI.WitnessList listWitnesses(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListWitnessesMethod(), getCallOptions(), request);
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
    public org.gsc.protos.Contract.AssetIssueContract getAssetIssueByName(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueByNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.AssetIssueList getAssetIssueListByName(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueListByNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Contract.AssetIssueContract getAssetIssueById(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetAssetIssueByIdMethod(), getCallOptions(), request);
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
     */
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
     */
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
    public org.gsc.api.GrpcAPI.DelegatedResourceList getDelegatedResource(org.gsc.api.GrpcAPI.DelegatedResourceMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetDelegatedResourceMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Protocol.DelegatedResourceAccountIndex getDelegatedResourceAccountIndex(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetDelegatedResourceAccountIndexMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Protocol.Exchange getExchangeById(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetExchangeByIdMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.api.GrpcAPI.ExchangeList listExchanges(org.gsc.api.GrpcAPI.EmptyMessage request) {
      return blockingUnaryCall(
          getChannel(), getListExchangesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Protocol.Transaction getTransactionById(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionByIdMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.gsc.protos.Protocol.TransactionInfo getTransactionInfoById(org.gsc.api.GrpcAPI.BytesMessage request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionInfoByIdMethod(), getCallOptions(), request);
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
  }

  /**
   */
  public static final class WalletConfirmedFutureStub extends io.grpc.stub.AbstractStub<WalletConfirmedFutureStub> {
    private WalletConfirmedFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private WalletConfirmedFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WalletConfirmedFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new WalletConfirmedFutureStub(channel, callOptions);
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
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.WitnessList> listWitnesses(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getListWitnessesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.AssetIssueList> getAssetIssueList(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.AssetIssueList> getPaginatedAssetIssueList(
        org.gsc.api.GrpcAPI.PaginatedMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetPaginatedAssetIssueListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Contract.AssetIssueContract> getAssetIssueByName(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueByNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.AssetIssueList> getAssetIssueListByName(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueListByNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Contract.AssetIssueContract> getAssetIssueById(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAssetIssueByIdMethod(), getCallOptions()), request);
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
     */
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
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.BlockExtention> getBlockByNum2(
        org.gsc.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByNum2Method(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.NumberMessage> getTransactionCountByBlockNum(
        org.gsc.api.GrpcAPI.NumberMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionCountByBlockNumMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.DelegatedResourceList> getDelegatedResource(
        org.gsc.api.GrpcAPI.DelegatedResourceMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetDelegatedResourceMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.DelegatedResourceAccountIndex> getDelegatedResourceAccountIndex(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetDelegatedResourceAccountIndexMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Exchange> getExchangeById(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetExchangeByIdMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.ExchangeList> listExchanges(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getListExchangesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.Transaction> getTransactionById(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionByIdMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.protos.Protocol.TransactionInfo> getTransactionInfoById(
        org.gsc.api.GrpcAPI.BytesMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionInfoByIdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Warning: do not invoke this interface provided by others.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.gsc.api.GrpcAPI.AddressPrKeyPairMessage> generateAddress(
        org.gsc.api.GrpcAPI.EmptyMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getGenerateAddressMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_ACCOUNT = 0;
  private static final int METHODID_GET_ACCOUNT_BY_ID = 1;
  private static final int METHODID_LIST_WITNESSES = 2;
  private static final int METHODID_GET_ASSET_ISSUE_LIST = 3;
  private static final int METHODID_GET_PAGINATED_ASSET_ISSUE_LIST = 4;
  private static final int METHODID_GET_ASSET_ISSUE_BY_NAME = 5;
  private static final int METHODID_GET_ASSET_ISSUE_LIST_BY_NAME = 6;
  private static final int METHODID_GET_ASSET_ISSUE_BY_ID = 7;
  private static final int METHODID_GET_NOW_BLOCK = 8;
  private static final int METHODID_GET_NOW_BLOCK2 = 9;
  private static final int METHODID_GET_BLOCK_BY_NUM = 10;
  private static final int METHODID_GET_BLOCK_BY_NUM2 = 11;
  private static final int METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM = 12;
  private static final int METHODID_GET_DELEGATED_RESOURCE = 13;
  private static final int METHODID_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX = 14;
  private static final int METHODID_GET_EXCHANGE_BY_ID = 15;
  private static final int METHODID_LIST_EXCHANGES = 16;
  private static final int METHODID_GET_TRANSACTION_BY_ID = 17;
  private static final int METHODID_GET_TRANSACTION_INFO_BY_ID = 18;
  private static final int METHODID_GENERATE_ADDRESS = 19;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final WalletConfirmedImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(WalletConfirmedImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
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
        case METHODID_LIST_WITNESSES:
          serviceImpl.listWitnesses((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.WitnessList>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_LIST:
          serviceImpl.getAssetIssueList((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_PAGINATED_ASSET_ISSUE_LIST:
          serviceImpl.getPaginatedAssetIssueList((org.gsc.api.GrpcAPI.PaginatedMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_BY_NAME:
          serviceImpl.getAssetIssueByName((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Contract.AssetIssueContract>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_LIST_BY_NAME:
          serviceImpl.getAssetIssueListByName((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AssetIssueList>) responseObserver);
          break;
        case METHODID_GET_ASSET_ISSUE_BY_ID:
          serviceImpl.getAssetIssueById((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Contract.AssetIssueContract>) responseObserver);
          break;
        case METHODID_GET_NOW_BLOCK:
          serviceImpl.getNowBlock((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block>) responseObserver);
          break;
        case METHODID_GET_NOW_BLOCK2:
          serviceImpl.getNowBlock2((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockExtention>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_NUM:
          serviceImpl.getBlockByNum((org.gsc.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Block>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_NUM2:
          serviceImpl.getBlockByNum2((org.gsc.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.BlockExtention>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_COUNT_BY_BLOCK_NUM:
          serviceImpl.getTransactionCountByBlockNum((org.gsc.api.GrpcAPI.NumberMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.NumberMessage>) responseObserver);
          break;
        case METHODID_GET_DELEGATED_RESOURCE:
          serviceImpl.getDelegatedResource((org.gsc.api.GrpcAPI.DelegatedResourceMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.DelegatedResourceList>) responseObserver);
          break;
        case METHODID_GET_DELEGATED_RESOURCE_ACCOUNT_INDEX:
          serviceImpl.getDelegatedResourceAccountIndex((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.DelegatedResourceAccountIndex>) responseObserver);
          break;
        case METHODID_GET_EXCHANGE_BY_ID:
          serviceImpl.getExchangeById((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Exchange>) responseObserver);
          break;
        case METHODID_LIST_EXCHANGES:
          serviceImpl.listExchanges((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.ExchangeList>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_BY_ID:
          serviceImpl.getTransactionById((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.Transaction>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_INFO_BY_ID:
          serviceImpl.getTransactionInfoById((org.gsc.api.GrpcAPI.BytesMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.protos.Protocol.TransactionInfo>) responseObserver);
          break;
        case METHODID_GENERATE_ADDRESS:
          serviceImpl.generateAddress((org.gsc.api.GrpcAPI.EmptyMessage) request,
              (io.grpc.stub.StreamObserver<org.gsc.api.GrpcAPI.AddressPrKeyPairMessage>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class WalletConfirmedBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WalletConfirmedBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.gsc.api.GrpcAPI.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("WalletConfirmed");
    }
  }

  private static final class WalletConfirmedFileDescriptorSupplier
      extends WalletConfirmedBaseDescriptorSupplier {
    WalletConfirmedFileDescriptorSupplier() {}
  }

  private static final class WalletConfirmedMethodDescriptorSupplier
      extends WalletConfirmedBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    WalletConfirmedMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (WalletConfirmedGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WalletConfirmedFileDescriptorSupplier())
              .addMethod(getGetAccountMethod())
              .addMethod(getGetAccountByIdMethod())
              .addMethod(getListWitnessesMethod())
              .addMethod(getGetAssetIssueListMethod())
              .addMethod(getGetPaginatedAssetIssueListMethod())
              .addMethod(getGetAssetIssueByNameMethod())
              .addMethod(getGetAssetIssueListByNameMethod())
              .addMethod(getGetAssetIssueByIdMethod())
              .addMethod(getGetNowBlockMethod())
              .addMethod(getGetNowBlock2Method())
              .addMethod(getGetBlockByNumMethod())
              .addMethod(getGetBlockByNum2Method())
              .addMethod(getGetTransactionCountByBlockNumMethod())
              .addMethod(getGetDelegatedResourceMethod())
              .addMethod(getGetDelegatedResourceAccountIndexMethod())
              .addMethod(getGetExchangeByIdMethod())
              .addMethod(getListExchangesMethod())
              .addMethod(getGetTransactionByIdMethod())
              .addMethod(getGetTransactionInfoByIdMethod())
              .addMethod(getGenerateAddressMethod())
              .build();
        }
      }
    }
    return result;
  }
}
