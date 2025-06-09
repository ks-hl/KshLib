package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.MapCache;
import dev.kshl.kshlib.misc.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public abstract class SettingManager<T> {
    final ConnectionManager sql;
    private final String table;
    final MapCache<Pair<Integer, Integer>, T> cache = new MapCache<>(3600000L, 3600000L, true);
    private final T def;
    private final String sqlType;
    private final ResultSetFunction<T> retrievalFunction;
    private final boolean multiple;
    private boolean initDone;

    SettingManager(ConnectionManager sql, String table, T def, String sqlType, ResultSetFunction<T> retrievalFunction, boolean multiple) {
        this.def = def;
        this.sqlType = sqlType;
        this.retrievalFunction = retrievalFunction;
        this.multiple = multiple;
        if (!table.matches("[\\w_]+")) throw new IllegalArgumentException("Invalid table name " + table);
        this.sql = sql;
        this.table = table;
    }

    public void init(Connection connection) throws SQLException {
        if (initDone) throw new IllegalStateException("Initialization is already complete.");


        String statement = "CREATE TABLE IF NOT EXISTS " + table + " (uid INTEGER, setting INTEGER, value " + sqlType + ", UNIQUE(uid,setting))";
        sql.execute(connection, statement);

        initDone = true;
    }

    private void validateSettingID(int setting) {
        validateSettingID(setting, multiple);
    }

    private static void validateSettingID(int setting, boolean multiple) {
        if (setting != 0 && !multiple) {
            throw new IllegalArgumentException("SettingID must be 0 for non-multiple SettingManager");
        } else if (setting <= 0 && multiple) {
            throw new IllegalArgumentException("SettingID must be >0 for multiple SettingManager");
        }
    }

    public void set(int uid, T value) throws SQLException, BusyException, IllegalArgumentException {
        set(uid, 0, value);
    }

    public void set(int uid, int setting, T value) throws SQLException, BusyException, IllegalArgumentException {
        if (!initDone) throw new IllegalStateException("Initialization is not complete.");
        validateSettingID(setting);
        validate(value);

        if (Objects.equals(value, def)) {
            sql.execute("DELETE FROM " + table + " WHERE uid=? AND setting=?", 3000L, uid, setting);
        } else {
            sql.executeTransaction(connection -> {
                int updated = sql.executeReturnRows("UPDATE " + table + " SET value=? WHERE uid=? AND setting=?", 3000L, value, uid, setting);
                if (updated > 0) return;

                sql.execute("INSERT INTO " + table + " (uid,setting,value) VALUES (?,?,?)", 3000L, uid, setting, value);
            }, 3000);
        }
        cache.put(new Pair<>(uid, setting), value);
    }

    public void validate(T value) throws IllegalArgumentException {
    }

    public T get(int uid) throws SQLException, BusyException {
        return get(uid, 0);
    }

    public T get(int uid, int setting) throws SQLException, BusyException {
        if (!initDone) throw new IllegalStateException("Initialization is not complete.");
        validateSettingID(setting);

        Pair<Integer, Integer> key = new Pair<>(uid, setting);
        T val = cache.get(key);
        if (val != null) return val;

        val = sql.query("SELECT value FROM " + table + " WHERE uid=? AND setting=?", rs -> {
            if (!rs.next()) return def;
            return retrievalFunction.apply(rs);
        }, 3000L, uid, setting);
        cache.put(key, val);
        return val;
    }

    public void clearCache() {
        cache.clear();
    }

    public String getTableName() {
        return table;
    }

    public T getDefault() {
        return def;
    }

    public abstract void setFromString(int uid, int setting, String value) throws SQLException, BusyException, IllegalArgumentException;

    public abstract void setFromObject(int uid, int setting, Object value) throws SQLException, BusyException, ClassCastException, ArgumentValidationException;

    abstract Boolean toBoolean(T value) throws ClassCastException;

    abstract Integer toInteger(T value) throws ClassCastException;

    public Boolean getBoolean(int uid) throws SQLException, BusyException, ClassCastException {
        return getBoolean(uid, 0);
    }

    public Boolean getBoolean(int uid, int setting) throws SQLException, BusyException, ClassCastException {
        T val = get(uid, setting);
        if (val == null) return null;
        return toBoolean(val);
    }

    public Integer getInteger(int uid) throws SQLException, BusyException, ClassCastException {
        return getInteger(uid, 0);
    }

    public Integer getInteger(int uid, int setting) throws SQLException, BusyException, ClassCastException {
        T val = get(uid, setting);
        if (val == null) return null;
        return toInteger(val);
    }

    public String getString(int uid) throws SQLException, BusyException {
        return getString(uid, 0);
    }

    public String getString(int uid, int setting) throws SQLException, BusyException {
        return String.valueOf(get(uid, setting));
    }

    public boolean isMultiple() {
        return multiple;
    }

    public static class Bool extends SettingManager<Boolean> {
        public Bool(ConnectionManager sql, String table, boolean multiple, Boolean def) {
            super(sql, table, Objects.requireNonNull(def, "def must be true/false"), "BOOLEAN", rs -> rs.getBoolean(1), multiple);
        }

        @Override
        public void setFromString(int uid, int setting, String value) throws SQLException, BusyException, IllegalArgumentException {
            set(uid, setting, Boolean.parseBoolean(value));
        }

        @Override
        public void setFromObject(int uid, int setting, Object value) throws SQLException, BusyException, ClassCastException, ArgumentValidationException {
            if (value instanceof Boolean bool) set(uid, setting, bool);
            else if (value instanceof String string) setFromString(uid, setting, string);
            else {
                throw new ClassCastException("Value is wrong type. " + value + (value == null ? "" : (", " + value.getClass().getName())));
            }
        }

        public boolean toggle(int uid) throws SQLException, BusyException {
            return toggle(uid, 0);
        }

        public boolean toggle(int uid, int setting) throws SQLException, BusyException {
            // TODO do this in 1 statement possible?
            return sql.executeTransaction(connection -> {
                boolean state = !get(uid, setting);
                set(uid, setting, state);
                return state;
            }, 3000L);
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
        public Text(ConnectionManager sql, String table, boolean multiple, String def) {
            super(sql, table, "TEXT", def, rs -> rs.getString(1), multiple);
        }

        @Override
        public void setFromString(int uid, int setting, String value) throws SQLException, BusyException, IllegalArgumentException {
            set(uid, setting, value);
        }

        @Override
        public void setFromObject(int uid, int setting, Object value) throws SQLException, BusyException, ClassCastException, ArgumentValidationException {
            if (value instanceof String string) set(uid, setting, string);
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
        public Int(ConnectionManager sql, String table, boolean multiple, Integer def) {
            super(sql, table, def, "INT", rs -> rs.getInt(1), multiple);
        }

        @Override
        public void setFromString(int uid, int setting, String value) throws SQLException, BusyException, ArgumentValidationException {
            set(uid, setting, Integer.parseInt(value));
        }

        @Override
        public void setFromObject(int uid, int setting, Object value) throws SQLException, BusyException, ClassCastException, ArgumentValidationException {
            if (value instanceof Integer i) set(uid, setting, i);
            else if (value instanceof String string) setFromString(uid, setting, string);
            else {
                throw new ClassCastException("Value is wrong type. " + value + (value == null ? "" : (", " + value.getClass().getName())));
            }
        }

        public void inc(int uid) throws SQLException, BusyException {
            inc(uid, 0);
        }

        public void inc(int uid, int setting) throws SQLException, BusyException {
            add(uid, setting, 1);
        }

        public void dec(int uid) throws SQLException, BusyException {
            dec(uid, 0);
        }

        public void dec(int uid, int setting) throws SQLException, BusyException {
            add(uid, setting, -1);
        }

        public void add(int uid, int amount) throws SQLException, BusyException {
            add(uid, 0, amount);
        }

        public void add(int uid, int setting, int amount) throws SQLException, BusyException {
            validateSettingID(setting, Int.this.isMultiple());
            sql.executeTransaction(connection -> {
                try {
                    sql.execute(connection, "INSERT INTO " + getTableName() + " (uid,setting,value) VALUES (?,?,?)", uid, setting, amount);
                } catch (SQLException e) {
                    if (!ConnectionManager.isConstraintViolation(e)) throw e;
                    sql.execute(connection, "UPDATE " + getTableName() + " SET value=value+? WHERE uid=? AND setting=?", amount, uid, setting);
                }
                cache.remove(new Pair<>(uid, setting));
            }, 3000L);
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
