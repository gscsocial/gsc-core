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
import org.gsc.common.utils.ByteArray;
import org.gsc.protos.Protocol.TXOutput;

public class TxOutputWrapper implements ProtoWrapper<TXOutput> {

  private TXOutput txOutput;

  /**
   * constructor TxOutputWrapper.
   *
   * @param value int value
   * @param address String address
   */
  public TxOutputWrapper(long value, String address) {
    this.txOutput = TXOutput.newBuilder()
        .setValue(value)
        .setPubKeyHash(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .build();
  }

  public TXOutput getTxOutput() {
    return txOutput;
  }

  /**
   * validateSignature.
   */
  public boolean validate() {
    return true;
  }

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  @Override
  public TXOutput getInstance() {
    return this.txOutput;
  }
}
