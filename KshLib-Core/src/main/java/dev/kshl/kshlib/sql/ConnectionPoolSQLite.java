package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ConnectionFunctionWithException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConnectionPoolSQLite extends ConnectionPool {
    private final Connection connection;
    private final ReentrantReadWriteLock.ReadLock r;
    private final ReentrantReadWriteLock.WriteLock w;

    public ConnectionPoolSQLite(File file) throws IOException, SQLException, ClassNotFoundException {
        super();

        if (file == null) {
            throw new FileNotFoundException("Null file provided");
        }

        if (file.getParentFile() != null) {
            if (!file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IOException("Failed to create parent directory for database.");
                }
            }
        }

        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        var lock = new ReentrantReadWriteLock(true);
        r = lock.readLock();
        w = lock.writeLock();
    }

    @Override
    public <T> T executeWithException_(ConnectionFunctionWithException<T> connectionFunction, long wait, boolean readOnly) throws Exception {
        if (isClosing()) {
            throw new BusyException("Database closing");
        }
        if (readOnly) {
            if (!r.tryLock(wait, TimeUnit.MILLISECONDS)) {
                throw new BusyException("Database busy");
            }
        } else {
            if (!w.tryLock(wait, TimeUnit.MILLISECONDS)) {
                throw new BusyException("Database busy");
            }
        }
        try {
            var ret = connectionFunction.apply(connection);
            if (w.getHoldCount() == 1 && !connection.getAutoCommit()) {
                throw new IllegalStateException("Auto commit not re-enabled after transaction");
            }
            return ret;
        } finally {
            if (readOnly) {
                r.unlock();
            } else {
                w.unlock();
            }
        }
    }

    @Override
    public int getActiveConnections() {
        return 1;
    }

    @Override
    public int getAllEstablishedConnections() {
        return 1;
    }

    @Override
    public void closeInternal() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    @Override
    protected void checkDriver() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }

    @Override
    public boolean isMySQL() {
        return false;
    }
}
