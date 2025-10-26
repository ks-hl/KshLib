package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.crypto.HashSHA256;
import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.MapCache;
import dev.kshl.kshlib.misc.Pair;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SQLSessionTokenUUIDManager {
    private static final SecureRandom RND = new SecureRandom();
    public static final int TOKEN_LENGTH = 32;

    private final ConnectionManager connectionManager;
    private final String table;
    private final MapCache<UUID, SessionCache> tokens = new MapCache<>(1, TimeUnit.MINUTES);
    @Getter
    private final long sessionDuration;
    private final boolean ipSticky;
    private boolean initialized;
    private long lastPurgedExpiredTokens;

    public SQLSessionTokenUUIDManager(ConnectionManager connectionManager, String table, long sessionDuration, boolean ipSticky) {
        this.connectionManager = connectionManager;
        this.table = ConnectionManager.validateTableName(table);
        this.sessionDuration = sessionDuration;
        this.ipSticky = ipSticky;

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
        scheduler.scheduleAtFixedRate(this::purgeExpiredTokens, 5, 5, TimeUnit.MINUTES);
    }

    public void init(Connection connection) throws SQLException {
        if (initialized) throw new IllegalArgumentException("Already initialized");

        connectionManager.execute(connection, "CREATE TABLE IF NOT EXISTS " + table + " (token_id varchar(36) PRIMARY KEY, uid INT, expires BIGINT, ip TEXT, token_hash BLOB)");
        connectionManager.execute(connection, "CREATE INDEX IF NOT EXISTS idx_" + table + "_uid_token_id ON " + table + " (uid, token_id)");
        connectionManager.execute(connection, "CREATE INDEX IF NOT EXISTS idx_" + table + "_expires ON " + table + " (expires)");

        initialized = true;
    }

    /**
     * Overload for {@link #generateNew(int, String)}
     */
    public SessionToken generateNew(int uid) throws SQLException, BusyException {
        return generateNew(uid, null);
    }

    /**
     * Generates a new {@link SessionToken} under the provided UID and IP.
     *
     * @param uid The UID associated with the user
     * @param ip  The IP address of the user. Only used if ipSticky is enabled.
     * @return The newly generated token.
     */
    public SessionToken generateNew(int uid, @Nullable String ip) throws SQLException, BusyException {
        if (!initialized) throw new IllegalArgumentException("Not initialized");
        if (ipSticky && ip == null) throw new IllegalArgumentException("IP can not be null if ipSticky");
        if (!ipSticky && ip != null) throw new IllegalArgumentException("IP can not be provided if not ipSticky");

        for (int i = 0; i < 10000; i++) {
            byte[] secretBytes = new byte[TOKEN_LENGTH];
            RND.nextBytes(secretBytes);
            String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

            final UUID id = UUID.randomUUID();
            final long now = System.currentTimeMillis();
            final long expires = now + getSessionDuration();
            try {
                connectionManager.execute("INSERT INTO " + table + " (token_id,uid,expires,ip,token_hash) VALUES (?,?,?,?,?)", 3000L, id.toString(), uid, expires, ip, hash(secret));
            } catch (SQLException e) {
                if (!connectionManager.isConstraintViolation(e)) throw e;
                continue;
            }
            var token = new SessionToken(uid, id, expires, ip, secret);
            synchronized (tokens) {
                tokens.put(id, token.toCache());
            }
            return token;
        }
        throw new IllegalStateException("Failed to generate token after 10000 attempts. Buy a lottery ticket.");
    }

    /**
     * Overload for {@link #test(UUID, String, String)}
     */
    public Optional<Integer> test(String combined) throws SQLException, BusyException, IllegalArgumentException {
        var token = parseToken(combined);
        return test(token.getLeft(), token.getRight());
    }

    /**
     * Overload for {@link #test(UUID, String, String)}
     */
    public Optional<Integer> test(UUID token_id, String token) throws SQLException, BusyException {
        return test(token_id, token, null);
    }

    /**
     * Tests the validity of a token.
     *
     * @param token_id The ID of the token
     * @param token    The token value of the token
     * @return The UID of the user, or -1 if no such session.
     */
    public Optional<Integer> test(UUID token_id, String token, String ip) throws SQLException, BusyException {
        if (!initialized) throw new IllegalArgumentException("Not initialized");
        if (ipSticky && ip == null) throw new IllegalArgumentException("IP can not be null if ipSticky");
        if (!ipSticky && ip != null) throw new IllegalArgumentException("IP can not be provided if not ipSticky");

        SessionCache cached;
        synchronized (tokens) {
            cached = tokens.get(token_id);
        }
        if (cached != null) {
            if (System.currentTimeMillis() >= cached.expires()) {
                synchronized (tokens) {
                    tokens.remove(token_id);
                }
                return Optional.empty();
            }
            if (test(cached.hash(), token) && (!ipSticky || ip.equals(cached.ip()))) {
                return Optional.of(cached.uid());
            }
            return Optional.empty();
        }

        SessionCache sessionCache = connectionManager.query("SELECT uid,token_hash,expires,ip FROM " + table + " WHERE token_id=? AND expires>?", rs -> {
            if (!rs.next()) return null;
            int uid = rs.getInt("uid");
            byte[] hash = rs.getBytes("token_hash");
            long expires = rs.getLong("expires");
            if (ipSticky) {
                if (!Objects.equals(rs.getString("ip"), ip)) return null;
            }
            return new SessionCache(uid, token_id, expires, ipSticky ? ip : null, hash);
        }, 10000L, token_id.toString(), System.currentTimeMillis());

        if (sessionCache == null || sessionCache.hash() == null) return Optional.empty();

        if (!test(sessionCache.hash(), token)) return Optional.empty();
        synchronized (tokens) {
            tokens.put(token_id, sessionCache);
        }
        return Optional.of(sessionCache.uid());
    }

    private boolean test(byte[] hash, String token) {
        return MessageDigest.isEqual(hash, hash(token));
    }

    private static byte[] hash(String token) {
        try {
            return HashSHA256.hash(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Not expected
        }
    }

    private void purgeExpiredTokens() {
        if (!connectionManager.isReady() || connectionManager.isClosed()) return;

        long time = System.currentTimeMillis();
        try {
            connectionManager.execute("DELETE FROM " + table + " WHERE expires<?", 500L, time);
        } catch (SQLException | BusyException ignored) {
        }
        synchronized (tokens) {
            tokens.values().removeIf(token -> token.expires() < time);
        }
    }

    public static Pair<UUID, String> parseToken(String combined) throws IllegalArgumentException {
        String[] split = combined.split(":");
        if (split.length != 2) throw new IllegalArgumentException("Invalid token format");
        return new Pair<>(UUID.fromString(split[0]), split[1]);
    }

    public record SessionToken(int uid, UUID token_id, long expires, @Nullable String ip, String token) {
        public SessionCache toCache() {
            return new SessionCache(uid, token_id, expires, ip, hash(token));
        }

        @Override
        @Nonnull
        public String toString() {
            return token_id + ":" + token;
        }
    }

    public record SessionCache(int uid, UUID token_id, long expires, @Nullable String ip, byte[] hash) {
    }

    public String getTableName() {
        return table;
    }

    public void remove(int uid) throws SQLException, BusyException {
        Set<UUID> toRemove = new HashSet<>();
        connectionManager.execute(connection -> {
            connectionManager.query(connection, "SELECT token_id FROM " + table + " WHERE uid=?", rs -> {
                while (rs.next()) {
                    try {
                        toRemove.add(UUID.fromString(rs.getString(1)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }, uid);
            connectionManager.execute(connection, "DELETE FROM " + table + " WHERE uid=?", uid);
        }, 3000L);
        synchronized (tokens) {
            toRemove.forEach(tokens::remove);
        }
    }

    public void remove(int uid, UUID token_id) throws SQLException, BusyException {
        connectionManager.executeReturnRows("DELETE FROM " + table + " WHERE uid=? AND token_id=?", 10000L, uid, token_id.toString());
        synchronized (tokens) {
            tokens.remove(token_id);
        }
    }
}
