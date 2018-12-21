package org.gsc.common.crypto;

import static org.junit.Assert.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.Utils;
import org.gsc.crypto.ECKey;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.Wallet;

@Slf4j
public class ECKeyTest {

  @Test
  public void testGeClientTestEcKey() {
   // final ECKey key = ECKey.fromPrivate(
     //   Hex.decode("1cd5a70741c6e583d2dd3c5f17231e608eb1e52437210d948c5085e141c2d830"));

    //assertEquals(Wallet.getAddressPreFixString() + "125b6c87b3d67114b3873977888c34582f27bbb0",
      //  ByteArray.toHexString(key.getAddress()));

    for(int i=0 ; i< 29;i++){
      ECKey ecKey = new ECKey(Utils.getRandom());
      //ECKey ecKey = ECKey.fromPrivate(Hex.decode("6836686763e953e47bd313dd534ce3018654eca5bb26ca6b95739f5f1a1ccd3c"));
      byte[] priKey = ecKey.getPrivKeyBytes();
      byte[] address = ecKey.getAddress();
      String addressStr = Wallet.encode58Check(address); //GKuH5fbaTVNZSyNsLm9JndCEftjkAhCCMg
      String priKeyStr = org.apache.commons.codec.binary.Hex.encodeHexString(priKey);
      logger.info(addressStr+" "+priKeyStr);
    }
  }
}