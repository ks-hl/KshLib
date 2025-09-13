package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.MapCache;
import javax.annotation.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

public abstract class SQLSet<T> {
    private final ConnectionManager connectionManager;
    private final String table;
    private final String type;
    private final MapCache<T, Boolean> cache;

    private SQLSet(ConnectionManager connectionManager, String table, String type, boolean cache) {
        this.connectionManager = connectionManager;
        this.table = ConnectionManager.validateTableName(table);
        this.type = type;
        this.cache = cache ? new MapCache<>(3600000L, 3600000L, true) : null;
    }

    public void init(Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + table + " (value " + type + " PRIMARY KEY)")) {
            preparedStatement.execute();
        }
    }

    @Nullable
    private Boolean getCached(T value) {
        if (cache == null) return null;
        return cache.get(value);
    }

    public boolean contains(T value) throws SQLException, BusyException {
        var cached = getCached(value);
        if (cached != null) return cached;
        boolean contains = connectionManager.query("SELECT * FROM " + table + " WHERE value=?", ResultSet::next, 10000L, value);
        cache(value, contains);
        return contains;
    }

    public boolean add(T value) throws SQLException, BusyException {
        var cached = getCached(value);
        if (cached != null && cached) return false;
        try {
            connectionManager.execute("INSERT INTO " + table + " (value) VALUES (?)", 10000L, value);
            cache(value, true);
            return true;
        } catch (SQLException e) {
            if (!ConnectionManager.isConstraintViolation(e)) throw e;
            return false;
        }
    }

    public boolean remove(T value) throws SQLException, BusyException {
        var cached = getCached(value);
        if (cached != null && !cached) return false;
        boolean change = connectionManager.executeReturnRows("DELETE FROM " + table + " WHERE value=?", 10000L, value) > 0;
        cache(value, false);
        return change;
    }

    private void cache(T value, boolean state) {
        if (cache == null) return;
        cache.put(value, state);
    }

    public String getTableName() {
        return table;
    }

    public static class Int extends SQLSet<Integer> {
        public Int(ConnectionManager connectionManager, String table, boolean cache) {
            super(connectionManager, table, "INT", cache);
        }
    }

    public static class BigInt extends SQLSet<Long> {
        public BigInt(ConnectionManager connectionManager, String table, boolean cache) {
            super(connectionManager, table, "BIGINT", cache);
        }
    }

    public static class ID {
        private final SQLIDManager.Str sqlidManager;
        private final SQLSet.Int set;

        public ID(ConnectionManager connectionManager, SQLIDManager.Str sqlidManager, String table, boolean cache) {
            this.sqlidManager = sqlidManager;
            this.set = new Int(connectionManager, table, cache);
        }

        public void init(Connection connection) throws SQLException {
            set.init(connection);
        }

        public boolean contains(String value) throws SQLException, BusyException {
            try {
                return set.contains(sqlidManager.getIDOpt(value, false).orElseThrow());
            } catch (NoSuchElementException e) {
                return false;
            }
        }

        public boolean add(String value) throws SQLException, BusyException {
            return set.add(sqlidManager.getIDOrInsert(value));
        }

        public boolean remove(String value) throws SQLException, BusyException {
            return set.remove(sqlidManager.getIDOrInsert(value));
        }
    }
}
