package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ConnectionFunctionWithException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Deprecated
public class ConnectionPoolMySQL extends ConnectionPool {
    private final int poolSize;
    private final ConnectionSupplier newConnectionSupplier;

    private final BlockingQueue<Connection> connectionPool;
    private final Set<Connection> activeConnections = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final ThreadLocal<Connection> issuedConnections = new ThreadLocal<>();
    private final AtomicInteger countEstablishedConnections = new AtomicInteger();

    public ConnectionPoolMySQL(String host, String database, String user, String pwd, int poolSize) throws SQLException, ClassNotFoundException {
        super();
        if (poolSize <= 0) throw new IllegalArgumentException("Invalid poolSize, must be >0");
        this.poolSize = poolSize;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);

        // Validate DB name (letters, digits, underscores only)
        if (!database.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid database name: " + database);
        }

        newConnectionSupplier = () -> DriverManager.getConnection("jdbc:mysql://" + host + "/" + database, user, pwd);

        // Test if DB exists, else create
        try (Connection connection = newConnectionSupplier.get()) {
            testConnection(connection);
        } catch (SQLException e) {
            System.err.println("Error code: " + e.getErrorCode());
            try (Connection connection = DriverManager.getConnection("jdbc:mysql://" + host, user, pwd)) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "CREATE DATABASE " + database + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")) {
                    statement.execute();
                }
            }
        }

        // Pre-fill pool
        for (int i = 0; i < poolSize; i++) {
            connectionPool.add(newConnection());
        }
    }

    @Override
    protected <T> T executeWithException_(ConnectionFunctionWithException<T> connectionFunction, long wait) throws Exception {
        if (isClosing()) {
            throw new BusyException("Database closing");
        }

        Connection conn = issuedConnections.get();
        boolean firstInThread = (conn == null);

        if (firstInThread) {
            conn = getConnectionFromPool(wait);
            issuedConnections.set(conn);
        }

        try {
            var ret = connectionFunction.apply(conn);
            if (firstInThread && !conn.getAutoCommit()) {
                throw new IllegalStateException("Auto commit not re-enabled after transaction");
            }
            return ret;
        } finally {
            if (firstInThread) {
                issuedConnections.remove();

                if (isClosing() || connectionPool.size() >= poolSize) {
                    closeAndRemove(conn);
                } else {
                    if (conn.isClosed() || !testConnection(conn)) {
                        closeAndRemove(conn);
                        conn = newConnection(); // ensure pool refills
                    }

                    if (!connectionPool.offer(conn)) { // return back to pool
                        closeAndRemove(conn);
                    }
                }
            }
        }
    }

    private Connection newConnection() throws SQLException {
        Connection connection = newConnectionSupplier.get();
        activeConnections.add(connection);
        countEstablishedConnections.incrementAndGet();
        return connection;
    }

    private void closeAndRemove(Connection conn) {
        try {
            conn.close();
        } catch (SQLException ignored) {
        } finally {
            activeConnections.remove(conn);
            countEstablishedConnections.decrementAndGet();
        }
    }

    @Override
    public int getActiveConnections() {
        return activeConnections.size();
    }

    @Override
    public int getAllEstablishedConnections() {
        return countEstablishedConnections.get();
    }

    @Override
    public void closeInternal() {
        // Close idle pool connections
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            closeAndRemove(conn);
        }

        // Close active connections
        for (Connection active : activeConnections) {
            closeAndRemove(active);
        }
    }

    @Override
    protected void checkDriver() throws ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Normal MySQL driver not found, trying old...");
            Class.forName("com.mysql.jdbc.Driver");
        }
    }

    private Connection getConnectionFromPool(long wait) throws BusyException {
        try {
            Connection conn = connectionPool.poll(wait, TimeUnit.MILLISECONDS);
            if (conn == null) {
                throw new BusyException("Unable to obtain Connection from pool");
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusyException("Interrupted while waiting for a connection");
        }
    }

    @Override
    public boolean isMySQL() {
        return true;
    }
}
