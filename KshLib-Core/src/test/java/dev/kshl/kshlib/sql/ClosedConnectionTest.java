package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;

import java.sql.Connection;
import java.sql.SQLException;

public class ClosedConnectionTest {
    @DatabaseTest
    public void test(ConnectionManager connectionManager) throws SQLException, BusyException, InterruptedException {
        if (!connectionManager.isMySQL()) return;

        // Ensures the MySQL connection pool can recover from closed connections
        for (int i = 0; i < 100; i++) {
            try {
                connectionManager.execute(Connection::close, 3000L);
            } catch (Throwable t) {
                System.err.println("Failed on attempt #" + (i + 1));
                throw t;
            }
        }
    }
}
