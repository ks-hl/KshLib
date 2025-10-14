package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ConnectionConsumer;
import dev.kshl.kshlib.function.ConnectionFunction;
import dev.kshl.kshlib.function.ConnectionFunctionWithException;
import dev.kshl.kshlib.function.PreparedStatementConsumer;
import dev.kshl.kshlib.function.PreparedStatementFunction;
import dev.kshl.kshlib.function.ResultSetConsumer;
import dev.kshl.kshlib.function.ResultSetFunction;
import dev.kshl.kshlib.function.ThrowingFunction;
import dev.kshl.kshlib.function.ThrowingRunnable;
import dev.kshl.kshlib.function.ThrowingSupplier;
import dev.kshl.kshlib.llm.embed.AbstractEmbeddings;
import lombok.Getter;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ConnectionManager implements Closeable, AutoCloseable {
    private final ConnectionPool connectionPool;
    @Getter
    private boolean closed;
    @Getter
    private boolean ready;
    private boolean shuttingDown;
    private final CompletableFuture<Void> readyCompletable = new CompletableFuture<>();

    public ConnectionManager(File sqliteFile) throws IOException, SQLException, ClassNotFoundException {
        this(sqliteFile, null, null, null, null, 0);
    }

    public ConnectionManager(String uri, String database, String user, String password, int poolSize) throws IOException, SQLException, ClassNotFoundException {
        this(null, uri, database, user, password, poolSize);
    }

    public ConnectionManager(@Nullable File sqliteFile, @Nullable String hostAndPort, @Nullable String database, @Nullable String user, @Nullable String password, int poolSize) throws ClassNotFoundException, SQLException, IOException {
        if (sqliteFile != null) {
            this.connectionPool = new ConnectionPoolSQLite(sqliteFile);
        } else if (hostAndPort != null) {
            this.connectionPool = new ConnectionPoolHikari(hostAndPort, database, user, password, poolSize);
        } else {
            throw new NullPointerException("sqliteFile or hostAndPort must be not null");
        }
    }

    @SuppressWarnings("unused")
    public static String sanitize(String str) {
        return str.replaceAll("[^\\u0020-\\u007F]", "¿");
    }

    public boolean isConstraintViolation(Exception e) {
        if (!(e instanceof SQLException sqlException)) return false;

        for (SQLException cur = sqlException; cur != null; cur = cur.getNextException()) {
            String state = cur.getSQLState();
            if (state != null && state.startsWith("23")) return true;
        }

        final int code = sqlException.getErrorCode();
        if (isMySQL()) {
            return switch (code) {
                case 1062, // duplicate key
                     1022, // duplicate key (ALTER TABLE, etc.)
                     1451, // cannot delete/update parent row: FK constraint
                     1452, // cannot add/update child row: FK constraint
                     3819 // CHECK constraint failed
                        -> true;
                default -> false;
            };
        } else {
            return switch (code) {
                case 19, // SQLITE_CONSTRAINT (generic)
                     1555, // SQLITE_CONSTRAINT_PRIMARYKEY
                     2067, // SQLITE_CONSTRAINT_UNIQUE
                     275, // SQLITE_CONSTRAINT_CHECK
                     787, // SQLITE_CONSTRAINT_FOREIGNKEY
                     1299 // SQLITE_CONSTRAINT_NOTNULL
                        -> true;
                default -> false;
            };
        }
    }

    public final void init() throws SQLException {
        if (ready) throw new IllegalStateException("Already initialized");
        try {
            connectionPool.consume(this::init, 1000000L);
        } catch (BusyException ignored) {
            // impossible (ready not marked true until after this)
        }
        ready = true;
        readyCompletable.complete(null);
        postInit();
    }

    @SuppressWarnings("unused")
    public String autoincrement() {
        return isMySQL() ? "AUTO_INCREMENT" : "AUTOINCREMENT";
    }

    @SuppressWarnings("unused")
    public String indexedBy(String index) {
        return String.format(isMySQL() ? "USE INDEX (%s)" : "INDEXED BY %s", index);
    }

    protected abstract void init(Connection connection) throws SQLException;

    protected void postInit() throws SQLException {
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;

        connectionPool.close();
    }

    /**
     * Same as {@link ConnectionManager#execute(ConnectionFunction, long)} but with any Exception
     *
     * @throws Exception For Exception thrown by the task
     */
    public final <T> T executeWithException(ConnectionFunctionWithException<T> task, long wait) throws Exception {
        if (closed) throw new IllegalStateException("closed");
        if (!ready) throw new IllegalStateException("Not yet initialized");
        checkAsync_();

        return connectionPool.executeWithException(task, wait);
    }

    /**
     * @see PreparedStatement#executeUpdate()
     */
    @SuppressWarnings("unused")
    public final int executeReturnRows(String stmt, long wait, Object... args) throws SQLException, BusyException {
        return execute(stmt).args(args).executeReturnRows(wait);
    }

    /**
     * @see PreparedStatement#executeUpdate()
     */
    public final int executeReturnRows(Connection connection, String stmt, Object... args) throws SQLException {
        return execute(stmt).args(args).executeReturnRows(connection);
    }

    /**
     * @see PreparedStatement#executeUpdate()
     */
    public final int executeReturnGenerated(String stmt, long wait, Object... args) throws SQLException, BusyException {
        return execute(stmt).args(args).executeReturnGenerated(wait);
    }

    /**
     * @see PreparedStatement#executeUpdate()
     */
    public final int executeReturnGenerated(Connection connection, String stmt, Object... args) throws SQLException {
        return execute(stmt).args(args).executeReturnGenerated(connection);
    }

    @SuppressWarnings("unused")
    public final ResultMap executeReturnMap(String stmt, long wait, Object... args) throws SQLException, BusyException {
        return applyResultSet(stmt, rs -> new ResultMap(this, rs)).args(args).executeQuery(wait);
    }

    //
    //   EXECUTE
    //

    /**
     * Same as {@link ConnectionManager#execute(ConnectionFunction, long)} with no return
     */
    public final void execute(ConnectionConsumer task, long wait) throws SQLException, BusyException {
        accept(task).executeQuery(wait);
    }

    /**
     * Executes a given task under a write-lock, providing the writeable Connection, then returns a value
     *
     * @param task The task to be executed under the write-lock
     * @param wait How long to wait for a lock
     * @return The value returned by the task
     * @throws BusyException If the wait time is exceeded
     * @throws SQLException  For SQLException thrown by the task
     */
    public final <T> T execute(ConnectionFunction<T> task, long wait) throws SQLException, BusyException {
        try {
            return executeWithException(task::apply, wait);
        } catch (SQLException | BusyException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Same as {@link ConnectionManager#execute(ConnectionFunction, long)} with no return
     */
    public final void executeTransaction(ConnectionConsumer task, long wait) throws SQLException, BusyException {
        executeTransaction(connection -> {
            task.accept(connection);
            return null;
        }, wait);
    }

    /**
     * Executes a given task under a write-lock, providing the writeable Connection, then returns a value, all within a transaction. If a SQLException is thrown by the function, the transaction will be rolled back, otherwise it will be committed.
     *
     * @param task The task to be executed under the write-lock
     * @param wait How long to wait for a lock
     * @return The value returned by the task
     * @throws BusyException If the wait time is exceeded
     * @throws SQLException  For SQLException thrown by the task
     */
    public final <T> T executeTransaction(ConnectionFunction<T> task, long wait) throws SQLException, BusyException {
        return execute((ConnectionFunction<T>) connection -> executeTransaction(connection, () -> task.apply(connection)), wait);
    }

    /**
     * Same as {@link ConnectionManager#execute(ConnectionFunction, long)} with no return
     */
    public final void executeTransaction(Connection connection, ThrowingRunnable<Exception> task) throws SQLException {
        executeTransaction(connection, () -> {
            task.run();
            return null;
        });
    }

    /**
     * Executes a given task under a write-lock, providing the writeable Connection, then returns a value, all within a transaction. If a SQLException is thrown by the function, the transaction will be rolled back, otherwise it will be committed.
     *
     * @param connection The connection to use
     * @param task       The task to be executed under the write-lock
     * @return The value returned by the task
     * @throws SQLException For SQLException thrown by the task
     */
    public final <T> T executeTransaction(Connection connection, ThrowingSupplier<T, Exception> task) throws SQLException {
        connection.setAutoCommit(false);

        try {
            return task.get();
        } catch (Throwable e) {
            connection.rollback();
            connection.setAutoCommit(true);
            if (e instanceof SQLException sqlException) throw sqlException;
            else if (e instanceof RuntimeException runtimeException) throw runtimeException;
            else throw new RuntimeException(e);
        } finally {
            if (!connection.getAutoCommit()) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        }
    }


    public final void execute(String stmt, long wait, Object... args) throws SQLException, BusyException {
        execute(stmt).args(args).executeQuery(wait);
    }


    public final void execute(Connection connection, String stmt, Object... args) throws SQLException {
        execute(stmt).args(args).executeQuery(connection);
    }


    public final <T> T execute(Connection connection, String statement, PreparedStatementFunction<T> function) throws SQLException {
        return applyPreparedStatement(statement, function).executeQuery(connection);
    }


    public final void execute(Connection connection, String statement, PreparedStatementConsumer consumer) throws SQLException {
        acceptPreparedStatement(statement, consumer).executeQuery(connection);
    }

    public final void execute(String statement, PreparedStatementConsumer consumer, long wait) throws SQLException, BusyException {
        acceptPreparedStatement(statement, consumer).executeQuery(wait);
    }


    public final <T> T execute(String statement, PreparedStatementFunction<T> function, long wait) throws SQLException, BusyException {
        return applyPreparedStatement(statement, function).executeQuery(wait);
    }

    public <T> void executeBatch(String statement, Collection<T> collection, ThrowingFunction<T, List<?>, SQLException> valueFunction, long wait) throws SQLException, BusyException {
        execute((ConnectionConsumer) connection -> executeBatch(connection, statement, collection, valueFunction), wait);
    }

    public <T> void executeBatch(Connection connection, String statement, Collection<T> collection, ThrowingFunction<T, List<?>, SQLException> valueFunction) throws SQLException {
        executeTransaction(connection, () -> {
            try (PreparedStatement ps = connection.prepareStatement(statement)) {
                for (T element : collection) {
                    List<?> values = valueFunction.apply(element);
                    for (int i = 0; i < values.size(); i++) {
                        Object value = values.get(i);
                        prepare(ps, i + 1, value);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        });
    }

    //
    // QUERY
    //


    @SuppressWarnings("unused")
    public static <T> T query(PreparedStatement preparedStatement, ResultSetFunction<T> resultSetFunction) throws SQLException, BusyException {
        try (ResultSet rs = preparedStatement.executeQuery()) {
            return resultSetFunction.apply(rs);
        }
    }


    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public <T> T query(String statement, ResultSetFunction<T> resultSetFunction, long wait, Object... args) throws SQLException, BusyException {
        return applyResultSet(statement, resultSetFunction).args(args).executeQuery(wait);
    }


    @SuppressWarnings("unused")
    public <T> void query(String statement, ResultSetConsumer resultSetConsumer, long wait, Object... args) throws SQLException, BusyException {
        acceptResultSet(statement, resultSetConsumer).args(args).executeQuery(wait);
    }


    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public final <T> T query(Connection connection, String statement, ResultSetFunction<T> resultSetFunction, Object... args) throws SQLException {
        return applyResultSet(statement, resultSetFunction).args(args).executeQuery(connection);
    }


    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public final void query(Connection connection, String statement, ResultSetConsumer resultSetConsumer, Object... args) throws SQLException {
        acceptResultSet(statement, resultSetConsumer).args(args).executeQuery(connection);
    }

    /**
     * A helper method of {@link this#query(String, ResultSetConsumer, long, Object...)}
     * which retrieves all elements from the ResultSet and maps them to a Stream of {@link T}
     * as specified by the resultSetFunction
     *
     * @param statement         The SQL statement
     * @param resultSetFunction The function to convert the ResultSet to ONE instance of {@link T}
     * @param wait              How long to wait
     * @param args              SQL parameters
     * @param <T>               The type of object
     */
    public final <T> Stream<T> queryAll(String statement, ResultSetFunction<T> resultSetFunction, long wait, Object... args) throws SQLException, BusyException {
        return execute(connection -> {
            return queryAll(connection, statement, resultSetFunction, args);
        }, wait);
    }

    public final <T> Stream<T> queryAll(Connection connection, String statement, ResultSetFunction<T> resultSetFunction, Object... args) throws SQLException, BusyException {
        Stream.Builder<T> stream = Stream.builder();
        query(connection, statement, rs -> {
            while (rs.next()) {
                stream.add(resultSetFunction.apply(rs));
            }
        }, args);
        return stream.build();
    }

    @SuppressWarnings("unused")
    public final byte[] getBlob(ResultSet rs, String key) throws SQLException {
        if (isMySQL()) {
            try (InputStream in = rs.getBlob(key).getBinaryStream()) {
                return in.readAllBytes();
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        } else {
            return rs.getBytes(key);
        }
    }

    public final byte[] getBlob(ResultSet rs, int index) throws SQLException {
        if (isMySQL()) {
            try (InputStream in = rs.getBlob(index).getBinaryStream()) {
                return in.readAllBytes();
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        } else {
            return rs.getBytes(index);
        }
    }

    @SuppressWarnings("unused")
    public final String concat(String... strings) {
        if (strings == null || strings.length < 2) {
            throw new IllegalArgumentException("Must provide 2 or more strings to concatenate");
        }
        if (isMySQL()) {
            return "CONCAT(" + String.join(",", strings) + ")";
        } else {
            return "(" + String.join("||", strings) + ")";
        }
    }

    @SuppressWarnings("unused")
    public final int count(Connection connection, String table) throws SQLException {
        String stmtStr = getCountStmt() + table;
        debugSQLStatement(stmtStr, table);
        return execute(connection, stmtStr, preparedStatement -> {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return -1;
            }
        });
    }

    @SuppressWarnings("unused")
    public final int count(String table, long wait) throws SQLException, BusyException {
        return execute((ConnectionFunction<Integer>) connection -> count(connection, table), wait);
    }

    public long hash(String table, String orderByColumn, Predicate<String> doIncludeColumn) throws SQLException, BusyException {
        return query("SELECT * FROM " + table + " ORDER BY " + orderByColumn, rs -> {
            List<String> columns = new ArrayList<>();
            for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                String columnName = rs.getMetaData().getColumnName(i + 1);
                if (!doIncludeColumn.test(columnName)) continue;
                columns.add(columnName);
            }
            columns.sort(String::compareTo);
            long hash = 0;
            for (String column : columns) hash = hash * 31 + column.hashCode();

            while (rs.next()) {
                for (String column : columns) {
                    hash *= 31;
                    hash += Objects.hash(rs.getObject(column));
                }
            }
            return hash;
        }, 10000L);
    }

    public List<Map.Entry<String, AtomicLong>> hashEachColumn(String table, String orderByColumn) throws SQLException, BusyException {
        return query("SELECT * FROM " + table + " ORDER BY " + orderByColumn, rs -> {
            List<String> columns = new ArrayList<>();
            for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                columns.add(rs.getMetaData().getColumnName(i + 1));
            }
            columns.sort(String::compareTo);
            Map<String, AtomicLong> hash = new HashMap<>();
            while (rs.next()) {
                for (String column : columns) {
                    AtomicLong columnHash = hash.computeIfAbsent(column, c -> new AtomicLong());
                    Object value = rs.getObject(column);
                    columnHash.getAndUpdate(l -> l * 31 + Objects.hash(value));
                }
            }
            var out = new ArrayList<>(hash.entrySet());
            out.sort(Map.Entry.comparingByKey());
            return out;
        }, 10000L);
    }

    public List<String> listTables() throws SQLException, BusyException {
        String statement;
        if (isMySQL()) statement = "SHOW TABLES";
        else statement = "SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%'";

        return query(statement, rs -> {
            List<String> out = new ArrayList<>();
            while (rs.next()) {
                out.add(rs.getString(1));
            }
            out.sort(String::compareTo);
            return out;
        }, 10000L);
    }

    public final String getCountStmt() {
        if (isMySQL()) {
            return "SELECT COUNT(*) FROM ";
        } else {
            return "SELECT COUNT(1) FROM ";
        }
    }

    public void prepare(PreparedStatement preparedStatement, Object... args) throws SQLException {
        if (args == null) return;

        for (int i = 0; i < args.length; i++) {
            try {
                prepare(preparedStatement, i + 1, args[i]);
            } catch (SQLException e) {
                if (!e.getMessage().contains("index out of range")) throw e;
                throw new ArrayIndexOutOfBoundsException(i);
            }
        }
    }

    public void prepare(PreparedStatement preparedStatement, int index, Object o) throws SQLException {
        if (o instanceof AbstractEmbeddings embeddings) {
            o = embeddings.toString();
        }
        if (o == null) {
            preparedStatement.setNull(index, Types.NULL);
        } else if (o instanceof String c) {
            preparedStatement.setString(index, c);
        } else if (o instanceof Integer c) {
            preparedStatement.setInt(index, c);
        } else if (o instanceof Long c) {
            preparedStatement.setLong(index, c);
        } else if (o instanceof Short s) {
            preparedStatement.setShort(index, s);
        } else if (o instanceof Boolean c) {
            preparedStatement.setBoolean(index, c);
        } else if (o instanceof Byte b) {
            preparedStatement.setByte(index, b);
        } else if (o instanceof byte[] c) {
            preparedStatement.setBytes(index, c);
        } else if (o instanceof Double c) {
            preparedStatement.setDouble(index, c);
        } else if (o instanceof Float f) {
            preparedStatement.setFloat(index, f);
        } else {
            throw new IllegalArgumentException(o.toString());
        }
    }

    /**
     * Handles debug Strings. Only called if {@link #isDebug()} returns true.
     *
     * @param line The line to print
     */
    protected abstract void debug(String line);

    /**
     * @return If the current thread is allowed to be used for database calls
     */
    protected abstract boolean checkAsync();

    private void checkAsync_() throws IllegalStateException {
        if (shuttingDown) return;
        if (checkAsync()) return;
        throw new IllegalStateException("Synchronous call to database.");
    }

    protected void debugSQLStatement(String stmt, Object... args) {
        if (!isDebug()) return;
        final String originalStmt = stmt;
        if (args == null) args = new Object[0];
        try {
            for (Object arg : args) {
                stmt = stmt.replaceFirst("\\?", Objects.toString(arg).replace("\\", "\\\\"));
            }
        } catch (Exception e) {
            stmt = originalStmt + ": ";
            boolean first = true;
            StringBuilder stmtBuilder = new StringBuilder(stmt);
            for (Object o : args) {
                if (first) first = false;
                else stmtBuilder.append(", ");
                stmtBuilder.append(o);
            }
            stmt = stmtBuilder.toString();
        }
        debug(stmt);
    }

    public final boolean isMySQL() {
        return connectionPool.isMySQL();
    }

    /**
     * @return Whether to generate debug statements and call {@link #debug(String)}
     */
    protected abstract boolean isDebug();

    public boolean tableExists(String table) throws SQLException, BusyException {
        return execute((ConnectionFunction<Boolean>) connection -> tableExists(connection, table), 3000L);
    }

    public boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    public boolean columnExists(String table, String column) throws SQLException, BusyException {
        return execute((ConnectionFunction<Boolean>) connection -> columnExists(connection, table, column), 3000L);
    }

    public boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    public boolean primaryKeyExists(String table) throws SQLException, BusyException {
        return execute((ConnectionFunction<Boolean>) connection -> primaryKeyExists(connection, table), 3000L);
    }

    public boolean primaryKeyExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getPrimaryKeys(null, null, table)) {
            return rs.next();
        }
    }

    public boolean indexExists(String table, String index) throws SQLException, BusyException {
        return execute((ConnectionFunction<Boolean>) connection -> indexExists(connection, table, index), 3000L);
    }

    public boolean indexExists(Connection connection, String table, String index) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getIndexInfo(null, null, table, false, false)) {
            while (rs.next()) {
                String idxName = rs.getString("INDEX_NAME");
                if (idxName != null && idxName.equalsIgnoreCase(index)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean uniqueConstraintExists(String table, String... columns) throws SQLException, BusyException {
        return execute((ConnectionFunction<Boolean>) connection -> uniqueConstraintExists(connection, table, columns), 3000L);
    }

    public boolean uniqueConstraintExists(Connection connection, String table, String... columns) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        Set<String> requested = Arrays.stream(columns)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Map of indexName -> set of columns in that index
        Map<String, Set<String>> indexColumns = new HashMap<>();

        try (ResultSet rs = meta.getIndexInfo(null, null, table, true, false)) {
            while (rs.next()) {
                boolean nonUnique = rs.getBoolean("NON_UNIQUE"); // false means unique
                if (nonUnique) continue;

                String idxName = rs.getString("INDEX_NAME");
                String colName = rs.getString("COLUMN_NAME");

                if (idxName != null && colName != null) {
                    indexColumns.computeIfAbsent(idxName, k -> new HashSet<>())
                            .add(colName.toLowerCase());
                }
            }
        }

        // Check if any unique index fully covers all requested columns
        for (Set<String> cols : indexColumns.values()) {
            if (cols.equals(requested)) {
                return true;
            }
        }
        return false;
    }

    public boolean isWithoutRowID(String table, long waitMillis) throws SQLException, BusyException {
        if (isMySQL()) throw new IllegalStateException("'WITHOUT ROWID' does not exist on MySQL");
        return execute((ConnectionFunction<Boolean>) connection -> isWithoutRowID(connection, table), waitMillis);
    }

    public boolean isWithoutRowID(Connection connection, String table) throws SQLException {
        if (isMySQL()) throw new IllegalStateException("'WITHOUT ROWID' does not exist on MySQL");
        String ddl = query(connection, "SELECT sql FROM sqlite_master WHERE type='table' AND name=?", rs -> {
            if (!rs.next()) return null;
            return rs.getString(1);
        }, table);
        return ddl != null && ddl.toUpperCase().contains("WITHOUT ROWID");
    }

    /**
     * Starts the build process for a statement which provides a Connection and expects and return {@param T}
     */
    @CheckReturnValue
    public <T> StatementBuilder<T> apply(ConnectionFunction<T> function) {
        return new StatementBuilder<>(this, function);
    }

    /**
     * Starts the build process for a statement which provides a Connection
     */
    @CheckReturnValue
    public StatementBuilder<Void> accept(ConnectionConsumer consumer) {
        return apply(rs -> {
            consumer.accept(rs);
            return null;
        });
    }

    /**
     * Starts the build process for a statement which provides a PreparedStatement build from the provided statement string and returns {@param T}
     */
    @CheckReturnValue
    public <T> StatementBuilder<T> applyPreparedStatement(String statement, PreparedStatementFunction<T> function) {
        return new StatementBuilder<>(this, statement, function);
    }

    /**
     * Starts the build process for a statement which provides a PreparedStatement build from the provided statement string
     */
    @CheckReturnValue
    public StatementBuilder<Void> acceptPreparedStatement(String statement, PreparedStatementConsumer consumer) {
        return applyPreparedStatement(statement, rs -> {
            consumer.consume(rs);
            return null;
        });
    }

    /**
     * Starts the build process for a statement which provides a ResultSet after executing the provided statement string in the specified way and returns {@param T}
     */
    @CheckReturnValue
    public <T> StatementBuilder<T> applyResultSet(String statement, ResultSetFunction<T> function) {
        return new StatementBuilder<>(this, statement, function);
    }

    /**
     * Starts the build process for a statement which provides a ResultSet after executing the provided statement string in the specified way
     */
    @CheckReturnValue
    public StatementBuilder<Void> acceptResultSet(String statement, ResultSetConsumer consumer) {
        return applyResultSet(statement, rs -> {
            consumer.consume(rs);
            return null;
        });
    }

    /**
     * Starts the build process for a statement which simply executes a statement string
     */
    @CheckReturnValue
    public StatementBuilder<Void> execute(String statement) {
        return new StatementBuilder<>(this, statement);
    }

    @Override
    public String toString() {
        return String.format("ConnectionManager[type=%s,ready=%s,closed=%s]", isMySQL() ? "mysql" : "sqlite", isReady(), closed);
    }

    /**
     * Allows Synchronous calls to the database to allow for shutdown tasks
     */
    public void markAsShuttingDown() {
        shuttingDown = true;
    }

    /**
     * The ratio of time this pool has been actively accessing the database in the past [5 minutes], [1 minute], and [5 seconds]
     * Note: This may be >1 if there are multiple Connections
     */
    public double[] getUsageTimeRatios() {
        return connectionPool.getUsageTimeRatios();
    }

    public int getActiveConnections() {
        return connectionPool.getActiveConnections();
    }

    public int getAllEstablishedConnections() {
        return connectionPool.getAllEstablishedConnections();
    }

    void checkEnoughArguments(PreparedStatement preparedStatement, Object... args) throws SQLException {
        int parameters = -1;
        try {
            parameters = preparedStatement.getParameterMetaData().getParameterCount();
        } catch (SQLFeatureNotSupportedException ignored) {
        }
        if (args == null) args = new Object[0];
        if (parameters > args.length) {
            throw new IllegalArgumentException(String.format("Not enough arguments provided (%s) for the number of parameters (%s) in the query.", args.length, parameters));
        }
    }

    private static final String TABLE_NAME_REGEX = "[a-zA-Z0-9_]+";

    public static String validateTableName(String tableName) {
        if (!tableName.matches(TABLE_NAME_REGEX)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName + ", must match " + TABLE_NAME_REGEX);
        }
        return tableName;
    }

    public final CompletionStage<Void> whenInitialized() {
        return readyCompletable.minimalCompletionStage();
    }
}
