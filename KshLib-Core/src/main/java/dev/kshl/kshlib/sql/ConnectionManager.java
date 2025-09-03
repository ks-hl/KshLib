package dev.kshl.kshlib.sql;

import com.mysql.cj.exceptions.MysqlErrorNumbers;
import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ThrowingFunction;
import dev.kshl.kshlib.function.ThrowingRunnable;
import dev.kshl.kshlib.function.ThrowingSupplier;
import dev.kshl.kshlib.llm.embed.AbstractEmbeddings;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class ConnectionManager implements Closeable, AutoCloseable {
    private final ConnectionPool connectionPool;
    private boolean closed;
    private boolean ready;
    private boolean shuttingDown;

    @SuppressWarnings("unused")
    public ConnectionManager(File sqliteFile) throws IOException, SQLException, ClassNotFoundException {
        this.connectionPool = new ConnectionPoolSQLite(sqliteFile);
    }

    @SuppressWarnings("unused")
    public ConnectionManager(String uri, String database, String user, String pwd, int poolSize) throws SQLException, ClassNotFoundException {
        this.connectionPool = new ConnectionPoolMySQL(uri, database, user, pwd, poolSize);
    }

    @SuppressWarnings("unused")
    public static String sanitize(String str) {
        return str.replaceAll("[^\\u0020-\\u007F]", "¿");
    }

    @SuppressWarnings("unused")
    public static String getSize(double bytes) {
        int oom = 0;
        while (bytes > 1024) {
            bytes /= 1024;
            oom++;
        }
        String out = switch (oom) {
            case 0 -> "B";
            case 1 -> "KB";
            case 2 -> "MB";
            case 3 -> "GB";
            case 4 -> "TB";
            default -> "";
        };
        return (Math.round(bytes * 100.0) / 100.0) + " " + out;
    }

    public static boolean isConstraintViolation(Exception e) {
        if (!(e instanceof SQLException sqlException)) return false;
        return Set.of(19, 1062, 1169, 4025).contains(sqlException.getErrorCode());
    }

    @SuppressWarnings("unused")
    public final void init() throws SQLException {
        if (ready) throw new IllegalStateException("Already initialized");
        try {
            connectionPool.consume(this::init, 1000000L);
        } catch (BusyException ignored) {
            // impossible (ready not marked true until after this)
        }
        ready = true;
        postInit();
    }

    public String autoincrement() {
        return isMySQL() ? "AUTO_INCREMENT" : "AUTOINCREMENT";
    }

    @SuppressWarnings("unused")
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
        debugSQLStatement(stmt, args);
        return execute(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(stmt)) {
                prepare(connection, pstmt, args);
                return pstmt.executeUpdate();
            }
        }, wait);
    }

    /**
     * @see PreparedStatement#executeUpdate()
     */
    public final int executeReturnGenerated(String stmt, long wait, Object... args) throws SQLException, BusyException {
        debugSQLStatement(stmt, args);
        return execute(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(stmt, Statement.RETURN_GENERATED_KEYS)) {
                prepare(connection, pstmt, args);
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            return -1;
        }, wait);
    }

    @SuppressWarnings("unused")
    public final ResultMap executeReturnMap(String stmt, long wait, Object... args) throws SQLException, BusyException {
        debugSQLStatement(stmt, args);
        return execute(stmt, preparedStatement -> {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return new ResultMap(this, rs);
            }
        }, wait, args);
    }

    //
    //   EXECUTE
    //

    /**
     * Same as {@link ConnectionManager#execute(ConnectionFunction, long)} with no return
     */
    public final void execute(ConnectionConsumer task, long wait) throws SQLException, BusyException {
        execute(connection -> {
            task.accept(connection);
            return null;
        }, wait);
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
        } catch (SQLException | BusyException e) {
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

    /**
     * @see PreparedStatement#execute()
     */
    public final void execute(String stmt, long wait, Object... args) throws SQLException, BusyException {
        execute(connection -> {
            execute(connection, stmt, args);
        }, wait);
    }

    /**
     * @see PreparedStatement#execute()
     */
    public final void execute(Connection connection, String stmt, Object... args) throws SQLException {
        debugSQLStatement(stmt, args);
        try (PreparedStatement pstmt = connection.prepareStatement(stmt)) {
            prepare(connection, pstmt, args);
            pstmt.execute();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public final <T> T execute(Connection connection, String statement, PreparedStatementFunction<T> function, Object... args) throws SQLException {
        debugSQLStatement(statement, args);
        try (PreparedStatement pstmt = connection.prepareStatement(statement)) {
            prepare(connection, pstmt, args);
            try {
                return function.apply(pstmt);
            } catch (BusyException e) {
                // unexpected
                throw new RuntimeException(e);
            }
        }
    }

    public final void execute(Connection connection, String statement, PreparedStatementConsumer function, Object... args) throws SQLException {
        execute(connection, statement, ppreparedStatement -> {
            function.consume(ppreparedStatement);
            return null;
        }, args);
    }

    @SuppressWarnings("unused")
    public final void execute(String statement, PreparedStatementConsumer function, long wait, Object... args) throws SQLException, BusyException {
        execute(statement, preparedStatement -> {
            function.consume(preparedStatement);
            return null;
        }, wait, args);
    }

    public final <T> T execute(String statement, PreparedStatementFunction<T> function, long wait, Object... args) throws SQLException, BusyException {
        debugSQLStatement(statement, args);
        return execute(connection -> {
            try (PreparedStatement pstmt = connection.prepareStatement(statement)) {
                prepare(connection, pstmt, args);
                return function.apply(pstmt);
            }
        }, wait);
    }

    public <T> void executeBatch(String statement, Collection<T> collection, ThrowingFunction<T, List<?>, SQLException> valueFunction, long wait) throws SQLException, BusyException {
        execute((ConnectionConsumer) connection -> executeBatch(connection, statement, collection, valueFunction), wait);
    }

    public <T> void executeBatch(Connection connection, String statement, Collection<T> collection, ThrowingFunction<T, List<?>, SQLException> valueFunction) throws SQLException {
        execute(connection, statement, ps -> {
            for (T element : collection) {
                List<?> values = valueFunction.apply(element);
                for (int i = 0; i < values.size(); i++) {
                    Object value = values.get(i);
                    prepare(connection, ps, i + 1, value);
                }
                ps.addBatch();
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
        return execute((ConnectionFunction<T>) connection -> query(connection, statement, resultSetFunction, args), wait);
    }

    @SuppressWarnings("unused")
    public <T> void query(String statement, ResultSetConsumer resultSetConsumer, long wait, Object... args) throws SQLException, BusyException {
        query(statement, resultSet -> {
            resultSetConsumer.consume(resultSet);
            return null;
        }, wait, args);
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public final <T> T query(Connection connection, String statement, ResultSetFunction<T> resultSetFunction, Object... args) throws SQLException {
        return execute(connection, statement, (PreparedStatementFunction<T>) preparedStatement -> query(preparedStatement, resultSetFunction), args);
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public final void query(Connection connection, String statement, ResultSetConsumer resultSetFunction, Object... args) throws SQLException {
        query(connection, statement, (ResultSetFunction<?>) rs -> {
            resultSetFunction.consume(rs);
            return null;
        }, args);
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

    public final void setBlob(Connection connection, PreparedStatement statement, int index, byte[] bytes) throws SQLException {
        if (isMySQL()) {
            Blob ablob = connection.createBlob();
            ablob.setBytes(1, bytes);
            statement.setBlob(index, ablob);
        } else {
            statement.setBytes(index, bytes);
        }
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
    public final String concat(String s1, String s2) {
        if (isMySQL()) {
            return "CONCAT(" + s1 + "," + s2 + ")";
        } else {
            return s1 + "||" + s2;
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
    protected final int count(String table, long wait) throws SQLException, BusyException {
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

    protected final String getCountStmt() {
        if (isMySQL()) {
            return "SELECT COUNT(*) FROM ";
        } else {
            return "SELECT COUNT(1) FROM ";
        }
    }

    protected void prepare(Connection connection, PreparedStatement preparedStatement, Object... args) throws SQLException {
        if (args == null) return;
        for (int i = 0; i < args.length; i++) {
            prepare(connection, preparedStatement, i + 1, args[i]);
        }
    }

    protected void prepare(Connection connection, PreparedStatement preparedStatement, int index, Object o) throws SQLException {
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
            setBlob(connection, preparedStatement, index, c);
        } else if (o instanceof Double c) {
            preparedStatement.setDouble(index, c);
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
        try {
            for (Object arg : args) {
                stmt = stmt.replaceFirst("\\?", arg.toString().replace("\\", "\\\\"));
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

    @SuppressWarnings("unused")
    public boolean isReady() {
        return ready;
    }

    public boolean tableExists(Connection connection, String table) throws SQLException {
        try {
            execute(connection, "SELECT 1 FROM " + table); // test table exists
            return true;
        } catch (SQLException e) {
            if (isMySQL()) {
                if (e.getErrorCode() == MysqlErrorNumbers.ER_NO_SUCH_TABLE) return false;
            } else {
                if (e.getMessage().contains("no such table: " + table)) return false;
            }
            throw e;
        }
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
}
