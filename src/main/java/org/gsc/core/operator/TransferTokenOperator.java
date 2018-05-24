/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gsc.core.operator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import org.gsc.common.exception.ContractExeException;
import org.gsc.common.exception.ContractValidateException;
import org.gsc.common.utils.AddressUtil;
import org.gsc.common.utils.TransactionUtil;
import org.gsc.core.chain.TransactionResultWrapper;
import org.gsc.core.wrapper.AccountWrapper;
import org.gsc.db.AccountStore;
import org.gsc.protos.Contract.TransferAssetContract;
import org.gsc.protos.Protocol.Transaction.Result.code;


public class TransferTokenOperator extends AbstractOperator {

  TransferTokenOperator(Any contract) { super(contract); }

  @Override
  public boolean execute(TransactionResultWrapper ret) throws ContractExeException {
    long fee = calcFee();
    try {
      TransferAssetContract transferAssetContract = this.contract
          .unpack(TransferAssetContract.class);
      AccountStore accountStore = this.dbManager.getAccountStore();
      byte[] ownerKey = transferAssetContract.getOwnerAddress().toByteArray();
      byte[] toKey = transferAssetContract.getToAddress().toByteArray();
      ByteString assetName = transferAssetContract.getAssetName();
      long amount = transferAssetContract.getAmount();

      AccountWrapper ownerAccount = accountStore.get(ownerKey);
      if (!ownerAccount.reduceTokenAmount(assetName, amount)) {
        throw new ContractExeException("reduceAssetAmount failed !");
      }
      accountStore.put(ownerKey, ownerAccount);

      AccountWrapper toAccountCapsule = accountStore.get(toKey);
      toAccountCapsule.addTokenAmount(assetName, amount);
      accountStore.put(toKey, toAccountCapsule);

      ret.setStatus(fee, code.SUCCESS);
    } catch (InvalidProtocolBufferException e) {
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    } catch (ArithmeticException e) {
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (!this.contract.is(TransferAssetContract.class)) {
        throw new ContractValidateException();
      }
      if (this.dbManager == null) {
        throw new ContractValidateException();
      }
      TransferAssetContract transferAssetContract = this.contract
          .unpack(TransferAssetContract.class);

      byte[] ownerAddress = transferAssetContract.getOwnerAddress().toByteArray();
      byte[] toAddress = transferAssetContract.getToAddress().toByteArray();
      byte[] assetName = transferAssetContract.getAssetName().toByteArray();
      long amount = transferAssetContract.getAmount();

      if (!AddressUtil.addressValid(ownerAddress)) {
        throw new ContractValidateException("Invalidate ownerAddress");
      }
      if (!AddressUtil.addressValid(toAddress)) {
        throw new ContractValidateException("Invalidate toAddress");
      }
      if (!TransactionUtil.validAssetName(assetName)) {
        throw new ContractValidateException("Invalidate assetName");
      }
      if (amount <= 0) {
        throw new ContractValidateException("Amount must greater than 0.");
      }

      if (Arrays.equals(ownerAddress, toAddress)) {
        throw new ContractValidateException("Cannot transfer asset to yourself.");
      }

      AccountWrapper ownerAccount = this.dbManager.getAccountStore().get(ownerAddress);
      if (ownerAccount == null) {
        throw new ContractValidateException("No owner account!");
      }

      //TODO
//      if (!this.dbManager.getAssetIssueStore().has(assetName)) {
//        throw new ContractValidateException("No asset !");
//      }

//      Map<String, Long> asset = ownerAccount.getAssetMap();
//      if (asset.isEmpty()) {
//        throw new ContractValidateException("Owner no asset!");
//      }

//      Long assetBalance = asset.get(ByteArray.toStr(assetName));
//      if (null == assetBalance || assetBalance <= 0) {
//        throw new ContractValidateException("assetBalance must greater than 0.");
//      }
//      if (amount > assetBalance) {
//        throw new ContractValidateException("assetBalance is not sufficient.");
//      }
//
//      AccountWrapper toAccount = this.dbManager.getAccountStore().get(toAddress);
//      if (toAccount == null) {
//        throw new ContractValidateException("To account is not exit!");
//      }
//
//      assetBalance = toAccount.getAssetMap().get(ByteArray.toStr(assetName));
//      if (assetBalance != null) {
//        assetBalance = Math.addExact(assetBalance, amount); //check if overflow
//      }
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    } catch (ArithmeticException e) {
      throw new ContractValidateException(e.getMessage());
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(TransferAssetContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
