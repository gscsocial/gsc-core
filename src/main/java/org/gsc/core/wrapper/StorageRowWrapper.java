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

package org.gsc.core.wrapper;

import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gsc.runtime.vm.DataWord;
import org.gsc.common.utils.Sha256Hash;


@Slf4j
public class StorageRowWrapper implements ProtoWrapper<byte[]> {

  private byte[] rowValue;
  @Setter
  @Getter
  private byte[] rowKey;

  @Getter
  private boolean dirty = false;

  private void markDirty() {
    dirty = true;
  }

  private StorageRowWrapper() {
  }

  public StorageRowWrapper(byte[] rowKey, byte[] rowValue) {
    this.rowKey = rowKey;
    this.rowValue = rowValue;
    markDirty();
  }

  public StorageRowWrapper(byte[] rowValue) {
    this.rowValue = rowValue;
  }


  public Sha256Hash getHash() {
    return Sha256Hash.of(this.rowValue);
  }


  public DataWord getValue() {
    return new DataWord(this.rowValue);
  }

  public void setValue(DataWord value) {
    this.rowValue = value.getData();
    markDirty();
  }

  @Override
  public byte[] getData() {
    return this.rowValue;
  }

  @Override
  public byte[] getInstance() {
    return this.rowValue;
  }

  @Override
  public String toString() {
    return Arrays.toString(rowValue);
  }
}
