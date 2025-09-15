package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.concurrent.ConcurrentReference;
import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ConnectionFunction;
import dev.kshl.kshlib.misc.BiDiMapCache;
import dev.kshl.kshlib.misc.UUIDHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class SQLIDManager<V> {
    private final ConnectionManager sql;
    private final String table;
    private final ConcurrentReference<BiDiMapCache<Integer, V>> cache = new ConcurrentReference<>(new BiDiMapCache<>(300000L, 300000L, true));
    private final String datatype;
    private boolean initDone;

    public SQLIDManager(ConnectionManager sql, String table, String datatype) {
        this.datatype = datatype;
        if (!table.matches("[\\w_]+")) throw new IllegalArgumentException("Invalid table name " + table);
        this.sql = sql;
        this.table = ConnectionManager.validateTableName(table);
    }

    public void init(Connection connection) throws SQLException {
        if (initDone) throw new IllegalStateException("Initialization is already complete.");

        boolean migrationRequired = sql.tableExists(connection, table) && !sql.uniqueConstraintExists(connection, table, "id");

        if (migrationRequired) {
            System.out.println("Migrating ID table `" + table + "` to add unique constraint");
            sql.execute(connection, "DROP TABLE IF EXISTS " + table + "_temp");
            sql.execute(connection, "ALTER TABLE " + table + " RENAME TO " + table + "_temp");
        }

        String metaData = getTableMetaDataColumns().trim();
        if (!metaData.isBlank() && !metaData.startsWith(",")) metaData = "," + metaData;
        sql.execute(connection, String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    value %s UNIQUE,
                    id INTEGER PRIMARY KEY %s UNIQUE
                    %s
                )""", table, datatype, sql.autoincrement(), metaData));

        if (migrationRequired) {
            sql.query(connection, "SELECT id,value FROM " + table + "_temp ORDER BY id ASC", rs -> {
                while (rs.next()) {
                    V value = getValue(rs, 2);
                    int id = rs.getInt(1);
                    try {
                        sql.execute(connection, "INSERT INTO " + table + " (id,value) VALUES (?,?)", id, toDatabaseObject(value));
                    } catch (SQLException e) {
                        if (ConnectionManager.isConstraintViolation(e)) {
                            System.err.println("Found duplicate ID `" + id + "` for value `" + value + "` in table " + table);
                        }
                    }
                }
            });
        }
        sql.execute(connection, "DROP TABLE IF EXISTS " + table + "_temp");

        initDone = true;
    }

    protected String getTableMetaDataColumns() {
        return "";
    }


    public int getIDOrInsert(@Nonnull V value) throws SQLException, BusyException {
        return sql.execute((ConnectionFunction<Integer>) connection -> getIDOrInsert(connection, value), 3000L);
    }

    public int getIDOrInsert(Connection connection, @Nonnull V value) throws SQLException {
        Objects.requireNonNull(value, "value must be nonnull");
        if (isInvalid(value)) throw new IllegalArgumentException("Invalid value: " + value);

        return getIDOpt(connection, value, true, false).orElseThrow();
    }

    public Optional<Integer> getIDOpt(V value, boolean insert) throws SQLException, BusyException {
        return getIDOpt(value, insert, false);
    }

    private Optional<Integer> getIDOpt(V value, boolean insert, boolean requireNew) throws SQLException, BusyException {
        return sql.execute((ConnectionFunction<Optional<Integer>>) connection -> getIDOpt(connection, value, insert, requireNew), 10000L);
    }

    private Optional<Integer> getIDOpt(Connection connection, V value, boolean insert, boolean requireNew) throws SQLException {
        if (!initDone) throw new IllegalStateException("Initialization is not complete.");
        if (value == null || isInvalid(value)) return Optional.empty();

        if (!requireNew) {
            Integer cachedValue = cache.function(cache -> {
                if (cache.containsValue(value)) {
                    return cache.getKey(value);
                }
                return null;
            });
            if (cachedValue != null) return Optional.of(cachedValue);
        }

        for (int i = 0; i < 30; i++) {
            try {
                return sql.executeTransaction(connection, () -> {
                    try {
                        if (insert) {
                            sql.execute(connection, "INSERT INTO " + table + " (value) VALUES (?)", toDatabaseObject(value));
                        }
                    } catch (SQLException e) {
                        if (requireNew || !ConnectionManager.isConstraintViolation(e)) throw e;
                    }
                    return sql.query(connection, "SELECT id FROM " + table + " WHERE value=?", rs -> {
                        if (!rs.next()) return Optional.empty();
                        int result = rs.getInt(1);
                        cache(result, value);
                        return Optional.of(result);
                    }, toDatabaseObject(value));
                });
            } catch (SQLException e) {
                int code = e.getErrorCode();
                String state = e.getSQLState();
                // Handle MySQL deadlock and lock wait timeout
                if (code == 1213 || code == 1205) continue;
                // Handle SQLite busy/locked
                if ("40001".equals(state) || "HY000".equals(state)) continue;
                throw e;
            }
        }
        throw new SQLException("Unable to get UID due to repeated deadlocks or timeouts");
    }

    public int getIDRequireNew(V value) throws SQLException, BusyException {
        return getIDOpt(value, true, true).orElseThrow();
    }

    /**
     * @deprecated Use {@link #getIDOpt(Object, boolean, boolean)}
     */
    @Deprecated
    public int getIDOrThrow(V value, boolean insert) throws SQLException, BusyException {
        return getIDOpt(value, insert).orElseThrow(() -> new IllegalArgumentException("ID for '" + value + "' not found."));
    }

    public V getNewUniqueID(Supplier<V> idCreator) throws SQLException, BusyException {
        for (int i = 0; i < 10000; i++) {
            V id = idCreator.get();
            try {
                getIDRequireNew(id);
            } catch (SQLException e) {
                if (ConnectionManager.isConstraintViolation(e)) continue;
                throw e;
            }
            return id;
        }
        throw new IllegalStateException("Failed to generate a new ID after 10000 iterations");
    }

    /**
     * Forces the provided uid/value pair into the database. This should really only be used for migrations or testing. Otherwise use {@link #getIDOpt(Object, boolean)}
     */
    public void put(int id, V value) throws SQLException, BusyException {
        sql.execute("INSERT INTO " + table + " (id,value) VALUES (?,?)", 3000L, id, toDatabaseObject(value));
    }

    @Nullable
    @Deprecated
    public V getValue(int id) throws SQLException, BusyException {
        return getValueOpt(id).orElse(null);
    }

    public Optional<V> getValueOpt(int id) throws SQLException, BusyException {
        if (!initDone) throw new IllegalStateException("Initialization is not complete.");
        if (id <= 0) return Optional.empty();
        V cachedValue = cache.function(cache -> {
            if (cache.containsKey(id)) return cache.get(id);
            return null;
        });
        if (cachedValue != null) return Optional.of(cachedValue);
        return sql.query("SELECT value FROM " + table + " WHERE id=?", rs -> {
            if (!rs.next()) return Optional.empty();

            V value = getValue(rs, 1);
            cache(id, value);
            return Optional.ofNullable(value);
        }, 30000L, id);
    }

    protected void cache(int id, V value) throws BusyException {
        if (id <= 0) return;
        cache.consume(cache -> cache.put(id, value), 10000L);
    }

    @Nonnull
    @Deprecated
    public V getValueOrThrow(int id) throws SQLException, BusyException {
        return getValueOpt(id).orElseThrow(() -> new IllegalArgumentException("Value not found for ID " + id));
    }

    public boolean remove(int id) throws SQLException, BusyException {
        return sql.executeReturnRows("DELETE FROM " + table + " WHERE id=?", 10000L, id) > 0;
    }

    public String getTableName() {
        return table;
    }

    public void clearCache() {
        cache.consume(BiDiMapCache::clear);
    }

    protected abstract V getValue(ResultSet rs, int index) throws SQLException;

    protected abstract Object toDatabaseObject(V value);

    protected abstract boolean isInvalid(V v);

    public static class Str extends SQLIDManager<String> {

        public Str(ConnectionManager sql, String table) {
            this(sql, table, 255);
        }

        public Str(ConnectionManager sql, String table, int varcharLength) {
            super(sql, table, "VARCHAR(" + varcharLength + ")");
        }

        @Override
        protected String getValue(ResultSet rs, int i) throws SQLException {
            return rs.getString(i);
        }

        @Override
        protected Object toDatabaseObject(String value) {
            return value;
        }

        @Override
        protected boolean isInvalid(String string) {
            return string == null || string.isEmpty() || string.equalsIgnoreCase("#null");
        }
    }

    public static class L extends SQLIDManager<Long> {

        public L(ConnectionManager sql, String table) {
            super(sql, table, "BIGINT");
        }

        @Override
        protected Long getValue(ResultSet rs, int i) throws SQLException {
            return rs.getLong(i);
        }

        @Override
        protected boolean isInvalid(Long l) {
            return l == null;
        }

        @Override
        protected Object toDatabaseObject(Long value) {
            return value;
        }
    }

    public static class UUIDText extends SQLIDManager<java.util.UUID> {
        public UUIDText(ConnectionManager sql, String table) {
            super(sql, table, "TEXT");
        }

        @Override
        protected UUID getValue(ResultSet rs, int i) throws SQLException {
            try {
                return UUIDHelper.fromString(rs.getString(i));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        @Override
        protected boolean isInvalid(UUID uuid) {
            return uuid == null;
        }

        @Override
        protected Object toDatabaseObject(UUID value) {
            return Objects.requireNonNull(value).toString();
        }
    }
}
