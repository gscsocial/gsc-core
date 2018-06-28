package org.gsc.crpto;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.config.Parameter;
import org.gsc.crypto.ECKey;
import org.gsc.crypto.jce.ECKeyPairGenerator;
import org.gsc.crypto.jce.SpongyCastleProvider;
import org.gsc.protos.Protocol;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.util.List;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

@Slf4j
public class ECKeyTest {

    private static final SecureRandom secureRandom = new SecureRandom();

    private String privString = "a4279a044bec3080645df9d3313933c01544b9cae072ea8405ade4df147b05de";
    private BigInteger privateKey = new BigInteger(privString, 16);

    private String pubString = "0440c7393de17fa0b19485bca832dd502a3bf8c978ea427c2391f701411067483ba5e8713bbb18537efdc88cd456343e053f38cd65bf484ab16db443a1608ec046";
    private String compressedPubString = "0240c7393de17fa0b19485bca832dd502a3bf8c978ea427c2391f701411067483b";
    private byte[] pubKey = Hex.decode(pubString);
    private byte[] compressedPubKey = Hex.decode(compressedPubString);
    private String address = "41fd88e2c9ca5a1fce25354eed46548681e415e1a2";

    private String exampleMessage = "This is an example of a signed message.";
    private String sigBase64 = "G+cceGhe+OYi4UijWyH0i1mYzdUvI/ZUn6LK3R0unkfVUqGF37Yvnu/OHP4IeFSySW0COnc3XbLZzAz056Dq/hs=";
    private String signatureHex = "e71c78685ef8e622e148a35b21f48b5998cdd52f23f6549fa2cadd1d2e9e47d552a185dfb62f9eefce1cfe087854b2496d023a77375db2d9cc0cf4e7a0eafe1b00";

    @Before
    public void init(){
        Security.addProvider(new BouncyCastleProvider());
        SpongyCastleProvider.getInstance();
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(-351262686, ECKey.fromPrivate(privateKey).hashCode());
    }

    @Test
    public void testECKey() {
        ECKey key = new ECKey();
        assertTrue(key.isPubKeyCanonical());
        assertNotNull(key.getPubKey());
        assertNotNull(key.getPrivKeyBytes());
        logger.info(Hex.toHexString(key.getPrivKeyBytes()) + " :Generated privkey");
        logger.info(Hex.toHexString(key.getPubKey()) + " :Generated pubkey");
    }

    @Test
    public void testFromPrivateKey() {
        ECKey key = ECKey.fromPrivate(privateKey);
        assertTrue(key.isPubKeyCanonical());
        assertTrue(key.hasPrivKey());
        assertArrayEquals(pubKey, key.getPubKey());
        assertEquals(address,Hex.toHexString(key.getAddress()));
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

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPrivateKey2() throws Exception {
        ECKey.fromPrivate(new byte[32]);
        fail("Expecting an IllegalArgumentException for using an non EC private key");
    }

    @Test
    public void testIsPubKeyOnly() {
        ECKey key = ECKey.fromPublicOnly(pubKey);
        assertTrue(key.isPubKeyCanonical());
        assertTrue(key.isPubKeyOnly());
        assertArrayEquals(key.getPubKey(), pubKey);
        assertEquals(address,Hex.toHexString(key.getAddress()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSignIncorrectInputSize() {
        ECKey key = new ECKey();
        String message = "The quick brown fox jumps over the lazy dog.";
        ECKey.ECDSASignature sig = key.doSign(message.getBytes());
        fail("Expecting an IllegalArgumentException for a non 32-byte input");
    }

    @Test(expected = ECKey.MissingPrivateKeyException.class)
    public void testSignWithPubKeyOnly() {
        ECKey key = ECKey.fromPublicOnly(pubKey);
        String message = "The quick brown fox jumps over the lazy dog.";
        byte[] input = ECKey.sha3(message.getBytes());
        ECKey.ECDSASignature sig = key.doSign(input);
        fail("Expecting an MissingPrivateKeyException for a public only ECKey");
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
        System.out.println(Hex.toHexString(pubFromPriv));
        assertArrayEquals(compressedPubKey, pubFromPriv);
    }

    @Test
    public void testGetAddress() {
        ECKey key = ECKey.fromPublicOnly(pubKey);
        assertArrayEquals(Hex.decode(address), key.getAddress());
    }

    @Test
    public void testToString() {
        ECKey key = ECKey.fromPrivate(BigInteger.TEN); // An example private key.
        assertEquals("pub:04a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7", key.toString());
    }

    @Test
    public void testEthereumSign() throws IOException {
        ECKey key = ECKey.fromPrivate(privateKey);
        System.out.println("Secret\t: " + Hex.toHexString(key.getPrivKeyBytes()));
        System.out.println("Pubkey\t: " + Hex.toHexString(key.getPubKey()));
        System.out.println("Data\t: " + exampleMessage);
        byte[] messageHash = ECKey.sha3(exampleMessage.getBytes());
        ECKey.ECDSASignature signature = key.sign(messageHash);
        String output = signature.toBase64();
        System.out.println("Signtr\t: " + output + " (Base64, length: " + output.length() + ")");
        assertEquals(sigBase64, output);
    }

    /**
     * Verified via https://etherchain.org/verify/signature
     */
    @Test
    public void testEthereumSignToHex() {
        ECKey key = ECKey.fromPrivate(privateKey);
        byte[] messageHash = ECKey.sha3(exampleMessage.getBytes());
        ECKey.ECDSASignature signature = key.sign(messageHash);
        String output = signature.toHex();
        System.out.println("Signature\t: " + output + " (Hex, length: " + output.length() + ")");
        assertEquals(signatureHex, output);
    }

    @Test
    public void testVerifySignature1() {
        ECKey key = ECKey.fromPublicOnly(pubKey);
        BigInteger r = new BigInteger("28157690258821599598544026901946453245423343069728565040002908283498585537001");
        BigInteger s = new BigInteger("30212485197630673222315826773656074299979444367665131281281249560925428307087");
        ECKey.ECDSASignature sig = ECKey.ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), (byte) 28);
        key.verify(ECKey.sha3(exampleMessage.getBytes()), sig);
    }

    @Test
    public void testVerifySignature2() {
        BigInteger r = new BigInteger("c52c114d4f5a3ba904a9b3036e5e118fe0dbb987fe3955da20f2cd8f6c21ab9c", 16);
        BigInteger s = new BigInteger("6ba4c2874299a55ad947dbc98a25ee895aabf6b625c26c435e84bfd70edf2f69", 16);
        ECKey.ECDSASignature sig = ECKey.ECDSASignature.fromComponents(r.toByteArray(), s.toByteArray(), (byte) 0x1b);
        byte[] rawtx = Hex.decode("f82804881bc16d674ec8000094cd2a3d9f938e13cd947ec05abc7fe734df8dd8268609184e72a0006480");
        byte[] rawHash = ECKey.sha3(rawtx);
        byte[] address = Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826");
        try {
            ECKey key = ECKey.signatureToKey(rawHash, sig);

            System.out.println("Signature public key\t: " + Hex.toHexString(key.getPubKey()));
            System.out.println("Sender is\t\t: " + Hex.toHexString(key.getAddress()));

            assertEquals(key, ECKey.signatureToKey(rawHash, sig.toBase64()));
            assertEquals(key, ECKey.recoverFromSignature(0, sig, rawHash));
            assertArrayEquals(key.getPubKey(), ECKey.recoverPubBytesFromSignature(0, sig, rawHash));


            assertArrayEquals(address, key.getAddress());
            assertArrayEquals(address, ECKey.signatureToAddress(rawHash, sig));
            assertArrayEquals(address, ECKey.signatureToAddress(rawHash, sig.toBase64()));
            assertArrayEquals(address, ECKey.recoverAddressFromSignature(0, sig, rawHash));

            assertTrue(key.verify(rawHash, sig));
        } catch (SignatureException e) {
            fail();
        }
    }



    @Test
    public void testSValue() throws Exception {
        // Check that we never generate an S value that is larger than half the curve order. This avoids a malleability
        // issue that can allow someone to change a transaction [hash] without invalidating the signature.
        final int ITERATIONS = 10;
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(ITERATIONS));
        List<ListenableFuture<ECKey.ECDSASignature>> sigFutures = Lists.newArrayList();
        final ECKey key = new ECKey();
        for (byte i = 0; i < ITERATIONS; i++) {
            final byte[] hash = ECKey.sha3(new byte[]{i});
            sigFutures.add(executor.submit(() -> key.doSign(hash)));
        }
        List<ECKey.ECDSASignature> sigs = Futures.allAsList(sigFutures).get();
        for (ECKey.ECDSASignature signature : sigs) {
            assertTrue(signature.s.compareTo(ECKey.HALF_CURVE_ORDER) <= 0);
        }
        final ECKey.ECDSASignature duplicate = new ECKey.ECDSASignature(sigs.get(0).r, sigs.get(0).s);
        assertEquals(sigs.get(0), duplicate);
        assertEquals(sigs.get(0).hashCode(), duplicate.hashCode());
    }

    @Test
    public void testSignVerify() {
        ECKey key = ECKey.fromPrivate(privateKey);
        String message = "This is an example of a signed message.";
        byte[] input = ECKey.sha3(message.getBytes());
        ECKey.ECDSASignature sig = key.sign(input);
        assertTrue(sig.validateComponents());
        assertTrue(key.verify(input, sig));
    }

    private void testProviderRoundTrip(Provider provider) throws Exception {
        ECKey key = new ECKey(provider, secureRandom);
        String message = "The quick brown fox jumps over the lazy dog.";
        byte[] input = ECKey.sha3(message.getBytes());
        ECKey.ECDSASignature sig = key.sign(input);
        assertTrue(sig.validateComponents());
        assertTrue(key.verify(input, sig));
    }

    @Test
    public void testSunECRoundTrip() throws Exception {
        Provider provider = Security.getProvider("SunEC");
        if (provider != null) {
            testProviderRoundTrip(provider);
        } else {
            System.out.println("Skip test as provider doesn't exist. " +
                    "Must be OpenJDK which could be shipped without 'SunEC'");
        }
    }

    @Test
    public void testSpongyCastleRoundTrip() throws Exception {
        testProviderRoundTrip(SpongyCastleProvider.getInstance());
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
    public void keyRecovery() throws Exception {
        ECKey key = new ECKey();
        String message = "Hello World!";
        byte[] hash = Sha256Hash.hash(message.getBytes());
        ECKey.ECDSASignature sig = key.doSign(hash);
        key = ECKey.fromPublicOnly(key.getPubKeyPoint());
        boolean found = false;
        for (int i = 0; i < 4; i++) {
            ECKey key2 = ECKey.recoverFromSignature(i, sig, hash);
            checkNotNull(key2);
            if (key.equals(key2)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testSignedMessageToKey() throws SignatureException {
        byte[] messageHash = ECKey.sha3(exampleMessage.getBytes());
        ECKey key = ECKey.signatureToKey(messageHash, sigBase64);
        assertNotNull(key);
        assertArrayEquals(pubKey, key.getPubKey());
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
}
