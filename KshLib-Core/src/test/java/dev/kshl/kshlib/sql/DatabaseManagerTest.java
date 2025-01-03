package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ThrowingConsumer;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseManagerTest {

    @DatabaseTest
    public void testMultipleQueries(ConnectionManager connectionManager) throws InterruptedException, ExecutionException {
        final int numThreads = 32;
        final int queriesPerThread = 1000;
        List<Thread> threads = new ArrayList<>();
        List<CompletableFuture<Void>> completables = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            CompletableFuture<Void> completable = new CompletableFuture<>();
            threads.add(new Thread(() -> {
                try {
                    for (int i1 = 0; i1 < queriesPerThread; i1++) {
                        connectionManager.execute("SELECT 1", 3000);
                    }
                } catch (Throwable t) {
                    completable.completeExceptionally(t);
                }
                completable.complete(null);
            }));
        }
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
        for (CompletableFuture<Void> completable : completables) {
            completable.get();
        }
        System.out.println("Executed " + (queriesPerThread * numThreads) + " queries");
    }

    private static Stream<ConnectionManager> provideConnectionManagers() throws IOException, SQLException, ClassNotFoundException {
        List<ConnectionManager> connectionManagerList = new ArrayList<>();

        connectionManagerList.add(new TestConnectionManager(new File("test.db")));  // SQLite)
        try {
            connectionManagerList.add(new TestConnectionManager("localhost:3306", "test", "test", "password", 8));  // MySQL)
        } catch (SQLException e) {
            if (e.getErrorCode() != 0) throw e;
            System.err.println("MySQL db not found");
        }

        return connectionManagerList.stream();
    }

    @DatabaseTest
    public void testCommonDatabaseOperations(ConnectionManager connectionManager) throws SQLException, BusyException, ExecutionException, InterruptedException, NoSuchAlgorithmException {
        int uid = (int) (System.currentTimeMillis() % 10000000);
        long time = System.currentTimeMillis();
        connectionManager.execute("INSERT INTO test_table (uid,time) VALUES (?,?)", 10000L, uid, time);

        assertTrue(connectionManager.execute("SELECT * FROM test_table", (PreparedStatementFunction<Boolean>) preparedStatement -> {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) return rs.getLong("time") == time && rs.getInt("uid") == uid;
            }
            return false;
        }, 30000L));

        assertTrue(connectionManager.execute((ConnectionFunction<Boolean>) connection -> connectionManager.tableExists(connection, "test_table"), 3000L));
        assertFalse(connectionManager.execute((ConnectionFunction<Boolean>) connection -> connectionManager.tableExists(connection, "test_tablefuih34iuhafiuh"), 3000L));


        connectionManager.execute(connection -> {
            connectionManager.execute(connection, "DROP TABLE IF EXISTS test_transactions");
            connectionManager.execute(connection, "CREATE TABLE test_transactions (number INT)");

            ThrowingConsumer<Integer, SQLException> insert = i -> connectionManager.execute(connection, "INSERT INTO test_transactions (number) VALUES (?)", i);
            ThrowingConsumer<Integer, SQLException> assertSize = size -> assertEquals(size, connectionManager.count(connection, "test_transactions"));

            insert.accept(1);
            assertSize.accept(1);

            assertThrows(TransactionTestException.class, () -> connectionManager.executeTransaction(connection, () -> {
                insert.accept(2);
                assertSize.accept(2);
                throw new TransactionTestException(); // Needs to error to test that transaction reverts
            }));

            assertSize.accept(1);

            connectionManager.executeTransaction(connection, () -> {
                insert.accept(3);
                assertSize.accept(2);
            });

            assertSize.accept(2);
        }, 3000);
    }

    private static final class TransactionTestException extends RuntimeException {
    }


}
