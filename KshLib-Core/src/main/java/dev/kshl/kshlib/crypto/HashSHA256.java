package dev.kshl.kshlib.crypto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashSHA256 {
    public static byte[] hash(byte[] bytes) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    public static byte[] hash(File file) throws IOException, NoSuchAlgorithmException {
        return hash(Files.readAllBytes(file.toPath()));
    }
}
