package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.concurrent.ConcurrentCollection;
import dev.kshl.kshlib.concurrent.ConcurrentHashMap;
import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionPoolMySQL extends ConnectionPool {
    private final int poolSize;
    private final ConnectionSupplier newConnectionSupplier;

    private final ConcurrentLinkedQueue<Connection> connectionPool = new ConcurrentLinkedQueue<>();
    private final ConcurrentCollection<Set<Connection>, Connection> activeConnections = new ConcurrentCollection<>(new HashSet<>());
    private final ConcurrentHashMap<Long, Connection> issuedConnections = new ConcurrentHashMap<>();
    private int countEstablishedConnections;

    public ConnectionPoolMySQL(String host, String database, String user, String pwd, int poolSize) throws SQLException, ClassNotFoundException {
        super();
        if (poolSize <= 0) throw new IllegalArgumentException("Invalid poolSize, must be >0");
        this.poolSize = poolSize;

        newConnectionSupplier = () -> DriverManager.getConnection("jdbc:mysql://" + host + "/" + database, user, pwd);
        try (Connection connection = newConnectionSupplier.get()) {
            testConnection(connection);
        } catch (SQLException e) {
            System.err.println("Error code: " + e.getErrorCode());
            try (Connection connection = DriverManager.getConnection("jdbc:mysql://" + host, user, pwd)) {
                try (PreparedStatement statement = connection.prepareStatement("CREATE DATABASE " + database)) {
                    statement.execute();
                }
            }
        }

        for (int i = 0; i < poolSize; i++) {
            connectionPool.add(newConnection());
        }
    }

    @Override
    public <T> T executeWithException(ConnectionFunctionWithException<T> connectionFunction, long wait) throws Exception {
        if (isClosing()) {
            throw new BusyException("Database closing");
        }
        final long threadID = Thread.currentThread().getId();
        Connection out = issuedConnections.get(threadID);
        boolean firstInThread = out == null;
        if (firstInThread) {
            out = getConnectionFromPool(wait);
            issuedConnections.put(threadID, out);
        }
        try {
            var ret = connectionFunction.apply(out);
            if (firstInThread && !out.getAutoCommit()) {
                throw new IllegalStateException("Auto commit not re-enabled after transaction");
            }
            return ret;
        } finally {
            if (firstInThread) {
                issuedConnections.remove(threadID);
                if (isClosing() || connectionPool.size() >= poolSize || !testConnection(out)) {
                    try {
                        out.close();
                    } catch (SQLException ignored) {
                    }
                    activeConnections.remove(out);
                } else {
                    connectionPool.add(out);
                }
            }
        }
    }

    public Connection newConnection() throws SQLException {
        Connection connection = newConnectionSupplier.get();
        activeConnections.add(connection);
        countEstablishedConnections++;
        return connection;
    }

    @Override
    public int getActiveConnections() {
        return activeConnections.size();
    }

    @Override
    public int getAllEstablishedConnections() {
        return countEstablishedConnections;
    }

    @Override
    public void closeInternal() {
        {
            Connection connection;
            while ((connection = connectionPool.poll()) != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }

        activeConnections.consumeForce(connections -> {
            for (Connection connection : connections) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        });
    }

    @Override
    protected void checkDriver() throws ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return;
        } catch (ClassNotFoundException ignored) {
            System.err.println("Normal MySQL driver not found, trying old...");
        }
        Class.forName("com.mysql.jdbc.Driver");
    }

    private final ReentrantLock getConnectionLock = new ReentrantLock();

    private Connection getConnectionFromPool(long wait) throws BusyException {
        final long startTime = System.currentTimeMillis();

        try {
            if (!getConnectionLock.tryLock(startTime, TimeUnit.MILLISECONDS)) {
                throw new BusyException("Unable to obtain Connection from pool");
            }
        } catch (InterruptedException ignored) {
        }

        try {
            while (System.currentTimeMillis() - startTime < wait) {
                Connection connection;
                if ((connection = connectionPool.poll()) != null) {
                    return connection;
                } else {
                    Thread.onSpinWait();
                }
            }
        } finally {
            getConnectionLock.unlock();
        }
        throw new BusyException("Unable to obtain Connection from pool");
    }
}