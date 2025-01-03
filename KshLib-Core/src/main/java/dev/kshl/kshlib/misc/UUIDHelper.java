package dev.kshl.kshlib.misc;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDHelper {
    /**
     * 00000000-0000-0000-0000-000000000000
     */
    public static final UUID ZERO = UUID.fromString("00000000-0000-0000-0000-000000000000");
    /**
     * 00000000-0000-0000-0000-000000000001
     */
    public static final UUID ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /**
     * Converts a String representation of a UUID to a UUID.
     *
     * @param uuid The UUID, with or without dashes.
     * @return The UUID, or null if uuid is null
     * @throws IllegalArgumentException if malformed
     */
    public static UUID fromString(String uuid) throws IllegalArgumentException {
        if (uuid == null) return null;
        if (uuid.length() == 32) {
            uuid = uuid.substring(0, 8) + '-' + uuid.substring(8, 12) + '-' + uuid.substring(12, 16) + '-' + uuid.substring(16, 20) + '-' + uuid.substring(20, 32);
        }
        return UUID.fromString(uuid);
    }

    public static byte[] toByteArray(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        write(uuid, buffer);
        return buffer.array();
    }

    public static void write(UUID uuid, ByteBuffer buffer) {
        if (buffer.array().length - buffer.position() < 16) {
            throw new IllegalArgumentException("Not enough space left for a UUID");
        }
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
    }

    public static UUID from(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Byte array must be 16 bytes");
        }
        return from(ByteBuffer.wrap(bytes));
    }

    public static UUID from(ByteBuffer byteBuffer) {
        if (byteBuffer.array().length - byteBuffer.position() < 16) {
            throw new IllegalArgumentException("Not enough bytes left for a UUID");
        }
        return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }
}
