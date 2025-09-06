package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.MapCache;
import dev.kshl.kshlib.misc.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class SQLUsernameManager implements ISQLManager {

    private final ConnectionManager sql;
    private final String table;
    private final MapCache<Integer, String> cacheUIDToUsername = new MapCache<>(3600000L, 30000L, true);
    private final MapCache<String, Integer> cacheUsernameToUID = new MapCache<>(3600000L, 30000L, true);

    public SQLUsernameManager(ConnectionManager sql, String table) {
        validateTableName(table);

        this.sql = sql;
        this.table = table;
    }

    public void init(Connection connection) throws SQLException {
        sql.executeTransaction(connection, () -> {
            boolean needsMigration = sql.uniqueConstraintExists(connection, table, "uid", "username");
            if (needsMigration) {
                sql.execute(connection, "DROP TABLE IF EXISTS " + table + "_temp");
                sql.execute(connection, "ALTER TABLE " + table + " RENAME TO " + table + "_temp");
            }

            sql.execute(connection, String.format("""
                    CREATE TABLE IF NOT EXISTS %s (
                        time BIGINT,
                        uid INTEGER,
                        username VARCHAR(255)%s
                    )""", table, sql.isMySQL() ? " COLLATE utf8mb4_general_ci" : "")); // utf8mb4_general_ci gives case-insensitive indexing to MySQL

            if (needsMigration) {
                sql.execute(connection, "INSERT INTO " + table + " SELECT * FROM " + table + "_temp");
                sql.execute(connection, "DROP TABLE IF EXISTS " + table + "_temp");
            }

            if (sql.indexExists(connection, table, "idx_time")) {
                sql.execute(connection, "DROP INDEX IF EXISTS idx_time");
            }
            sql.execute(connection, String.format("CREATE INDEX IF NOT EXISTS idx_%s_time ON %s (time)", table, table));
            sql.execute(connection, String.format("CREATE INDEX IF NOT EXISTS idx_%s_uid_time ON %s (uid, time DESC)", table, table));
            sql.execute(connection, String.format("CREATE INDEX IF NOT EXISTS idx_%s_username_time ON %s (username, time DESC)", table, table));

            if (sql.isMySQL()) {
                sql.execute(connection, String.format("CREATE INDEX IF NOT EXISTS idx_%s_username_time ON %s (username, time DESC)", table, table));
            } else {
                sql.execute(connection, String.format("CREATE INDEX IF NOT EXISTS idx_%s_username_time ON %s (username COLLATE NOCASE, time DESC)", table, table)); // Case-insensitive indexing
            }
        });
    }

    public void updateUsername(int uid, String username) throws SQLException, BusyException {
        Objects.requireNonNull(username, "Username must not be null");
        if (uid <= 0) throw new IllegalArgumentException("UID must be > 0");

        sql.execute("INSERT INTO " + table + " (time,uid,username) VALUES (?,?,?)", 3000L, System.currentTimeMillis(), uid, username);
        cacheUIDToUsername.remove(uid);
        cacheUsernameToUID.remove(username.toLowerCase());
        cache(uid, username);
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
        return getUIDAndUsername(username).map(Pair::getLeft);
    }

    public Optional<Pair<Integer, String>> getUIDAndUsername(String username) throws SQLException, BusyException {
        if (username == null) return Optional.empty();

        Integer cachedUID = cacheUsernameToUID.get(username.toLowerCase());
        String cachedUsername = cacheUIDToUsername.get(cachedUID);
        if (cachedUID != null && cachedUsername != null) {
            return Optional.of(new Pair<>(cachedUID, cachedUsername));
        }

        Optional<Pair<Integer, String>> uidName = sql.query(
                String.format("SELECT uid,username FROM %s WHERE username=? %s ORDER BY time DESC LIMIT 1", table, sql.isMySQL() ? "" : "COLLATE NOCASE"), rs -> {
                    if (!rs.next()) return Optional.empty();
                    return Optional.of(new Pair<>(rs.getInt(1), rs.getString(2)));
                }, 3000L, username);

        cache(uidName.map(Pair::getLeft).orElse(null), uidName.map(Pair::getRight).orElse(username));

        if (uidName.filter(p -> p.getLeft() != null && p.getRight() != null).isEmpty()) return Optional.empty();
        return uidName;
    }

    public Optional<UUID> getUUID(String username, SQLIDManager.UUIDText sqlIDManager) throws SQLException, BusyException {
        return sqlIDManager.getValueOpt(getUID(username).orElse(-1));
    }

    public Optional<String> getUsername(UUID uuid, SQLIDManager.UUIDText sqlIDManager) throws SQLException, BusyException {
        return getUsername(sqlIDManager.getIDOpt(uuid, false).orElse(-1));
    }
}
