package dev.kshl.kshlib.encryption;

import dev.kshl.kshlib.function.ThrowingConsumer;
import dev.kshl.kshlib.function.ThrowingSupplier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class Handshake {
    protected final ThrowingConsumer<byte[], Exception> send;
    protected final ThrowingSupplier<byte[], Exception> receive;
    protected final EncryptionRSA privateKey;
    protected final EncryptionRSA publicKey;
    private static final Map<UUID, Set<Long>> burnedTimes = new HashMap<>();

    protected Handshake(ThrowingConsumer<byte[], Exception> send, ThrowingSupplier<byte[], Exception> receive, EncryptionRSA privateKey, EncryptionRSA publicKey) {
        this.send = send;
        this.receive = receive;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    protected byte[] sign(byte[] data) throws GeneralSecurityException {
        byte[] signature = privateKey.encrypt(HashSHA256.hash(data));

        ByteBuffer buffer = ByteBuffer.allocate(data.length + signature.length + 4);

        buffer.putShort((short) data.length);
        buffer.putShort((short) signature.length);

        buffer.put(data);
        buffer.put(signature);

        return buffer.array();
    }

    protected byte[] validate(ByteBuffer byteBuffer) throws GeneralSecurityException {

        byte[] value = new byte[byteBuffer.getShort()];
        byte[] signature = new byte[byteBuffer.getShort()];

        byteBuffer.get(value);
        byteBuffer.get(signature);

        if (!Arrays.equals(HashSHA256.hash(value), publicKey.decrypt(signature))) {
            throw new IllegalStateException("Sent value does not match signature");
        }

        return value;
    }

    protected static boolean isValid(UUID uuid, long timeStamp) {
        if (Math.abs(System.currentTimeMillis() - timeStamp) > 15000) return false;
        Set<Long> burnedTimesSet = burnedTimes.computeIfAbsent(uuid, u -> new HashSet<>());
        if (!burnedTimesSet.add(timeStamp)) return false;

        burnedTimes.values().forEach(set -> set.removeIf(t -> System.currentTimeMillis() - t > 5000));
        burnedTimes.values().removeIf(Set::isEmpty);
        return true;
    }

    public abstract EncryptionAES handshake() throws IOException, GeneralSecurityException;
}
