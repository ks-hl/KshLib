package dev.kshl.kshlib.misc.snowflake;

import dev.kshl.kshlib.misc.Bits;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Follows the <a href="https://github.com/mastodon/mastodon/blob/main/lib/mastodon/snowflake.rb">Mastodon</a> Snowflake specification.
 */
public class SnowflakeMastodon implements Snowflake {
    private static final long EPOCH = 1420070400000L; // Thursday, January 1, 2015 12:00:00 AM GMT

    private final byte[] table;
    private final byte[] secretSalt;

    private long time;
    private int counter = 0;
    private int sequenceBase;

    public SnowflakeMastodon(@Nullable String tableName, String secretSalt) {
        this.table = tableName == null ? new byte[]{} : tableName.getBytes(StandardCharsets.UTF_8);
        this.secretSalt = Objects.requireNonNull(secretSalt, "secretSalt must be not null").getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public synchronized long getNextSnowflake() {
        long now = System.currentTimeMillis();
        if (counter >= 65536) {
            while (now == time) Thread.onSpinWait();
        }
        if (now == time) {
            counter++;
        } else {
            time = now;
            counter = 0;
            sequenceBase = hash();
        }
        long snowflake = (time - EPOCH) << 16;
        snowflake |= (sequenceBase + counter) & 0xFFFF;
        return snowflake;
    }

    private int hash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            digest.update(secretSalt);
            digest.update(table);
            digest.update(Bits.toBytes(time));

            byte[] hash = digest.digest();
            return ((hash[0] & 0xFF) | ((hash[1] & 0xFF) << 8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
