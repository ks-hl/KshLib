package dev.kshl.kshlib.sql;

import dev.kshl.kshlib.exceptions.BusyException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLiteConnectionPoolTest {
    @TempDir
    Path tempPath;
    private ConnectionManager connectionManager;
    private ExecutorService executor;

    @BeforeEach
    public void init() throws SQLException, IOException, ClassNotFoundException {
        connectionManager = new TestConnectionManager(tempPath.resolve(UUID.randomUUID() + ".db").toFile());
        executor = Executors.newScheduledThreadPool(10);
    }

    @AfterEach
    public void after() {
        connectionManager.close();
        executor.shutdown();
    }

    @Test
    public void testReadOnly() throws SQLException, BusyException {
        String create = "CREATE TABLE IF NOT EXISTS tbl (i INT)";
        assertThrowsReadOnly(() -> connectionManager.execute(create).readOnly().executeQuery(1000));

        connectionManager.execute(create).executeQuery(1000);

        String insert = "INSERT INTO tbl (i) VALUES (1)";
        assertThrowsReadOnly(() -> connectionManager.execute(insert).readOnly().executeQuery(1000));

        connectionManager.execute(insert, 3000L);
    }

    @Test
    void read_pool_exhaustion_yields_busy() throws Exception {
        int connectionCount = connectionManager.getActiveConnections() - 1;

        CountDownLatch latch = new CountDownLatch(connectionCount);
        CompletableFuture<Void> testDone = new CompletableFuture<>();

        for (int i = 0; i < connectionCount; i++) {
            executor.submit(() -> {
                connectionManager.accept(connection -> {
                    latch.countDown();
                    try {
                        testDone.get(3, TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                    }
                }).readOnly().executeQuery(10);
                return null;
            });
        }

        latch.await();
        assertEquals(0, latch.getCount());

        assertThrows(BusyException.class, () -> connectionManager.execute("SELECT 1").readOnly().executeQuery(1));
        assertThrows(BusyException.class, () -> connectionManager.execute("SELECT 1").executeQuery(1));

        testDone.complete(null);
    }

    @Test
    @Timeout(1)
    public void testWriteWaitsForReads() throws Exception {
        final int count = 3;
        final Semaphore semaphore = new Semaphore(count);
        CountDownLatch latch = new CountDownLatch(count);
        CompletableFuture<Void> testDone = new CompletableFuture<>();

        CompletableFuture<Void> excFuture = new CompletableFuture<>();
        for (int i = 0; i < count; i++) {
            semaphore.acquire();
            executor.submit(() -> {
                try {
                    connectionManager.accept(connection -> {
                        latch.countDown();
                        try {
                            testDone.get(3, TimeUnit.SECONDS);
                        } catch (Exception ignored) {
                        }
                    }).readOnly().executeQuery(1);
                    semaphore.release();
                } catch (Exception e) {
                    excFuture.completeExceptionally(e);
                }
            });
        }

        latch.await();
        assertEquals(0, latch.getCount());
        assertEquals(0, semaphore.availablePermits());

        executor.submit(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            testDone.complete(null);
        });

        AtomicBoolean success = new AtomicBoolean();
        connectionManager.accept(connection -> success.set(semaphore.availablePermits() == count)).executeQuery(100);

        if (excFuture.isCompletedExceptionally()) {
            try {
                excFuture.get();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Exception ex) {
                    throw ex;
                }
                throw e;
            }
        }

        assertTrue(success.get());
    }

    private static void assertThrowsReadOnly(Executable executable) {
        var e = assertThrows(SQLiteException.class, executable);
        assertEquals(SQLiteErrorCode.SQLITE_READONLY.code, e.getErrorCode());
    }
}
