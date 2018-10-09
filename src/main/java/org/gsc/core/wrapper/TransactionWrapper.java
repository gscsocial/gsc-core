/*
 * java-gsc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-gsc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gsc.core.wrapper;

import static org.gsc.protos.Contract.AssetIssueContract;
import static org.gsc.protos.Contract.VoteAssetContract;
import static org.gsc.protos.Contract.VoteWitnessContract;
import static org.gsc.protos.Contract.WitnessCreateContract;
import static org.gsc.protos.Contract.WitnessUpdateContract;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.ECKey.ECDSASignature;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.core.exception.BadItemException;
import org.gsc.core.exception.ValidateSignatureException;
import org.gsc.core.Wallet;
import org.gsc.db.AccountStore;
import org.gsc.protos.Contract.AccountCreateContract;
import org.gsc.protos.Contract.AccountUpdateContract;
import org.gsc.protos.Contract.FreezeBalanceContract;
import org.gsc.protos.Contract.ParticipateAssetIssueContract;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Contract.TransferContract;
import org.gsc.protos.Contract.UnfreezeAssetContract;
import org.gsc.protos.Contract.UnfreezeBalanceContract;
import org.gsc.protos.Contract.UpdateAssetContract;
import org.gsc.protos.Contract.WithdrawBalanceContract;
import org.gsc.protos.Protocol.Transaction;
import org.gsc.protos.Protocol.Transaction.Contract.ContractType;
import org.spongycastle.util.encoders.Hex;

@Slf4j
public class TransactionWrapper implements ProtoWrapper<Transaction> {

  private Transaction transaction;
  @Setter
  private boolean isVerified = false;
  /**
   * constructor TransactionWrapper.
   */
  public TransactionWrapper(Transaction trx) {
    this.transaction = trx;
  }

  /**
   * get account from bytes data.
   */
  public TransactionWrapper(byte[] data) throws BadItemException {
    try {
      this.transaction = Transaction.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new BadItemException("Transaction proto data parse exception");
    }
  }

  /*lll
  public TransactionWrapper(byte[] key, long value) throws IllegalArgumentException {
    if (!Wallet.addressValid(key)) {
      throw new IllegalArgumentException("Invalid address");
    }
    TransferContract transferContract = TransferContract.newBuilder()
        .setAmount(value)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(key))
        .build();
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(ContractType.TransferContract).setParameter(
            Any.pack(transferContract)).build());
    logger.info("Transaction create succeeded！");
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }*/

  public TransactionWrapper(AccountCreateContract contract, AccountStore accountStore) {
    AccountWrapper account = accountStore.get(contract.getOwnerAddress().toByteArray());
    if (account != null && account.getType() == contract.getType()) {
      return; // Account isexit
    }

    createTransaction(contract, ContractType.AccountCreateContract);
  }

  public TransactionWrapper(TransferContract contract, AccountStore accountStore) {
    Transaction.Contract.Builder contractBuilder = Transaction.Contract.newBuilder();

    AccountWrapper owner = accountStore.get(contract.getOwnerAddress().toByteArray());
    if (owner == null || owner.getBalance() < contract.getAmount()) {
      return; //The balance is not enough
    }

    createTransaction(contract, ContractType.TransferContract);
  }

  public TransactionWrapper(VoteWitnessContract voteWitnessContract) {
    createTransaction(voteWitnessContract, ContractType.VoteWitnessContract);
  }

  public TransactionWrapper(WitnessCreateContract witnessCreateContract) {
    createTransaction(witnessCreateContract, ContractType.WitnessCreateContract);
  }

  public TransactionWrapper(WitnessUpdateContract witnessUpdateContract) {
    createTransaction(witnessUpdateContract, ContractType.WitnessUpdateContract);
  }

  public TransactionWrapper(TransferAssetContract transferAssetContract) {
    createTransaction(transferAssetContract, ContractType.TransferAssetContract);
  }

  public TransactionWrapper(ParticipateAssetIssueContract participateAssetIssueContract) {
    createTransaction(participateAssetIssueContract, ContractType.ParticipateAssetIssueContract);
  }

  public void resetResult() {
    this.transaction = this.getInstance().toBuilder().clearRet().build();
  }

  public void setResult(TransactionResultWrapper transactionResultWrapper) {
//    this.transaction = this.getInstance().toBuilder().addRet(transactionResultWrapper.getInstance()).build();
  }

  public void setReference(long blockNum, byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = this.transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(ByteArray.subArray(blockHash, 8, 16)))
        .setRefBlockBytes(ByteString.copyFrom(ByteArray.subArray(refBlockNum, 6, 8)))
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  /**
   * @param expiration must be in milliseconds format
   */
  public void setExpiration(long expiration) {
    Transaction.raw rawData = this.transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
  }

  public long getExpiration() {
    return transaction.getRawData().getExpiration();
  }

  @Deprecated
  public TransactionWrapper(AssetIssueContract assetIssueContract) {
    createTransaction(assetIssueContract, ContractType.AssetIssueContract);
  }

  public TransactionWrapper(com.google.protobuf.Message message, ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  @Deprecated
  public void createTransaction(com.google.protobuf.Message message, ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public Sha256Hash getMerkleHash() {
    byte[] transBytes = this.transaction.toByteArray();
    return Sha256Hash.of(transBytes);
  }

  private Sha256Hash getRawHash() {
    return Sha256Hash.of(this.transaction.getRawData().toByteArray());
  }

  /**
   * check balance of the address.
   */
  public boolean checkBalance(byte[] address, byte[] to, long amount, long balance) {
    if (!Wallet.addressValid(address)) {
      logger.error("address invalid");
      return false;
    }

    if (!Wallet.addressValid(to)) {
      logger.error("address invalid");
      return false;
    }

    if (amount <= 0) {
      logger.error("amount required a positive number");
      return false;
    }

    if (amount > balance) {
      logger.error("don't have enough money");
      return false;
    }

    return true;
  }

  public void sign(byte[] privateKey) {
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(getRawHash().getBytes());
    ByteString sig = ByteString.copyFrom(signature.toByteArray());
    this.transaction = this.transaction.toBuilder().addSignature(sig).build();
  }

  // todo mv this static function to capsule util
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
        case UpdateAssetContract:
          owner = contractParameter.unpack(UpdateAssetContract.class).getOwnerAddress();
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

  // todo mv this static function to capsule util
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


  /**
   * validate signature
   */
  public boolean validateSignature() throws ValidateSignatureException {
    if (isVerified == true) {
      return true;
    }

    if (this.getInstance().getSignatureCount() !=
        this.getInstance().getRawData().getContractCount()) {
      throw new ValidateSignatureException("miss sig or contract");
    }

    List<Transaction.Contract> listContract = this.transaction.getRawData().getContractList();
    for (int i = 0; i < this.transaction.getSignatureCount(); ++i) {
      try {
        Transaction.Contract contract = listContract.get(i);
        byte[] owner = getOwner(contract);
        byte[] address = ECKey.signatureToAddress(getRawHash().getBytes(),
            getBase64FromByteString(this.transaction.getSignature(i)));
        logger.info("check sig owner={} address={}",Hex.toHexString(owner),Hex.toHexString(address));
        if (!Arrays.equals(owner, address)) {
          isVerified = false;
          throw new ValidateSignatureException("sig error");
        }
      } catch (SignatureException e) {
        isVerified = false;
        throw new ValidateSignatureException(e.getMessage());
      }
    }

    isVerified = true;
    return true;
  }

  public Sha256Hash getTransactionId() {
    return getRawHash();
  }

  @Override
  public byte[] getData() {
    return this.transaction.toByteArray();
  }

  public long getSerializedSize() {
    return this.transaction.getSerializedSize();
  }

  @Override
  public Transaction getInstance() {
    return this.transaction;
  }

  private StringBuffer toStringBuff = new StringBuffer();

  @Override
  public String toString() {

    toStringBuff.setLength(0);
    toStringBuff.append("TransactionWrapper \n[ ");

    toStringBuff.append("hash=").append(getTransactionId()).append("\n");
    AtomicInteger i = new AtomicInteger();
    if (!getInstance().getRawData().getContractList().isEmpty()) {
      toStringBuff.append("contract list:{ ");
      getInstance().getRawData().getContractList().forEach(contract -> {
        toStringBuff.append("[" + i + "] ").append("type: ").append(contract.getType())
            .append("\n");
        toStringBuff.append("from address=").append(getOwner(contract)).append("\n");
        toStringBuff.append("to address=").append(getToAddress(contract)).append("\n");
        if (contract.getType().equals(ContractType.TransferContract)) {
          TransferContract transferContract;
          try {
            transferContract = contract.getParameter()
                .unpack(TransferContract.class);
            toStringBuff.append("transfer amount=").append(transferContract.getAmount())
                .append("\n");
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        } else if (contract.getType().equals(ContractType.TransferAssetContract)) {
          TransferAssetContract transferAssetContract;
          try {
            transferAssetContract = contract.getParameter()
                .unpack(TransferAssetContract.class);
            toStringBuff.append("transfer asset=").append(transferAssetContract.getAssetName())
                .append("\n");
            toStringBuff.append("transfer amount=").append(transferAssetContract.getAmount())
                .append("\n");
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        }
        if (this.transaction.getSignatureList().size() >= i.get() + 1) {
          toStringBuff.append("sign=").append(getBase64FromByteString(
              this.transaction.getSignature(i.getAndIncrement()))).append("\n");
        }
      });
      toStringBuff.append("}\n");
    } else {
      toStringBuff.append("contract list is empty\n");
    }

    toStringBuff.append("]");
    return toStringBuff.toString();
  }
}