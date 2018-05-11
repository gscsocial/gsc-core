package org.gsc.core.chain;

import org.gsc.common.utils.Sha256Hash;

public class BlockId extends Sha256Hash {

  public BlockId(long num, byte[] hash) {
    super(num, hash);
  }
}
