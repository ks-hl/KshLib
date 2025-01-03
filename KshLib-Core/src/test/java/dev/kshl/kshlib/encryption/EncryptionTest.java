package dev.kshl.kshlib.encryption;


import dev.kshl.kshlib.function.ThrowingUnaryOperator;
import dev.kshl.kshlib.misc.Bits;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EncryptionTest {
    private static final String msg = "Hello world";

    @Test
    public void testAES() throws IllegalBlockSizeException, BadPaddingException {
        EncryptionAES aes1 = new EncryptionAES(EncryptionAES.generateRandomKey());
        EncryptionAES aes2 = new EncryptionAES(EncryptionAES.generateRandomKey());
        for (int i = 0; i < 100; i++) {
            assertEquals(msg, new String(aes1.decrypt(aes1.encrypt(msg.getBytes()))));
            assertEquals(msg, new String(aes2.decrypt(aes2.encrypt(msg.getBytes()))));
        }
        byte[] cipherText1 = aes1.encrypt(msg.getBytes());
        byte[] cipherText2 = aes1.encrypt(msg.getBytes());
        assert !Arrays.equals(cipherText1, 16, cipherText1.length - 16, cipherText2, 16, cipherText2.length);
        assertThrows(GeneralSecurityException.class, () -> aes1.decrypt(aes2.encrypt(msg.getBytes())));
    }

    @Test
    public void testAesWithDifferentSizes() throws IllegalBlockSizeException, BadPaddingException {
        // Test with empty, small, and large inputs
        String[] testMessages = {"", "a", new String(new char[10000]).replace('\0', 'a')};
        for (String testMsg : testMessages) {
            EncryptionAES aes = new EncryptionAES(EncryptionAES.generateRandomKey());
            byte[] encrypted = aes.encrypt(testMsg.getBytes());
            String decrypted = new String(aes.decrypt(encrypted));
            assertEquals(testMsg, decrypted);
        }
    }

    @Test
    public void testRSA() throws GeneralSecurityException {
        EncryptionRSA.RSAPair pair = EncryptionRSA.generate();
        for (int i = 0; i < 100; i++) {
            assertEquals(msg, new String(pair.privateKey().decrypt(pair.publicKey().encrypt(msg.getBytes()))));
            assertEquals(msg, new String(pair.publicKey().decrypt(pair.privateKey().encrypt(msg.getBytes()))));
        }
        assertThrows(GeneralSecurityException.class, () -> pair.publicKey().decrypt(pair.publicKey().encrypt(msg.getBytes())));
        assertThrows(GeneralSecurityException.class, () -> pair.privateKey().decrypt(pair.privateKey().encrypt(msg.getBytes())));
    }

    @Test
    public void testDH() throws GeneralSecurityException, IOException {
        AlgorithmParameters params = EncryptionDH.generateParameters(1024); // Smaller key used for test efficiency

        KeyPair alice_key = EncryptionDH.generate(params);
        KeyPair bob_key = EncryptionDH.generate(EncryptionDH.generateParameters(params.getEncoded()));

        SecretKey key = EncryptionDH.combine(alice_key.getPrivate(), bob_key.getPublic());

        assertEquals(msg, new String(EncryptionDH.decrypt(key, EncryptionDH.encrypt(key, msg.getBytes()))));
    }

    @Test
    public void testDhParameterConsistency() throws GeneralSecurityException, IOException {
        AlgorithmParameters params1 = EncryptionDH.generateParameters(1024); // Smaller key used for test efficiency
        AlgorithmParameters params2 = EncryptionDH.generateParameters(params1.getEncoded());

        KeyPair alice_key = EncryptionDH.generate(params1);
        KeyPair bob_key = EncryptionDH.generate(params2);

        SecretKey key1 = EncryptionDH.combine(alice_key.getPrivate(), bob_key.getPublic());
        SecretKey key2 = EncryptionDH.combine(bob_key.getPrivate(), alice_key.getPublic());

        assertEquals(key1, key2);
    }

    @Test
    public void testECC() throws GeneralSecurityException {
        // Step 1: Server and Client generate their ECC key pairs
        EncryptionECC.ECCPair serverKeys = EncryptionECC.generate();
        EncryptionECC.ECCPair clientKeys = EncryptionECC.generate();

        System.out.println(serverKeys.privateKey());
        System.out.println(serverKeys.publicKey());

        // Step 2: Server sends its public key to the Client
        EncryptionECC<PublicKey> serverPublicKey = serverKeys.publicKey();

        EncryptionRSA.RSAPair rsa1 = EncryptionRSA.generate();
        EncryptionRSA.RSAPair rsa2 = EncryptionRSA.generate();

        EncryptionECC<PublicKey> serverPublicKeyEncryptedUnencrypted = EncryptionECC.getPublicKey(
                rsa1.publicKey().decrypt(
                        rsa1.privateKey().encrypt(
                                serverPublicKey.getKey().getEncoded()
                        )
                )
        );

        // Step 3: Client sends its public key to the Server
        EncryptionECC<PublicKey> clientPublicKey = clientKeys.publicKey();

        // Step 4: Server and Client generate the shared secret using the other's public key
        EncryptionAES serverAES = serverKeys.combine(clientPublicKey.getKey());
        EncryptionAES clientAES = clientKeys.combine(serverPublicKeyEncryptedUnencrypted.getKey());

        // Step 5: Both Server and Client should now have the same shared secret
        assertArrayEquals(serverAES.encodeKey(), clientAES.encodeKey(), "Keys do not match!");

        // Step 7: Encrypt a message on the Server side
        String message = UUID.randomUUID().toString().repeat(10);
        byte[] plainText = message.getBytes();
        byte[] cipherText = clientAES.encrypt(message.getBytes());
        System.out.println("Plaintext: " + plainText.length + " bytes");
        System.out.println("Cipher: " + cipherText.length + " bytes");

        // Step 8: Decrypt the message on the Client side
        byte[] decryptedMessage = serverAES.decrypt(cipherText);

        // Step 9: Verify that the decrypted message matches the original
        assertArrayEquals(plainText, decryptedMessage, "Decrypted message does not match the original!");
    }

    @Test
    public void testMessageTampering() throws GeneralSecurityException {
        final byte[] msg = "This is a message with no significant meaning".getBytes();

        for (int i = 0; i < 100; i++) {
            EncryptionAES encryptionAES = new EncryptionAES(EncryptionAES.generateRandomKey());
            testMessageTampering(encryptionAES::encrypt, encryptionAES::decrypt, msg);

        }

        for (int i = 0; i < 2; i++) {
            EncryptionRSA.RSAPair encryptionRSA = EncryptionRSA.generate();
            testMessageTampering(encryptionRSA.publicKey()::encrypt, encryptionRSA.privateKey()::decrypt, msg);
            testMessageTampering(encryptionRSA.privateKey()::encrypt, encryptionRSA.publicKey()::decrypt, msg);
        }

        for (int i = 0; i < 2; i++) {
            AlgorithmParameters algorithmParameters = EncryptionDH.generateParameters(1024); // Smaller key used for test efficiency
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            KeyPair d1 = EncryptionDH.generate(algorithmParameters);
            KeyPair d2 = EncryptionDH.generate(algorithmParameters);
            SecretKey combined = EncryptionDH.combine(d1.getPrivate(), d2.getPublic());
            EncryptionAES encryptionAES = EncryptionDH.deriveAESKey(combined, salt);
            testMessageTampering(encryptionAES::encrypt, encryptionAES::decrypt, msg);
        }

        for (int i = 0; i < 10; i++) {
            EncryptionECC.ECCPair ecc1 = EncryptionECC.generate();
            EncryptionECC.ECCPair ecc2 = EncryptionECC.generate();
            EncryptionAES encryptionAES = EncryptionECC.combine(ecc1.privateKey().getKey(), ecc2.publicKey().getKey());
            testMessageTampering(encryptionAES::encrypt, encryptionAES::decrypt, msg);
        }
    }

    private static void testMessageTampering(ThrowingUnaryOperator<byte[], GeneralSecurityException> encrypt,
                                             ThrowingUnaryOperator<byte[], GeneralSecurityException> decrypt,
                                             byte[] msg) throws GeneralSecurityException {

        testMessageTamperingModify(encrypt, decrypt, msg, b -> {
            b[0] = Bits.setBit(b[0], 0, !Bits.getBit(b[0], 0));
            return b;
        });
        testMessageTamperingModify(encrypt, decrypt, msg, b -> {
            int i = b.length - 1;
            b[i] = Bits.setBit(b[i], 7, !Bits.getBit(b[i], 7));
            return b;
        });
        testMessageTamperingModify(encrypt, decrypt, msg, b -> {
            int i = b.length / 2;
            b[i] = Bits.setBit(b[i], 4, !Bits.getBit(b[i], 4));
            return b;
        });
        testMessageTamperingModify(encrypt, decrypt, msg, b -> Arrays.copyOfRange(b, 0, b.length - 1));
        testMessageTamperingModify(encrypt, decrypt, msg, b -> {
            byte[] out = new byte[b.length + 1];
            System.arraycopy(b, 0, out, 0, b.length);
            return out;
        });
    }

    private static void testMessageTamperingModify(ThrowingUnaryOperator<byte[], GeneralSecurityException> encrypt,
                                                   ThrowingUnaryOperator<byte[], GeneralSecurityException> decrypt,
                                                   byte[] msg,
                                                   UnaryOperator<byte[]> modify) throws GeneralSecurityException {
        byte[] encrypted = modify.apply(encrypt.apply(msg));
        assertThrows(GeneralSecurityException.class, () -> decrypt.apply(encrypted));
    }
}
