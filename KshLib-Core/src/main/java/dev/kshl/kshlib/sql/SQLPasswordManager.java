package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.crypto.HashPBKDF2;
import dev.kshl.kshlib.exceptions.BusyException;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class SQLPasswordManager {
    protected final ConnectionManager connectionManager;
    private final String table;
    private final Type type;
    private boolean initialized;

    public enum Type {
        /**
         * Used for user passwords. Iteration count is 100000
         */
        PASSWORD(100000),
        /**
         * Used for tokens which are able to be revoked and easily changed. Iteration count is 1000
         */
        TOKEN(1000);

        public final int iterationCount;

        Type(int iterationCount) {
            this.iterationCount = iterationCount;
        }
    }


    public SQLPasswordManager(ConnectionManager connectionManager, String table, Type type) throws NoSuchAlgorithmException {
        this.connectionManager = connectionManager;
        this.table = table;
        this.type = type;

        HashPBKDF2.checkAlgorithm();
    }

    public void init(Connection connection) throws SQLException {
        if (initialized) throw new IllegalArgumentException("Already initialized");

        connectionManager.execute(connection, getCreateTableStatement());
        try {
            connectionManager.execute(connection, "ALTER TABLE " + table + " ADD COLUMN expires BIGINT");
        } catch (SQLException ignored) {
        }
        initialized = true;
    }

    protected String getCreateTableStatement() {
        return "CREATE TABLE IF NOT EXISTS " + table + " (created BIGINT, uid INT PRIMARY KEY, last_changed BIGINT, expires BIGINT, hash BLOB)";
    }

    /**
     * Sets the password for the given user
     *
     * @param expiresAt The time after which this password will be ignored and authentication attempts will fail
     */
    public void setPassword(int uid, String password, long expiresAt) throws SQLException, BusyException {
        setPassword(uid, password, false, expiresAt);
    }

    /**
     * Sets the password for the given user
     *
     * @param requireNew If true, and the UID is already in the database, this operation will throw a SQLException constraint violation
     * @param expiresAt  The time after which this password will be ignored and authentication attempts will fail
     */
    public void setPassword(int uid, String password, boolean requireNew, long expiresAt) throws SQLException, BusyException {
        if (!initialized) throw new IllegalArgumentException("Not initialized");

        final long now = System.currentTimeMillis();
        connectionManager.executeTransaction(connection -> {
            try {
                connectionManager.execute(connection, "INSERT INTO " + table + " (created,uid) VALUES (?,?)", now, uid);
            } catch (SQLException e) {
                if (requireNew || !ConnectionManager.isConstraintViolation(e)) throw e;
            }
            try {
                connectionManager.execute(connection, "UPDATE " + table + " SET hash=?,last_changed=?,expires=? WHERE uid=?", HashPBKDF2.hash(password, type.iterationCount), now, expiresAt, uid);
            } catch (NoSuchAlgorithmException e) {
                // unexpected, verified at creation.
                throw new RuntimeException(e);
            }
        }, 10000L);
    }

    public boolean testPassword(int uid, String password) throws SQLException, BusyException {
        if (!initialized) throw new IllegalArgumentException("Not initialized");

        byte[] hash = connectionManager.query("SELECT hash FROM " + table + " WHERE uid=? AND (expires IS NULL OR expires<=0 OR expires>?)", rs -> {
            if (!rs.next()) return null;
            return connectionManager.getBlob(rs, 1);
        }, 10000L, uid, System.currentTimeMillis());
        if (hash == null || hash.length == 0) return false;

        try {
            return HashPBKDF2.test(hash, password, type.iterationCount);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean contains(int uid) throws SQLException, BusyException {
        byte[] hash = connectionManager.query("SELECT hash FROM " + table + " WHERE uid=?", rs -> {
            if (!rs.next()) return null;
            return connectionManager.getBlob(rs, 1);
        }, 10000L, uid);

        return hash != null && hash.length >= 32;
    }

    public Set<Integer> listUsers() throws SQLException, BusyException {
        return connectionManager.query("SELECT uid FROM " + table, rs -> {
            Set<Integer> out = new HashSet<>();
            while (rs.next()) out.add(rs.getInt(1));
            return out;
        }, 3000);
    }

    public long getLastChanged(int uid) throws SQLException, BusyException {
        return connectionManager.query("SELECT last_changed FROM " + table + " WHERE uid=?", rs -> {
            if (!rs.next()) return -1L;
            return rs.getLong(1);
        }, 10000L, uid);
    }

    public String getTableName() {
        return table;
    }

    public boolean remove(int uid) throws SQLException, BusyException {
        return connectionManager.executeReturnRows("DELETE FROM " + table + " WHERE uid=?", 10000L, uid) > 0;
    }
}
