package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class ConnectionPool {
    private boolean closing;

    protected ConnectionPool() throws ClassNotFoundException {
        checkDriver();
    }

    protected boolean testConnection(Connection connection) {
        try {
            return connection.isValid(2);
        } catch (SQLException ignored) {
            return false;
        }
    }

    public void consume(ConnectionConsumer connectionConsumer, long wait) throws SQLException, BusyException {
        function(connection -> {
            connectionConsumer.accept(connection);
            return null;
        }, wait);
    }

    public final <T> T function(ConnectionFunction<T> connectionFunction, long wait) throws SQLException, BusyException {
        try {
            return executeWithException(connectionFunction::apply, wait);
        } catch (SQLException | BusyException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract <T> T executeWithException(ConnectionFunctionWithException<T> task, long wait) throws Exception;

    public final boolean isMySQL() {
        return this instanceof ConnectionPoolMySQL;
    }

    @SuppressWarnings("unused")
    public abstract int getActiveConnections();

    @SuppressWarnings("unused")
    public abstract int getAllEstablishedConnections();

    public boolean isClosing() {
        return closing;
    }

    public final void close() {
        if (closing) return;
        closing = true;
        closeInternal();
    }

    protected abstract void closeInternal();

    protected abstract void checkDriver() throws ClassNotFoundException;
}
