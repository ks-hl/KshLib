package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ConnectionConsumer;
import dev.kshl.kshlib.function.ConnectionFunction;
import dev.kshl.kshlib.misc.MapCache;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public abstract class SQLSet<T> {
    private final ConnectionManager connectionManager;
    private final String table;
    private final String type;
    private final MapCache<T, Boolean> cache;

    private SQLSet(ConnectionManager connectionManager, String table, String type, boolean cache) {
        this.connectionManager = connectionManager;
        this.table = ConnectionManager.validateTableName(table);
        this.type = type;
        this.cache = cache ? new MapCache<>(1, TimeUnit.HOURS) : null;
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
        return connectionManager.apply(connection -> contains(connection, value)).readOnly().executeQuery(3000L);
    }

    public boolean contains(Connection connection, T value) throws SQLException {
        var cached = getCached(value);
        if (cached != null) return cached;
        boolean contains = connectionManager.query(connection, "SELECT * FROM " + table + " WHERE value=?", ResultSet::next, value);
        cache(value, contains);
        return contains;
    }

    public boolean add(T value) throws SQLException, BusyException {
        return connectionManager.execute((ConnectionFunction<Boolean>) connection -> add(connection, value), 3000L);
    }

    public boolean add(Connection connection, T value) throws SQLException {
        if (getCached(value) == Boolean.TRUE) return false;
        boolean added = connectionManager.executeReturnRows(connection, connectionManager.getInsertOrIgnore() + " INTO " + table + " (value) VALUES (?)", value) > 0;
        cache(value, true);
        return added;
    }

    public boolean remove(T value) throws SQLException, BusyException {
        return connectionManager.execute((ConnectionFunction<Boolean>) connection -> remove(connection, value), 3000L);
    }

    public boolean remove(Connection connection, T value) throws SQLException {
        if (getCached(value) == Boolean.FALSE) return false;
        boolean change = connectionManager.executeReturnRows(connection, "DELETE FROM " + table + " WHERE value=?", value) > 0;
        cache(value, false);
        return change;
    }

    public void addAll(Collection<T> values) throws SQLException, BusyException {
        connectionManager.execute((ConnectionConsumer) connection -> addAll(connection, values), 3000L);
    }

    public void removeAll(Collection<T> values) throws SQLException, BusyException {
        connectionManager.execute((ConnectionConsumer) connection -> removeAll(connection, values), 3000L);
    }

    public void addAll(Connection connection, Collection<T> values) throws SQLException {
        connectionManager.executeBatch(connection, "INSERT OR IGNORE INTO " + table + " (value) VALUES (?)", values, List::of);
    }

    public void removeAll(Connection connection, Collection<T> values) throws SQLException {
        connectionManager.executeBatch(connection, "DELETE FROM " + table + " WHERE value=?", values, List::of);
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

    private static class ID<U, T extends SQLIDManager<U>> {
        private final T sqlidManager;
        private final SQLSet.Int set;

        ID(ConnectionManager connectionManager, T sqlidManager, String table, boolean cache) {
            this.sqlidManager = sqlidManager;
            this.set = new Int(connectionManager, table, cache);
        }

        public void init(Connection connection) throws SQLException {
            set.init(connection);
        }

        public boolean contains(U value) throws SQLException, BusyException {
            try {
                return set.contains(sqlidManager.getIDOpt(value, false).orElseThrow());
            } catch (NoSuchElementException e) {
                return false;
            }
        }

        public boolean add(U value) throws SQLException, BusyException {
            return set.add(sqlidManager.getIDOrInsert(value));
        }

        public boolean remove(U value) throws SQLException, BusyException {
            return set.remove(sqlidManager.getIDOrInsert(value));
        }
    }

    public static class IDText extends ID<String, SQLIDManager.Str> {
        public IDText(ConnectionManager connectionManager, SQLIDManager.Str sqlidManager, String table, boolean cache) {
            super(connectionManager, sqlidManager, table, cache);
        }
    }

    public static class IDUUID extends ID<java.util.UUID, SQLIDManager.UUIDText> {
        public IDUUID(ConnectionManager connectionManager, SQLIDManager.UUIDText sqlidManager, String table, boolean cache) {
            super(connectionManager, sqlidManager, table, cache);
        }
    }
}
