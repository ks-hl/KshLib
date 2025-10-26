package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.MapCache;
import dev.kshl.kshlib.misc.Pair;
import dev.kshl.kshlib.misc.TreeSetString;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SQLUsernameManager implements ISQLManager {

    private final ConnectionManager sql;
    private final String table;
    private final MapCache<Integer, String> cacheUIDToUsername = new MapCache<>(1, TimeUnit.HOURS);
    private final MapCache<String, Integer> cacheUsernameToUID = new MapCache<>(1, TimeUnit.HOURS);
    private final TreeSetString recentUsernames = new TreeSetString.CaseInsensitive();
    private boolean recentUsernamesInitialized;

    public SQLUsernameManager(ConnectionManager sql, String table) {
        validateTableName(table);

        this.sql = sql;
        this.table = ConnectionManager.validateTableName(table);
    }

    public void init(Connection connection) throws SQLException {
        sql.executeTransaction(connection, () -> {
            boolean needsMigration = sql.tableExists(connection, table) && !sql.uniqueConstraintExists(connection, table, "uid", "username");
            if (needsMigration) {
                sql.execute(connection, "DROP TABLE IF EXISTS " + table + "_temp");
                sql.execute(connection, "ALTER TABLE " + table + " RENAME TO " + table + "_temp");
            }

            sql.execute(connection, String.format("""
                    CREATE TABLE IF NOT EXISTS %s (
                        time BIGINT,
                        uid INTEGER,
                        username VARCHAR(255)%s,
                        UNIQUE(uid,username)
                    )""", table, sql.isMySQL() ? " COLLATE utf8mb4_general_ci" : "")); // utf8mb4_general_ci gives case-insensitive indexing to MySQL

            if (needsMigration) {
                sql.execute(connection, "INSERT OR IGNORE INTO " + table + " SELECT * FROM " + table + "_temp ORDER BY TIME DESC");
                sql.execute(connection, "DROP TABLE IF EXISTS " + table + "_temp");
            }

            if (sql.indexExists(connection, table, "idx_time")) {
                if (sql.isMySQL()) {
                    sql.execute(connection, "DROP INDEX IF EXISTS idx_time ON " + table);
                } else {
                    sql.execute(connection, "DROP INDEX IF EXISTS idx_time");
                }
            }
            sql.execute(connection, String.format("CREATE INDEX IF NOT EXISTS idx_%s_time ON %s (time)", table, table));
            sql.execute(connection, String.format("CREATE INDEX IF NOT EXISTS idx_%s_uid_time ON %s (uid, time DESC)", table, table));

            // Case-insensitive username index
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

        String stored = getUsername(uid).orElse(null);
        long now = System.currentTimeMillis();
        if (sql.isMySQL()) {
            sql.execute(String.format("""
                    INSERT INTO %s (time, uid, username)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE time = VALUES(time)
                    """, table), 3000L, now, uid, username);
        } else {
            sql.execute(String.format("""
                    INSERT INTO %s (time, uid, username)
                    VALUES (?, ?, ?)
                    ON CONFLICT(uid, username) DO UPDATE SET time = excluded.time
                    """, table), 3000L, now, uid, username);
        }

        if (stored != null && !stored.equals(username)) {
            recentUsernames.remove(stored);
        }
        cacheUIDToUsername.remove(uid);
        cacheUsernameToUID.remove(username.toLowerCase());
        cache(uid, username);
    }

    void cache(Integer uid, String username) {
        if (uid != null) {
            cacheUIDToUsername.put(uid, username);
        }
        if (username != null) {
            cacheUsernameToUID.put(username.toLowerCase(), uid);
        }
        if (username == null) return;
        synchronized (recentUsernames) {
            recentUsernames.add(username);
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

    /**
     * Populates a TreeSet with recent usernames so it may be queried for command prefix uses
     *
     * @param limit The limit to retrieve from the database. It can grow larger than this as new usernames are added during runtime. 5,000 is a good starting point, this will use around 450KB of memory, so it's not insignificant.
     */
    public void populateRecentUsernames(Connection connection, int limit) throws SQLException {
        synchronized (recentUsernames) {
            if (recentUsernamesInitialized) {
                throw new IllegalStateException("recentUsernames already initialized");
            }
            sql.query(connection, String.format("""
                    SELECT username FROM (
                      SELECT username, ROW_NUMBER()
                      OVER (PARTITION BY uid ORDER BY time DESC) AS rn
                      FROM %s
                    ) t
                    WHERE rn = 1
                    LIMIT ?
                    """, table), rs -> {
                while (rs.next()) {
                    String name = rs.getString(1);
                    if (name == null) continue;
                    recentUsernames.add(name);
                }
            }, limit);
            recentUsernamesInitialized = true;
        }
    }

    public Set<String> getRecentUsernamesStartingWith(String prefix) {
        synchronized (recentUsernames) {
            if (!recentUsernamesInitialized) {
                throw new IllegalStateException("recentUsernames not initialized. Call populateRecentUsernames first.");
            }
            return Collections.unmodifiableSet(recentUsernames.getSubList(prefix));
        }
    }

    public int getRecentUsernamesSize() {
        synchronized (recentUsernames) {
            if (!recentUsernamesInitialized) {
                throw new IllegalStateException("recentUsernames not initialized. Call populateRecentUsernames first.");
            }
            return recentUsernames.size();
        }
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
                String.format("""
                        SELECT u.uid, u.username
                        FROM %1$s u
                        WHERE u.uid = (
                            SELECT uid FROM %1$s
                            WHERE username = ? %2$s
                            ORDER BY time DESC LIMIT 1
                        )
                        ORDER BY u.time DESC
                        LIMIT 1
                        """, table, sql.isMySQL() ? "" : "COLLATE NOCASE"),
                rs -> {
                    if (!rs.next()) return Optional.empty();
                    return Optional.of(new Pair<>(rs.getInt(1), rs.getString(2)));
                },
                3000L, username
        );

        cache(uidName.map(Pair::getLeft).orElse(null), uidName.map(Pair::getRight).orElse(username));

        if (uidName.filter(p -> p.getLeft() != null && p.getRight() != null).isEmpty()) return Optional.empty();
        return uidName;
    }

    public Optional<Pair<UUID, String>> getUUIDAndUsername(String username, SQLIDManager.UUIDText sqlIDManager) throws SQLException, BusyException {
        return getUIDAndUsername(username).map((Function<? super Pair<Integer, String>, ? extends Pair<UUID, String>>) p -> {
            try {
                return new Pair<>(sqlIDManager.getValueOpt(p.getLeft()).orElseThrow(), p.getRight());
            } catch (SQLException | BusyException e) {
                return null;
            }
        });
    }

    public Optional<UUID> getUUID(String username, SQLIDManager.UUIDText sqlIDManager) throws SQLException, BusyException {
        return sqlIDManager.getValueOpt(getUID(username).orElse(-1));
    }

    public Optional<String> getUsername(UUID uuid, SQLIDManager.UUIDText sqlIDManager) throws SQLException, BusyException {
        return getUsername(sqlIDManager.getIDOpt(uuid, false).orElse(-1));
    }
}
