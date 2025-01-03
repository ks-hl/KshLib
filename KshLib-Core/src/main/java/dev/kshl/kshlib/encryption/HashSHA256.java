package dev.kshl.kshlib.encryption;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashSHA256 {
    public static byte[] hash(byte[] bytes) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }
}
