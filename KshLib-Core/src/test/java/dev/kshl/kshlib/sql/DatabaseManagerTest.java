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
        final int numThreads = 16;
        final int queriesPerThread = 250;
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

        File sqliteFile = new File("/tmp/kshlib/test.db");
        //noinspection ResultOfMethodCallIgnored
        sqliteFile.delete();
        connectionManagerList.add(new TestConnectionManager(sqliteFile));  // SQLite)
        try {
            connectionManagerList.add(new TestConnectionManager("localhost:3306", "test", "test", "password", 8));  // MySQL)
        } catch (SQLException e) {
            System.err.println("MySQL db not found or error occurred");
            e.printStackTrace();
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


    @DatabaseTest
    public void testBatchInsert(ConnectionManager connectionManager) throws SQLException, BusyException {
        String table = "batch_test";
        connectionManager.execute("DROP TABLE IF EXISTS " + table, 3000L);
        connectionManager.execute("CREATE TABLE " + table + " (a INT, b INT)", 3000L);

        int[][] expected = {{1, 2}, {3, 4}};
        connectionManager.executeBatch("INSERT INTO " + table + " (a,b) VALUES (?,?)",
                List.of(expected),
                i -> List.of(i[0], i[1]),
                3000L
        );
        connectionManager.query("SELECT * FROM " + table, rs -> {
            int count = 0;
            while (rs.next()) {
                assertEquals(expected[count][0], rs.getInt(1));
                assertEquals(expected[count][1], rs.getInt(2));
                count++;
            }
            assertEquals(2, count);
        }, 3000L);

        // Batch switch the values
        connectionManager.executeBatch("UPDATE " + table + " SET a=?, b=? WHERE a=? AND b=?",
                List.of(new int[]{1, 2}, new int[]{3, 4}),
                i -> List.of(i[1], i[0], i[0], i[1]),
                3000L
        );
        connectionManager.query("SELECT * FROM " + table, rs -> {
            int count = 0;
            while (rs.next()) {
                assertEquals(expected[count][1], rs.getInt(1));
                assertEquals(expected[count][0], rs.getInt(2));
                count++;
            }
            assertEquals(2, count);
        }, 3000L);
    }

    @DatabaseTest
    public void testTableColumnExists(ConnectionManager sql) throws SQLException, BusyException {
        String table = "table_exists";
        sql.execute("DROP TABLE IF EXISTS " + table, 3000L);
        assertFalse(sql.tableExists(table));
        assertFalse(sql.columnExists(table, "col"));
        sql.execute("CREATE TABLE " + table + " (col INT)", 3000L);
        assertTrue(sql.columnExists(table, "col"));
        assertFalse(sql.columnExists(table, "col2"));
    }

    @DatabaseTest
    public void testPrimaryKeyExists(ConnectionManager sql) throws SQLException, BusyException {
        String table = "pk_test";
        sql.execute("DROP TABLE IF EXISTS " + table, 3000L);

        // Create table without primary key
        sql.execute("CREATE TABLE " + table + " (col INT)", 3000L);
        assertFalse(sql.primaryKeyExists(table));

        // Drop and recreate with primary key
        sql.execute("DROP TABLE " + table, 3000L);
        sql.execute("CREATE TABLE " + table + " (id INT PRIMARY KEY, col INT)", 3000L);
        assertTrue(sql.primaryKeyExists(table));
    }

    @DatabaseTest
    public void testIndexExists(ConnectionManager sql) throws SQLException, BusyException {
        String table = "idx_test";
        String index = "idx_col";
        sql.execute("DROP TABLE IF EXISTS " + table, 3000L);

        // Create table without index
        sql.execute("CREATE TABLE " + table + " (col INT)", 3000L);
        assertFalse(sql.indexExists(table, index));

        // Create index explicitly
        sql.execute("CREATE INDEX " + index + " ON " + table + " (col)", 3000L);
        assertTrue(sql.indexExists(table, index));

        // Check for a non-existent index
        assertFalse(sql.indexExists(table, "nonexistent_index"));
    }

    @DatabaseTest
    public void testUniqueConstraintExists(ConnectionManager sql) throws SQLException, BusyException {
        String table = "unique_test";
        sql.execute("DROP TABLE IF EXISTS " + table, 3000L);

        // Create table without unique constraint
        sql.execute("CREATE TABLE " + table + " (id INTEGER PRIMARY KEY, value TEXT)", 3000L);
        assertFalse(sql.uniqueConstraintExists(table, "value"));

        // Drop and recreate with a UNIQUE constraint on 'value'
        sql.execute("DROP TABLE " + table, 3000L);
        sql.execute("CREATE TABLE " + table + " (id INTEGER PRIMARY KEY, value TEXT UNIQUE)", 3000L);
        assertTrue(sql.uniqueConstraintExists(table, "value"));

        // Add another column without unique constraint
        assertFalse(sql.uniqueConstraintExists(table, "other_col"));

        // Drop and recreate with a UNIQUE constraint on both
        sql.execute("DROP TABLE " + table, 3000L);
        sql.execute("CREATE TABLE " + table + " (id INTEGER PRIMARY KEY, value TEXT, UNIQUE(id,value))", 3000L);
        assertFalse(sql.uniqueConstraintExists(table, "value"));
        assertTrue(sql.uniqueConstraintExists(table, "id", "value"));
        assertTrue(sql.uniqueConstraintExists(table, "value", "id"));
    }

    @DatabaseTest
    public void testParameterCounts(ConnectionManager sql) throws SQLException, BusyException {
        String table = "parameter_test";
        sql.execute("DROP TABLE IF EXISTS " + table, 3000L);
        sql.execute("CREATE TABLE " + table + " (a INT)", 3000L);
        assertThrows(IllegalArgumentException.class, () -> sql.execute("SELECT 1 FROM " + table + " WHERE a=?", 3000L));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> sql.execute("SELECT 1 FROM " + table, 3000L, 1));
    }

}
