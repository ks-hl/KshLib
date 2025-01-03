package dev.kshl.kshlib.encryption;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncryptionECC<K extends Key> {

    private final K key;

    private EncryptionECC(K key) {
        this.key = key;
    }

    public static EncryptionECC<PublicKey> getPublicKey(byte[] bytes) throws GeneralSecurityException {
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytes);
        return new EncryptionECC<>(keyFactory.generatePublic(publicKeySpec));
    }

    public static EncryptionECC<PrivateKey> getPrivateKey(byte[] bytes) throws GeneralSecurityException {
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytes);
        return new EncryptionECC<>(keyFactory.generatePrivate(privateKeySpec));
    }

    public static ECCPair generate() {
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256, new SecureRandom());
            kpg.initialize(new ECGenParameterSpec("secp256r1")); // P-256 curve
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        KeyPair pair = kpg.generateKeyPair();
        return new ECCPair(new EncryptionECC<>(pair.getPublic()), new EncryptionECC<>(pair.getPrivate()));
    }

    @Override
    public String toString() {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public K getKey() {
        return key;
    }

    public record ECCPair(EncryptionECC<PublicKey> publicKey, EncryptionECC<PrivateKey> privateKey) {
        public EncryptionAES combine(PublicKey otherPublicKey) throws GeneralSecurityException {
            return EncryptionECC.combine(privateKey.getKey(), otherPublicKey);
        }
    }

    public static EncryptionAES combine(PrivateKey privateKey, PublicKey otherPublicKey) throws GeneralSecurityException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(otherPublicKey, true);

        return new EncryptionAES(new SecretKeySpec(keyAgreement.generateSecret(), 0, 32, "AES"));
    }

    // ECDSA Sign Data
    public byte[] sign(byte[] data) throws GeneralSecurityException {
        if (!(key instanceof PrivateKey)) {
            throw new IllegalArgumentException("Signing requires a private key.");
        }
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign((PrivateKey) key);
        signature.update(data);
        return signature.sign();
    }

    // ECDSA Verify Signature
    public boolean verify(byte[] data, byte[] signatureBytes) throws GeneralSecurityException {
        if (!(key instanceof PublicKey)) {
            throw new IllegalArgumentException("Verification requires a public key.");
        }
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify((PublicKey) key);
        signature.update(data);
        return signature.verify(signatureBytes);
    }
}
