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

package org.gsc.wallet.common.client.utils;

/*
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gsc-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.protobuf.ByteString;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.ECKey.ECDSASignature;
import org.gsc.utils.Sha256Hash;
//import org.gsc.protos.Protocol.DeferredStage;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract;


public class TransactionUtils {

  private static final Logger logger = LoggerFactory.getLogger("Transaction");
  private static final int RESERVE_BALANCE = 10;
  public static final int NORMALTRANSACTION = 0;
  public static final int UNEXECUTEDDEFERREDTRANSACTION = 1;
  public static final int EXECUTINGDEFERREDTRANSACTION = 2;

  /**
   * constructor.
   */

  public static byte[] getHash(Transaction transaction) {
    Transaction.Builder tmp = transaction.toBuilder();
    //tmp.clearId();

    return Sha256Hash.hash(tmp.build().toByteArray());
  }

  /**
   * constructor.
   */

  public static byte[] getOwner(Transaction.Contract contract) {
    ByteString owner;
    try {
      switch (contract.getType()) {
        case AccountCreateContract:
          owner = contract.getParameter()
              .unpack(org.gsc.protos.Contract.AccountCreateContract.class).getOwnerAddress();
          break;
        case TransferContract:
          owner = contract.getParameter().unpack(org.gsc.protos.Contract.TransferContract.class)
              .getOwnerAddress();
          break;
        case TransferAssetContract:
          owner = contract.getParameter()
              .unpack(org.gsc.protos.Contract.TransferAssetContract.class).getOwnerAddress();
          break;
        case VoteAssetContract:
          owner = contract.getParameter().unpack(org.gsc.protos.Contract.VoteAssetContract.class)
              .getOwnerAddress();
          break;
        case VoteWitnessContract:
          owner = contract.getParameter().unpack(org.gsc.protos.Contract.VoteWitnessContract.class)
              .getOwnerAddress();
          break;
        case WitnessCreateContract:
          owner = contract.getParameter()
              .unpack(org.gsc.protos.Contract.WitnessCreateContract.class).getOwnerAddress();
          break;
        case AssetIssueContract:
          owner = contract.getParameter().unpack(org.gsc.protos.Contract.AssetIssueContract.class)
              .getOwnerAddress();
          break;
        case ParticipateAssetIssueContract:
          owner = contract.getParameter()
              .unpack(org.gsc.protos.Contract.ParticipateAssetIssueContract.class)
              .getOwnerAddress();
          break;
        case CreateSmartContract:
          owner = contract.getParameter().unpack(org.gsc.protos.Contract.CreateSmartContract.class)
              .getOwnerAddress();
          break;
        default:
          return null;
      }
      return owner.toByteArray();
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */

  public static String getBase64FromByteString(ByteString sign) {
    byte[] r = sign.substring(0, 32).toByteArray();
    byte[] s = sign.substring(32, 64).toByteArray();
    byte v = sign.byteAt(64);
    if (v < 27) {
      v += 27; //revId -> v
    }
    ECDSASignature signature = ECDSASignature.fromComponents(r, s, v);
    return signature.toBase64();
  }

  /*
   * 1. check hash
   * 2. check double spent
   * 3. check sign
   * 4. check balance
   */

  public static boolean validTransaction(Transaction signedTransaction) {
    assert (signedTransaction.getSignatureCount()
        == signedTransaction.getRawData().getContractCount());
    List<Transaction.Contract> listContract = signedTransaction.getRawData().getContractList();
    byte[] hash = Sha256Hash.hash(signedTransaction.getRawData().toByteArray());
    int count = signedTransaction.getSignatureCount();
    if (count == 0) {
      return false;
    }
    for (int i = 0; i < count; ++i) {
      try {
        Transaction.Contract contract = listContract.get(i);
        byte[] owner = getOwner(contract);
        byte[] address = ECKey
            .signatureToAddress(hash, getBase64FromByteString(signedTransaction.getSignature(i)));
        if (!Arrays.equals(owner, address)) {
          return false;
        }
      } catch (SignatureException e) {
        e.printStackTrace();
        return false;
      }
    }
    return true;
  }

  /**
   * constructor.
   */

  public static Transaction sign(Transaction transaction, ECKey myKey) {
    ByteString lockSript = ByteString.copyFrom(myKey.getAddress());
    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();

    byte[] hash = Sha256Hash.hash(transaction.getRawData().toByteArray());
    List<Contract> listContract = transaction.getRawData().getContractList();
    for (int i = 0; i < listContract.size(); i++) {
      ECDSASignature signature = myKey.sign(hash);
      ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
      transactionBuilderSigned.addSignature(
          bsSign);//Each contract may be signed with a different private key in the future.
    }

    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  /**
   * constructor.
   */
  public static Transaction setTimestamp(Transaction transaction) {
    long currentTime = System.currentTimeMillis();//*1000000 + System.nanoTime()%1000000;
    Transaction.Builder builder = transaction.toBuilder();
    org.gsc.protos.Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData()
        .toBuilder();
    rowBuilder.setTimestamp(currentTime);
    builder.setRawData(rowBuilder.build());
    return builder.build();
  }

  /*  *//**
   * constructor.
   *//*
  public static GrpcAPI.TransactionExtention setDelaySecondsToExtension(GrpcAPI
      .TransactionExtention transactionExtention, long delaySeconds) {
    if (delaySeconds == 0) {
      return transactionExtention;
    }
    GrpcAPI.TransactionExtention.Builder builder = transactionExtention.toBuilder();

    Transaction transaction = setDelaySeconds(transactionExtention.getTransaction(), delaySeconds);
    builder.setTransaction(transaction);

    return builder.build();
  }*/

  /**
   * constructor.
   */
  /*  public static Transaction setDelaySeconds(Transaction transaction, long delaySeconds) {
    DeferredStage deferredStage = transaction.getRawData().toBuilder()
        .getDeferredStage().toBuilder().setDelaySeconds(delaySeconds)
        .setStage(UNEXECUTEDDEFERREDTRANSACTION).build();
    Transaction.raw rawData = transaction.toBuilder().getRawData()
        .toBuilder().setDeferredStage(deferredStage).build();
    return transaction.toBuilder().setRawData(rawData).build();
  }*/


}
