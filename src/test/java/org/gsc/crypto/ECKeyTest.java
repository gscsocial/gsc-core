package org.gsc.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.SignatureException;
import java.util.Arrays;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.core.wrapper.WitnessWrapper;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.gsc.core.Wallet;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import static org.iq80.leveldb.impl.Iq80DBFactory.*;

@Slf4j
public class ECKeyTest {

  private String privString = "0DA11901BCA7EE3C66927974F10F08127FD61CF74C0BF6CA6A6F9A03E5222925";
  private BigInteger privateKey = new BigInteger(privString, 16);

  private String pubString = "040947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad75aa17564ae80a20bb044ee7a6d903e8e8df624b089c95d66a0570f051e5a05b";
  private String compressedPubString = "030947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad";
  private byte[] pubKey = Hex.decode(pubString);
  private byte[] compressedPubKey = Hex.decode(compressedPubString);
  private String address = "cd2a3d9f938e13cd947ec05abc7fe734df8dd826";

  @Test
  public void testHashCode() {
    assertEquals(-351262686, ECKey.fromPrivate(privateKey).hashCode());
  }

  @Test
  public void testECKey() {
    ECKey key = ECKey.fromPrivate(privateKey);
    assertTrue(key.isPubKeyCanonical());
    assertNotNull(key.getPubKey());
    assertNotNull(key.getPrivKeyBytes());
    logger.info(Hex.toHexString(key.getPrivKeyBytes()) + " :Generated privkey");
    logger.info(Hex.toHexString(key.getPubKey()) + " :Generated pubkey");
    logger.info(Hex.toHexString(key.getAddress()));
    WitnessWrapper witnessCapsule = new WitnessWrapper(ByteString.copyFrom(key.getAddress()));
    ByteString address = witnessCapsule.getAddress();
    String addressString = Wallet.encode58Check(address.toByteArray());
    logger.info(addressString);
  }

  @Test
  public void testFromPrivateKey() {
    ECKey key = ECKey.fromPrivate(privateKey);
    WitnessWrapper witnessCapsule = new WitnessWrapper(ByteString.copyFrom(key.getAddress()));
    ByteString address = witnessCapsule.getAddress();
    String addressString = Wallet.encode58Check(address.toByteArray());
    assertTrue(key.isPubKeyCanonical());
    assertTrue(key.hasPrivKey());
    assertArrayEquals(pubKey, key.getPubKey());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPrivatePublicKeyBytesNoArg() {
    new ECKey((BigInteger) null, null);
    fail("Expecting an IllegalArgumentException for using only null-parameters");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidPrivateKey() throws Exception {
    new ECKey(
            Security.getProvider("SunEC"),
            KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate(),
            ECKey.fromPublicOnly(pubKey).getPubKeyPoint());
    fail("Expecting an IllegalArgumentException for using an non EC private key");
  }

  @Test
  public void testIsPubKeyOnly() {
    ECKey key = ECKey.fromPublicOnly(pubKey);
    assertTrue(key.isPubKeyCanonical());
    assertTrue(key.isPubKeyOnly());
    assertArrayEquals(key.getPubKey(), pubKey);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSignIncorrectInputSize() {
    ECKey key = new ECKey();
    String message = "The quick brown fox jumps over the lazy dog.";
    ECKey.ECDSASignature sig = key.doSign(message.getBytes());
    fail("Expecting an IllegalArgumentException for a non 32-byte input");
  }

  @Test(expected = SignatureException.class)
  public void testBadBase64Sig() throws SignatureException {
    byte[] messageHash = new byte[32];
    ECKey.signatureToKey(messageHash, "This is not valid Base64!");
    fail("Expecting a SignatureException for invalid Base64");
  }

  @Test(expected = SignatureException.class)
  public void testInvalidSignatureLength() throws SignatureException {
    byte[] messageHash = new byte[32];
    ECKey.signatureToKey(messageHash, "abcdefg");
    fail("Expecting a SignatureException for invalid signature length");
  }

  @Test
  public void testPublicKeyFromPrivate() {
    byte[] pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, false);
    assertArrayEquals(pubKey, pubFromPriv);
  }

  @Test
  public void testPublicKeyFromPrivateCompressed() {
    byte[] pubFromPriv = ECKey.publicKeyFromPrivate(privateKey, true);
    assertArrayEquals(compressedPubKey, pubFromPriv);
  }

  @Test
  public void testGetAddress() {
    ECKey key = ECKey.fromPublicOnly(pubKey);
    // Addresses are prefixed with a constant.
    byte[] prefixedAddress = key.getAddress();
    byte[] unprefixedAddress = Arrays.copyOfRange(key.getAddress(), 1, prefixedAddress.length);
    assertArrayEquals(Hex.decode(address), unprefixedAddress);
    assertEquals(Wallet.getAddressPreFixByte(), prefixedAddress[0]);
  }

  @Test
  public void testGetAddressFromPrivateKey() {
    ECKey key = ECKey.fromPrivate(privateKey);
    // Addresses are prefixed with a constant.
    byte[] prefixedAddress = key.getAddress();
    byte[] unprefixedAddress = Arrays.copyOfRange(key.getAddress(), 1, prefixedAddress.length);
    assertArrayEquals(Hex.decode(address), unprefixedAddress);
    assertEquals(Wallet.getAddressPreFixByte(), prefixedAddress[0]);
  }

  @Test
  public void testToString() {
    ECKey key = ECKey.fromPrivate(BigInteger.TEN); // An example private key.
    assertEquals("pub:04a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7", key.toString());
  }

  @Test
  public void testIsPubKeyCanonicalCorect() {
    // Test correct prefix 4, right length 65
    byte[] canonicalPubkey1 = new byte[65];
    canonicalPubkey1[0] = 0x04;
    assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey1));
    // Test correct prefix 2, right length 33
    byte[] canonicalPubkey2 = new byte[33];
    canonicalPubkey2[0] = 0x02;
    assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey2));
    // Test correct prefix 3, right length 33
    byte[] canonicalPubkey3 = new byte[33];
    canonicalPubkey3[0] = 0x03;
    assertTrue(ECKey.isPubKeyCanonical(canonicalPubkey3));
  }

  @Test
  public void testIsPubKeyCanonicalWrongLength() {
    // Test correct prefix 4, but wrong length !65
    byte[] nonCanonicalPubkey1 = new byte[64];
    nonCanonicalPubkey1[0] = 0x04;
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey1));
    // Test correct prefix 2, but wrong length !33
    byte[] nonCanonicalPubkey2 = new byte[32];
    nonCanonicalPubkey2[0] = 0x02;
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey2));
    // Test correct prefix 3, but wrong length !33
    byte[] nonCanonicalPubkey3 = new byte[32];
    nonCanonicalPubkey3[0] = 0x03;
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey3));
  }

  @Test
  public void testIsPubKeyCanonicalWrongPrefix() {
    // Test wrong prefix 4, right length 65
    byte[] nonCanonicalPubkey4 = new byte[65];
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey4));
    // Test wrong prefix 2, right length 33
    byte[] nonCanonicalPubkey5 = new byte[33];
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey5));
    // Test wrong prefix 3, right length 33
    byte[] nonCanonicalPubkey6 = new byte[33];
    assertFalse(ECKey.isPubKeyCanonical(nonCanonicalPubkey6));
  }

  @Test
  public void testGetPrivKeyBytes() {
    ECKey key = new ECKey();
    assertNotNull(key.getPrivKeyBytes());
    assertEquals(32, key.getPrivKeyBytes().length);
  }

  @Test
  public void testEqualsObject() {
    ECKey key0 = new ECKey();
    ECKey key1 = ECKey.fromPrivate(privateKey);
    ECKey key2 = ECKey.fromPrivate(privateKey);

    assertFalse(key0.equals(key1));
    assertTrue(key1.equals(key1));
    assertTrue(key1.equals(key2));
  }

  @Test
  public void decryptAECSIC(){
    ECKey key = ECKey.fromPrivate(Hex.decode("abb51256c1324a1350598653f46aa3ad693ac3cf5d05f36eba3f495a1f51590f"));
    byte[] payload = key.decryptAES(Hex.decode("84a727bc81fa4b13947dc9728b88fd08"));
    System.out.println(Hex.toHexString(payload));
  }

  @Test
  public void testNodeId() {
    ECKey key = ECKey.fromPublicOnly(pubKey);

    assertEquals(key, ECKey.fromNodeId(key.getNodeId()));
  }
  @Test
  public void achieveDBData() throws IOException {

    // deposit.getDbManager();0x0000000000000000000000000000000000000000000000000000000000000000
    //BlockWrapper.BlockId blockId = dbManager.getHeadBlockId();
    Options options = new Options();
    options.createIfMissing(true);
    DB db = null;
    try {
      // db = factory.open(new File("/home/kay/workspace/mico/gsc/gsc-core/output-directory/database/contract"), options);
      db = factory.open(new File("/Users/mico/run_path/FullNode-vm/output-directory/database/witness"), options);

      logger.info("---------------------------------------------");
      System.out.println();
      DBIterator iterator = db.iterator();
      iterator.seekToFirst();
      int count = 0;
      while (iterator.hasNext()){
        count++;
        String key = ByteArray.toHexString(iterator.peekNext().getKey());
        String value = ByteArray.toHexString(iterator.peekNext().getValue());
        System.out.println("key:" + key+ ", value:" + value);
        iterator.next();
      }
      iterator.close();
      System.out.println("Num: " + count);
      System.out.println();
      logger.info("---------------------------------------------");

      db.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}