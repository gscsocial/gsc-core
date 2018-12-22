/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.gsc.net.message;

import org.gsc.common.overlay.message.Message;

public abstract class GSCMessage extends Message {

  public GSCMessage() {
  }

  public GSCMessage(byte[] rawData) {
    super(rawData);
  }

  public GSCMessage(byte type, byte[] rawData) {
    super(type, rawData);
  }
}
