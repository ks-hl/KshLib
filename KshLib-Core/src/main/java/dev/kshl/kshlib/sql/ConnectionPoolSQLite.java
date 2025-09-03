package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.concurrent.ConcurrentReference;
import dev.kshl.kshlib.exceptions.BusyException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionPoolSQLite extends ConnectionPool {
    private final ConcurrentReference<Connection> connection;

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

        this.connection = new ConcurrentReference<>(DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath()));
    }

    @Override
    protected <T> T executeWithException_(ConnectionFunctionWithException<T> connectionFunction, long wait) throws Exception {
        if (isClosing()) {
            throw new BusyException("Database closing");
        }
        try {
            return connection.functionThrowing(connection -> {
                var ret = connectionFunction.apply(connection);
                if (this.connection.getHoldCount() == 1 && !connection.getAutoCommit()) {
                    throw new IllegalStateException("Auto commit not re-enabled after transaction");
                }
                return ret;
            }, wait);
        } catch (SQLException | BusyException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        connection.consumeForce(connection1 -> {
            try {
                connection1.close();
            } catch (SQLException ignored) {
            }
        });
    }

    @Override
    protected void checkDriver() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }
}
