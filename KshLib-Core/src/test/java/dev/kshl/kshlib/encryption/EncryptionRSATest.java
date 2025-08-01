package dev.kshl.kshlib.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncryptionRSATest {

    private EncryptionRSA.RSAPair keyPair;
    private EncryptionRSA publicKey;
    private EncryptionRSA privateKey;
    private final byte[] message = "TLS certificate fingerprint".getBytes();

    @BeforeEach
    public void setUp() {
        keyPair = EncryptionRSA.generate();
        publicKey = keyPair.publicKey();
        privateKey = keyPair.privateKey();
    }

    @Test
    public void testSignAndValidateSignature_Success() throws Exception {
        byte[] signature = privateKey.sign(message);

        // correct message + signature
        publicKey.validateSignature(message, message, signature);
    }

    @Test
    public void testValidateSignature_FailsOnModifiedMessage() throws Exception {
        byte[] signature = privateKey.sign(message);
        byte[] modifiedMessage = "Fake fingerprint".getBytes();

        GeneralSecurityException ex = assertThrows(GeneralSecurityException.class, () ->
                publicKey.validateSignature(modifiedMessage, message, signature));

        assertTrue(ex.getMessage().contains("TLS cert fingerprint mismatch"));
    }

    @Test
    public void testValidateSignature_FailsOnWrongSignature() throws Exception {
        // Sign using another key pair
        EncryptionRSA.RSAPair wrongKeys = EncryptionRSA.generate();
        byte[] badSignature = wrongKeys.privateKey().sign(message);

        GeneralSecurityException ex = assertThrows(GeneralSecurityException.class, () ->
                publicKey.validateSignature(message, message, badSignature));

        assertTrue(ex.getMessage().contains("TLS fingerprint signature invalid"));
    }

    @Test
    public void testSignFailsWithPublicKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                publicKey.sign(message));
        assertTrue(ex.getMessage().contains("Can not sign with a public key"));
    }

    @Test
    public void testValidateFailsWithPrivateKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                privateKey.validateSignature(message, message, new byte[0]));
        assertTrue(ex.getMessage().contains("Can not validate signature with a private key"));
    }
}
