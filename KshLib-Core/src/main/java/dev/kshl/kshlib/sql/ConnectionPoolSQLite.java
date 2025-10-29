package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.concurrent.ConcurrentArrayList;
import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ConnectionFunctionWithException;
import dev.kshl.kshlib.function.ThrowingSupplier;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class ConnectionPoolSQLite extends ConnectionPool {
    private final Connection writeConnection;
    private final BlockingQueue<Connection> readConnections = new ArrayBlockingQueue<>(4);
    private final List<Connection> allReadConnections = new ConcurrentArrayList<>();
    private int establishedConnections;

    private final ReentrantReadWriteLock.ReadLock r;
    private final ReentrantReadWriteLock.WriteLock w;

    public ConnectionPoolSQLite(File file) throws IOException, SQLException, ClassNotFoundException {
        super();

        if (file == null) {
            throw new FileNotFoundException("Null file provided");
        }

        if (file.getParentFile() != null && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IOException("Failed to create parent directory for database.");
        }

        // WRITE
        {
            SQLiteConfig writeConfig = new SQLiteConfig();
            writeConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
            writeConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
            writeConfig.setBusyTimeout(5000);
            writeConfig.setPragma(SQLiteConfig.Pragma.FOREIGN_KEYS, "ON");
            this.writeConnection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath(), writeConfig.toProperties());
            establishedConnections++;
        }

        // READ
        {
            SQLiteConfig readConfig = new SQLiteConfig();
            readConfig.setReadOnly(true);
            readConfig.enableLoadExtension(false);
            readConfig.setOpenMode(SQLiteOpenMode.FULLMUTEX);
            readConfig.setBusyTimeout(5000);
            readConfig.setPragma(SQLiteConfig.Pragma.FOREIGN_KEYS, "ON");
            readConfig.setPragma(SQLiteConfig.Pragma.JDBC_EXPLICIT_READONLY, "ON");
            while (readConnections.remainingCapacity() > 0) {
                var connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath(), readConfig.toProperties());
                establishedConnections++;
                allReadConnections.add(connection);
                readConnections.add(connection);
            }
        }

        var lock = new ReentrantReadWriteLock(true);
        r = lock.readLock();
        w = lock.writeLock();
    }

    @Override
    public <T> T executeWithException_(ConnectionFunctionWithException<T> connectionFunction, long wait, boolean readOnly) throws Exception {
        if (isClosing()) throw new BusyException("Database closing");
        if (readOnly) {
            long start = System.currentTimeMillis();
            if (!r.tryLock(wait, TimeUnit.MILLISECONDS)) {
                throw new BusyException("Database busy");
            }
            try {
                if (isClosing()) throw new BusyException("Database closing");
                Connection connection = readConnections.poll(Math.max(0, wait - (System.currentTimeMillis() - start)), TimeUnit.MILLISECONDS);
                if (connection == null) {
                    throw new BusyException("Database busy");
                }
                try {
                    return connectionFunction.apply(connection);
                } finally {
                    if (!readConnections.offer(connection)) {
                        try {
                            connection.close();
                        } catch (SQLException ignored) {
                        }
                    }
                }
            } finally {
                r.unlock();
            }
        } else {
            if (!w.tryLock(wait, TimeUnit.MILLISECONDS)) {
                throw new BusyException("Database busy");
            }
            try {
                if (isClosing()) throw new BusyException("Database closing");
                var ret = connectionFunction.apply(writeConnection);
                if (w.getHoldCount() == 1 && !writeConnection.getAutoCommit()) {
                    try {
                        writeConnection.setAutoCommit(true);
                    } catch (SQLException ignored) {
                    }
                    throw new IllegalStateException("Auto commit not re-enabled after transaction");
                }
                return ret;
            } finally {
                w.unlock();
            }
        }
    }

    @Override
    public int getActiveConnections() {
        if (isClosing()) return 0;
        return allReadConnections.size() + 1;
    }

    @Override
    public int getAllEstablishedConnections() {
        return establishedConnections;
    }

    @Override
    public void closeInternal() {
        try {
            w.lockInterruptibly();
        } catch (InterruptedException ignored) {
        }
        try {
            writeConnection.close();
        } catch (SQLException ignored) {
        }
        for (Connection readConnection : allReadConnections) {
            try {
                readConnection.close();
            } catch (SQLException ignored) {
            }
        }
        allReadConnections.clear();
        readConnections.clear();
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
