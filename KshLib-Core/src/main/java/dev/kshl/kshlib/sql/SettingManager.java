package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.MapCache;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public abstract class SettingManager<T> {
    private final ConnectionManager sql;
    private final String table;
    private final MapCache<Integer, T> cache = new MapCache<>(3600000L, 3600000L, true);
    private final T def;
    private final String sqlType;
    private final ResultSetFunction<T> retrievalFunction;
    private boolean initDone;

    private SettingManager(ConnectionManager sql, String table, T def, String sqlType, ResultSetFunction<T> retrievalFunction) {
        this.def = def;
        this.sqlType = sqlType;
        this.retrievalFunction = retrievalFunction;
        if (!table.matches("[\\w_]+")) throw new IllegalArgumentException("Invalid table name " + table);
        this.sql = sql;
        this.table = table;
    }

    public void init(Connection connection) throws SQLException {
        if (initDone) throw new IllegalStateException("Initialization is already complete.");


        String statement = "CREATE TABLE IF NOT EXISTS " + table + " (uid INTEGER PRIMARY KEY, value " + sqlType + ")";
        sql.execute(connection, statement);

        initDone = true;
    }


    public void set(int uid, T value) throws SQLException, BusyException, IllegalArgumentException {
        if (!initDone) throw new IllegalStateException("Initialization is not complete.");
        validate(value);

        if (Objects.equals(value, def)) {
            sql.execute("DELETE FROM " + table + " WHERE uid=?", 3000L, uid);
        } else {
            sql.executeTransaction(connection -> {
                int updated = sql.executeReturnRows("UPDATE " + table + " SET value=? WHERE uid=?", 3000L, value, uid);
                if (updated > 0) return;

                sql.execute("INSERT INTO " + table + " (uid,value) VALUES (?,?)", 3000L, uid, value);
            }, 3000);
        }
        cache.put(uid, value);
    }

    public void validate(T value) throws IllegalArgumentException {
    }

    public T get(int uid) throws SQLException, BusyException {
        if (!initDone) throw new IllegalStateException("Initialization is not complete.");

        T val = cache.get(uid);
        if (val != null) return val;

        val = sql.query("SELECT value FROM " + table + " WHERE uid=?", rs -> {
            if (!rs.next()) return def;
            return retrievalFunction.apply(rs);
        }, 3000L, uid);
        cache.put(uid, val);
        return val;
    }

    public void clearCache() {
        cache.clear();
    }

    public String getTableName() {
        return table;
    }

    public abstract void setFromString(int uid, String value) throws SQLException, BusyException, IllegalArgumentException;

    public abstract void setFromObject(int uid, Object value) throws SQLException, BusyException, ClassCastException, ArgumentValidationException;

    abstract Boolean toBoolean(T value) throws ClassCastException;

    abstract Integer toInteger(T value) throws ClassCastException;

    public Boolean getBoolean(int uid) throws SQLException, BusyException, ClassCastException {
        T val = get(uid);
        if (val == null) return null;
        return toBoolean(val);
    }

    public Integer getInteger(int uid) throws SQLException, BusyException, ClassCastException {
        T val = get(uid);
        if (val == null) return null;
        return toInteger(val);
    }

    public String getString(int uid) throws SQLException, BusyException {
        return String.valueOf(get(uid));
    }

    public static class Bool extends SettingManager<Boolean> {
        public Bool(ConnectionManager sql, String table, Boolean def) {
            super(sql, table, def, "BOOLEAN", rs -> rs.getBoolean(1));
        }

        @Override
        public void setFromString(int uid, String value) throws SQLException, BusyException, IllegalArgumentException {
            set(uid, Boolean.parseBoolean(value));
        }

        @Override
        public void setFromObject(int uid, Object value) throws SQLException, BusyException, ClassCastException, ArgumentValidationException {
            if (value instanceof Boolean bool) set(uid, bool);
            else if (value instanceof String string) setFromString(uid, string);
            else {
                throw new ClassCastException("Value is wrong type. " + value + (value == null ? "" : (", " + value.getClass().getName())));
            }
        }

        @Override
        Boolean toBoolean(Boolean value) throws ClassCastException {
            return value;
        }

        @Override
        Integer toInteger(Boolean value) throws ClassCastException {
            throw new ClassCastException("Cannot cast Boolean to Integer");
        }
    }

    public static class Text extends SettingManager<String> {
        public Text(ConnectionManager sql, String table, String def) {
            super(sql, table, "TEXT", def, rs -> rs.getString(1));
        }

        @Override
        public void setFromString(int uid, String value) throws SQLException, BusyException, IllegalArgumentException {
            set(uid, value);
        }

        @Override
        public void setFromObject(int uid, Object value) throws SQLException, BusyException, ClassCastException, ArgumentValidationException {
            if (value instanceof String string) set(uid, string);
            else {
                throw new ClassCastException("Value is wrong type. " + value + (value == null ? "" : (", " + value.getClass().getName())));
            }
        }

        @Override
        Boolean toBoolean(String value) throws ClassCastException {
            try {
                return Boolean.parseBoolean(value);
            } catch (IllegalArgumentException e) {
                throw new ClassCastException(e.getMessage());
            }
        }

        @Override
        Integer toInteger(String value) throws ClassCastException {
            try {
                return Integer.parseInt(value);
            } catch (IllegalArgumentException e) {
                throw new ClassCastException(e.getMessage());
            }
        }
    }

    public static class Int extends SettingManager<Integer> {
        public Int(ConnectionManager sql, String table, Integer def) {
            super(sql, table, def, "INT", rs -> rs.getInt(1));
        }

        @Override
        public void setFromString(int uid, String value) throws SQLException, BusyException, ArgumentValidationException {
            set(uid, Integer.parseInt(value));
        }

        @Override
        public void setFromObject(int uid, Object value) throws SQLException, BusyException, ClassCastException, ArgumentValidationException {
            if (value instanceof Integer i) set(uid, i);
            else if (value instanceof String string) setFromString(uid, string);
            else {
                throw new ClassCastException("Value is wrong type. " + value + (value == null ? "" : (", " + value.getClass().getName())));
            }
        }

        @Override
        Boolean toBoolean(Integer value) throws ClassCastException {
            throw new ClassCastException("Cannot cast Integer to Boolean");
        }

        @Override
        Integer toInteger(Integer value) throws ClassCastException {
            return value;
        }
    }

    public static class ArgumentValidationException extends IllegalArgumentException {
        public ArgumentValidationException(String msg) {
            super(msg);
        }
    }
}
