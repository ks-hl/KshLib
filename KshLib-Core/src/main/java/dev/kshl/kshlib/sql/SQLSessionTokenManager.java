package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.crypto.CodeGenerator;
import dev.kshl.kshlib.crypto.HashPBKDF2;
import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.MapCache;
import lombok.Getter;

import javax.annotation.Nullable;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Deprecated
public class SQLSessionTokenManager {
    public static final int TOKEN_LENGTH = 48;
    private static final int HASH_SALT_BYTES = 16;
    private static final int HASH_KEY_BYTES = 32;
    private final ConnectionManager connectionManager;
    private final String table;
    private final MapCache<Integer, SessionToken> tokens = new MapCache<>(60000L, 10000L, true);
    @Getter
    private final long sessionDuration;
    private final boolean ipSticky;
    private boolean initialized;
    private long lastPurgedExpiredTokens;

    public SQLSessionTokenManager(ConnectionManager connectionManager, String table, long sessionDuration, boolean ipSticky) throws NoSuchAlgorithmException {
        this.connectionManager = connectionManager;
        this.table = ConnectionManager.validateTableName(table);
        this.sessionDuration = sessionDuration;
        this.ipSticky = ipSticky;

        HashPBKDF2.checkAlgorithm();
    }

    public void init(Connection connection) throws SQLException {
        if (initialized) throw new IllegalArgumentException("Already initialized");

        connectionManager.execute(connection, "CREATE TABLE IF NOT EXISTS " + table + " (token_id INT PRIMARY KEY, uid INT, expires BIGINT, ip TEXT, hash BLOB)");
        purgeExpiredTokens(connection);
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

        String secret = CodeGenerator.generateSecret(TOKEN_LENGTH, true, false, false);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 10000; i++) {
            try {
                int id = random.nextInt(10000000, Integer.MAX_VALUE);
                return put(uid, ip, id, secret);
            } catch (SQLException e) {
                if (!connectionManager.isConstraintViolation(e)) throw e;
            }
        }
        throw new IllegalStateException("Failed to generate token after 10000 attempts. Buy a lottery ticket.");
    }

    /**
     * Overload for {@link #test(int, String, String)}
     */
    public int test(int token_id, String token) throws SQLException, BusyException {
        return test(token_id, token, null);
    }

    /**
     * Tests the validity of a token.
     *
     * @param token_id The ID of the token
     * @param token    The token value of the token
     * @return The UID of the user, or -1 if no such session.
     */
    public int test(int token_id, String token, String ip) throws SQLException, BusyException {
        if (!initialized) throw new IllegalArgumentException("Not initialized");
        if (ipSticky && ip == null) throw new IllegalArgumentException("IP can not be null if ipSticky");

        SessionToken cached;
        synchronized (tokens) {
            cached = tokens.get(token_id);
        }
        if (cached != null) {
            if (System.currentTimeMillis() >= cached.expires()) {
                synchronized (tokens) {
                    tokens.remove(token_id);
                }
                return -1;
            }
            return cached.token().equals(token) && (!ipSticky || ip.equals(cached.ip())) ? cached.uid() : -1;
        }

        try {
            connectionManager.execute(this::purgeExpiredTokens, 1000L);
        } catch (BusyException ignored) {
        }

        AtomicReference<byte[]> hash = new AtomicReference<>();
        SessionToken sessionToken = connectionManager.query("SELECT uid,hash,expires,ip FROM " + table + " WHERE token_id=? AND expires>?", rs -> {
            if (!rs.next()) return null;
            int uid = rs.getInt(1);
            hash.set(connectionManager.getBlob(rs, 2));
            long expires = rs.getLong(3);
            if (ipSticky) {
                if (!Objects.equals(rs.getString(4), ip)) return null;
            }
            return new SessionToken(uid, token_id, expires, ipSticky ? ip : null, token);
        }, 10000L, token_id, System.currentTimeMillis());

        if (sessionToken == null || hash.get() == null || hash.get().length < (HASH_KEY_BYTES + HASH_SALT_BYTES))
            return -1;

        try {
            if (!HashPBKDF2.test(hash.get(), token, 1000)) return -1;
        } catch (NoSuchAlgorithmException e) {
            return -1; // Not expected, validated on instance creation
        }
        synchronized (tokens) {
            tokens.put(token_id, sessionToken);
        }
        return sessionToken.uid();
    }

    private SessionToken put(int uid, String ip, int token_id, String password) throws SQLException, BusyException {
        if (!initialized) throw new IllegalArgumentException("Not initialized");
        if (ipSticky && ip == null) throw new IllegalArgumentException("IP can not be null if ipSticky");

        final long now = System.currentTimeMillis();
        final long expires = now + getSessionDuration();
        try {
            connectionManager.execute("INSERT INTO " + table + " (token_id,uid,expires,ip,hash) VALUES (?,?,?,?,?)", 3000L, token_id, uid, expires, ipSticky ? ip : null, HashPBKDF2.hash(password, 1000));
        } catch (NoSuchAlgorithmException e) {
            // unexpected, verified at creation.
            throw new RuntimeException(e);
        }
        try {
            connectionManager.execute(this::purgeExpiredTokens, 1000L);
        } catch (BusyException ignored) {
        }
        return new SessionToken(uid, token_id, expires, ip, password);
    }

    private void purgeExpiredTokens(Connection connection) throws SQLException {
        if (System.currentTimeMillis() - lastPurgedExpiredTokens < 60000L * 15L) return;
        lastPurgedExpiredTokens = System.currentTimeMillis();

        long time = System.currentTimeMillis();
        connectionManager.execute(connection, "DELETE FROM " + table + " WHERE expires<?", time);
        synchronized (tokens) {
            tokens.values().removeIf(token -> token.expires() < time);
        }
    }

    public record SessionToken(int uid, int token_id, long expires, @Nullable String ip, String token) {
    }

    public String getTableName() {
        return table;
    }

    public boolean remove(int uid) throws SQLException, BusyException {
        return connectionManager.executeReturnRows("DELETE FROM " + table + " WHERE uid=?", 10000L, uid) > 0;
    }

    public boolean remove(int uid, int token_id) throws SQLException, BusyException {
        return connectionManager.executeReturnRows("DELETE FROM " + table + " WHERE uid=? AND token_id=?", 10000L, uid, token_id) > 0;
    }
}
