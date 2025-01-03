package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.MapCache;
import dev.kshl.kshlib.misc.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class SQLUsernameManager implements ISQLManager {

    private final ConnectionManager sql;
    private final String table;
    private final MapCache<Integer, String> cacheUIDToUsername = new MapCache<>(3600000L, 3600000L, true);
    private final MapCache<String, Integer> cacheUsernameToUID = new MapCache<>(3600000L, 3600000L, true);

    public SQLUsernameManager(ConnectionManager sql, String table) {
        validateTableName(table);

        this.sql = sql;
        this.table = table;
    }

    public void init(Connection connection) throws SQLException {
        String stmt = "CREATE TABLE IF NOT EXISTS " + table + " (time BIGINT, uid INTEGER, username ";

        if (sql.isMySQL()) {
            stmt += "VARCHAR(255), UNIQUE KEY unique_uid_username (uid, username)";
        } else {
            stmt += "TEXT, UNIQUE(uid, username)";
        }

        stmt += ")";

        sql.execute(connection, stmt);

        sql.execute(connection, "CREATE INDEX IF NOT EXISTS idx_time ON " + table + " (time)");
    }

    public void updateUsername(int uid, String username) throws SQLException, BusyException {
        Objects.requireNonNull(username, "Username must not be null");
        if (uid <= 0) throw new IllegalArgumentException("UID must be > 0");

        try {
            sql.execute("INSERT INTO " + table + " (time,uid,username) VALUES (?,?,?)", 3000L, System.currentTimeMillis(), uid, username);
            cacheUIDToUsername.remove(uid);
            cacheUsernameToUID.remove(username.toLowerCase());
            cache(uid, username);
        } catch (SQLException e) {
            if (ConnectionManager.isConstraintViolation(e)) return;
            throw e;
        }
    }

    private void cache(Integer uid, String username) {
        if (uid != null) {
            cacheUIDToUsername.put(uid, username);
        }
        if (username != null) {
            cacheUsernameToUID.put(username.toLowerCase(), uid);
        }
    }

    public Optional<String> getUsername(int uid) throws SQLException, BusyException {
        if (uid <= 0) return Optional.empty();

        String cached = cacheUIDToUsername.get(uid);
        if (cached != null) return Optional.of(cached);

        Optional<String> username = sql.query("SELECT username FROM " + table + " WHERE uid=? ORDER BY time DESC LIMIT 1", rs -> {
            if (!rs.next()) return Optional.empty();
            return Optional.ofNullable(rs.getString(1));
        }, 3000L, uid);

        cache(uid, username.orElse(null));
        return username;
    }

    public Optional<Integer> getUID(String username) throws SQLException, BusyException {
        if (username == null) return Optional.empty();

        Integer cached = cacheUsernameToUID.get(username.toLowerCase());
        if (cached != null) return Optional.of(cached);

        Optional<Pair<Integer, String>> uidName = sql.query("SELECT uid,username FROM " + table + " WHERE lower(username)=? ORDER BY time DESC LIMIT 1", rs -> {
            if (!rs.next()) return Optional.empty();
            return Optional.of(new Pair<>(rs.getInt(1), rs.getString(2)));
        }, 3000L, username.toLowerCase());

        cache(uidName.map(Pair::getKey).orElse(null), uidName.map(Pair::getValue).orElse(username));

        return uidName.map(Pair::getKey);
    }
}
