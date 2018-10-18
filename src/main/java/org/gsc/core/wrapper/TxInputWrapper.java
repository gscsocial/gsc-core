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

import com.google.protobuf.ByteString;
import org.gsc.protos.Protocol.TXInput;

public class TxInputWrapper implements ProtoWrapper<TXInput> {

  private TXInput txInput;

  /**
   * constructor TxInputWrapper.
   *
   * @param txId byte[] txId
   * @param vout int vout
   * @param signature byte[] signature
   * @param pubKey byte[] pubKey
   */
  public TxInputWrapper(byte[] txId, long vout, byte[]
          signature, byte[] pubKey) {
    TXInput.raw txInputRaw = TXInput.raw.newBuilder()
            .setTxID(ByteString.copyFrom(txId))
            .setVout(vout)
            .setPubKey(ByteString.copyFrom(pubKey)).build();

    this.txInput = TXInput.newBuilder()
            .setRawData(txInputRaw)
            .setSignature(ByteString.copyFrom(signature))
            .build();

  }

  public TXInput getTxInput() {
    return txInput;
  }

  public boolean validate() {
    return true;
  }

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  @Override
  public TXInput getInstance() {
    return this.txInput;
  }
}
