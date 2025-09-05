package dev.kshl.kshlib.crypto;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class HashPBKDF2 {
    private static final int SALT_BYTES = 32;
    private static final int KEY_BYTES = 64;

    public static void checkAlgorithm() throws NoSuchAlgorithmException {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
    }

    public static byte[] hash(String password, int iterationCount) throws NoSuchAlgorithmException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);

        return hash(password, salt, iterationCount);
    }

    public static byte[] hash(String password, byte[] salt, int iterationCount) throws NoSuchAlgorithmException {
        if (salt.length != SALT_BYTES) throw new IllegalArgumentException("Invalid salt length");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, KEY_BYTES * 8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");

        byte[] hash;
        try {
            hash = factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            // We just generated this spec, if it's wrong, it's a problem internal to this method.
            throw new RuntimeException(e);
        }
        byte[] out = new byte[salt.length + hash.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = i < salt.length ? salt[i] : hash[i - salt.length];
        }

        return out;
    }

    public static boolean test(byte[] hash, String password, int iterationCount) throws NoSuchAlgorithmException {
        if (hash == null || hash.length < (KEY_BYTES + SALT_BYTES)) return false;

        byte[] salt = new byte[SALT_BYTES];
        System.arraycopy(hash, 0, salt, 0, salt.length);
        return Arrays.equals(hash, hash(password, salt, iterationCount));
    }
}
