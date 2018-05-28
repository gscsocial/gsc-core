package org.gsc.core.chain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.gsc.protos.Contract.AccountCreateContract;
import org.gsc.protos.Contract.AccountUpdateContract;
import org.gsc.protos.Contract.AssetIssueContract;
import org.gsc.protos.Contract.DeployContract;
import org.gsc.protos.Contract.FreezeBalanceContract;
import org.gsc.protos.Contract.ParticipateAssetIssueContract;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Contract.UnfreezeAssetContract;
import org.gsc.protos.Contract.UnfreezeBalanceContract;
import org.gsc.protos.Contract.VoteAssetContract;
import org.gsc.protos.Contract.VoteWitnessContract;
import org.gsc.protos.Contract.WithdrawBalanceContract;
import org.gsc.protos.Contract.WitnessCreateContract;
import org.gsc.protos.Contract.WitnessUpdateContract;
import org.gsc.protos.Protocol.Transaction;

public interface ProtoUtil {

  public static byte[] getOwner(Transaction.Contract contract) {
    ByteString owner;
    try {
      Any contractParameter = contract.getParameter();

      switch (contract.getType()) {
        case AccountCreateContract:
          owner = contractParameter.unpack(AccountCreateContract.class).getOwnerAddress();
          break;
        case TransferContract:
          owner = contractParameter.unpack(TransferContract.class).getOwnerAddress();
          break;
        case TransferAssetContract:
          owner = contractParameter.unpack(TransferAssetContract.class).getOwnerAddress();
          break;
        case VoteAssetContract:
          owner = contractParameter.unpack(VoteAssetContract.class).getOwnerAddress();
          break;
        case VoteWitnessContract:
          owner = contractParameter.unpack(VoteWitnessContract.class).getOwnerAddress();
          break;
        case WitnessCreateContract:
          owner = contractParameter.unpack(WitnessCreateContract.class).getOwnerAddress();
          break;
        case AssetIssueContract:
          owner = contractParameter.unpack(AssetIssueContract.class).getOwnerAddress();
          break;
        case DeployContract:
          owner = contractParameter.unpack(DeployContract.class).getOwnerAddress();
          break;
        case WitnessUpdateContract:
          owner = contractParameter.unpack(WitnessUpdateContract.class).getOwnerAddress();
          break;
        case ParticipateAssetIssueContract:
          owner = contractParameter.unpack(ParticipateAssetIssueContract.class).getOwnerAddress();
          break;
        case AccountUpdateContract:
          owner = contractParameter.unpack(AccountUpdateContract.class).getOwnerAddress();
          break;
        case FreezeBalanceContract:
          owner = contractParameter.unpack(FreezeBalanceContract.class).getOwnerAddress();
          break;
        case UnfreezeBalanceContract:
          owner = contractParameter.unpack(UnfreezeBalanceContract.class).getOwnerAddress();
          break;
        case UnfreezeAssetContract:
          owner = contractParameter.unpack(UnfreezeAssetContract.class).getOwnerAddress();
          break;
        case WithdrawBalanceContract:
          owner = contractParameter.unpack(WithdrawBalanceContract.class).getOwnerAddress();
          break;
        // todo add other contract
        default:
          return null;
      }
      return owner.toByteArray();
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public static byte[] getToAddress(Transaction.Contract contract) {
    ByteString to;
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case TransferContract:
          to = contractParameter.unpack(TransferContract.class).getToAddress();
          break;
        case TransferAssetContract:
          to = contractParameter.unpack(TransferAssetContract.class).getToAddress();
          break;
        case ParticipateAssetIssueContract:
          to = contractParameter.unpack(ParticipateAssetIssueContract.class).getToAddress();
          break;
        // todo add other contract

        default:
          return null;
      }
      return to.toByteArray();
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

}
