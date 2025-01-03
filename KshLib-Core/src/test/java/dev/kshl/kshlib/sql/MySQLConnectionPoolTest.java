package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLConnectionPoolTest {
    @Test
    public void testMySQLPool() throws Exception {
        ConnectionManager connectionManager;
        try {
            connectionManager = new ConnectionManager("localhost:3306", "test", "test", "password", 10) {
                @Override
                protected void init(Connection connection) throws SQLException {
                }

                @Override
                protected void debug(String line) {
                }

                @Override
                protected boolean checkAsync() {
                    return true;
                }

                @Override
                protected boolean isDebug() {
                    return false;
                }
            };
        } catch (SQLException e) {
            System.err.print("Unable to connect to MySQL database, is it up?");
            e.printStackTrace();
            return;
        }
        connectionManager.init();

        ConnectionConsumer test = connection -> {
            // Check that the same thread accessing twice get the same Connection.
            connectionManager.execute(connection2 -> {
                Assertions.assertEquals(connection, connection2);
            }, 3000L);
            Thread thread = new Thread(() -> {
                // Check that 2 different threads accessing at the same time get 2 different Connections
                try {
                    connectionManager.execute(connection2 -> {
                        Assertions.assertNotEquals(connection, connection2);
                    }, 3000L);
                } catch (SQLException | BusyException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            thread.join();
        };

        connectionManager.execute(test, 3000L);
        connectionManager.executeTransaction(test, 3000L);

        Assertions.assertThrows(RuntimeException.class, () -> {
            connectionManager.executeTransaction(c -> {
                connectionManager.execute((ConnectionConsumer) c2 -> {
                    throw new IllegalArgumentException();
                }, 3000L);
            }, 3000L);
        });
    }
}
