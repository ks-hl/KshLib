package dev.kshl.kshlib.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncryptionDH {
    private static final String ALGO = "DH";

    public static PublicKey getPublicKey(byte[] bytes) throws InvalidKeySpecException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytes);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public static AlgorithmParameters generateParameters(int keySize) {
        try {
            AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance(ALGO);
            paramGen.init(keySize);
            return paramGen.generateParameters();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static AlgorithmParameters generateParameters(byte[] encoded) throws IOException {
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance(ALGO);
            params.init(encoded);
            return params;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyPair generate(AlgorithmParameters params) throws GeneralSecurityException {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGO);
            keyGen.initialize(params.getParameterSpec(DHParameterSpec.class));
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static SecretKey combine(PrivateKey private1, PublicKey public1) throws InvalidKeyException {
        try {
            KeyAgreement ka = KeyAgreement.getInstance(ALGO);
            ka.init(private1);
            ka.doPhase(public1, true);
            byte[] sharedSecret = ka.generateSecret();
            return new SecretKeySpec(sharedSecret, 0, 32, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encrypt(SecretKey key, byte[] bytes) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(bytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decrypt(SecretKey key, byte[] bytes) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(bytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static EncryptionAES deriveAESKey(SecretKey secretKey, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(Base64.getEncoder().encodeToString(secretKey.getEncoded()).toCharArray(), salt, 100000, 256); // 256-bit AES key
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey aesKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        return new EncryptionAES(aesKey);
    }
}