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

package org.gsc.core;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.TxOutputWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.gsc.common.utils.ByteArray;

@Slf4j
public class TxOutputWrapperTest {
  @Test
  public void testTxOutputCapsule() {
    long value = 123456L;
    String address = "3450dde5007c67a50ec2e09489fa53ec1ff59c61e7ddea9638645e6e5f62e5f5";
    TxOutputWrapper txOutputWrapper = new TxOutputWrapper(value, address);

    Assert.assertEquals(value, txOutputWrapper.getTxOutput().getValue());
    Assert.assertEquals(address,
        ByteArray.toHexString(txOutputWrapper.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputWrapper.validate());

    long value3 = 9852448L;
    String address3 = "0xfd1a5decba973b0d31e84e7d8f4a5b10d33ab37ce6533f1ff5a9db2d9db8ef";
    String address4 = "fd1a5decba973b0d31e84e7d8f4a5b10d33ab37ce6533f1ff5a9db2d9db8ef";
    TxOutputWrapper txOutputWrapper2 = new TxOutputWrapper(value3, address3);

    Assert.assertEquals(value3, txOutputWrapper2.getTxOutput().getValue());
    Assert.assertEquals(address4,
        ByteArray.toHexString(txOutputWrapper2.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputWrapper2.validate());

    long value5 = 67549L;
    String address5 = null;
    TxOutputWrapper txOutputWrapper3 = new TxOutputWrapper(value5, address5);

    Assert.assertEquals(value5, txOutputWrapper3.getTxOutput().getValue());
    Assert.assertEquals("",
        ByteArray.toHexString(txOutputWrapper3.getTxOutput().getPubKeyHash().toByteArray()));
    Assert.assertTrue(txOutputWrapper3.validate());

  }

}

