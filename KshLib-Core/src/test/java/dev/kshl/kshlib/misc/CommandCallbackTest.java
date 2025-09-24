package dev.kshl.kshlib.misc;

import dev.kshl.kshlib.log.ILogger;
import dev.kshl.kshlib.log.StdOutLogger;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CommandCallbackTest {

    private static ILogger logger() {
        // Uses your concrete stdout logger as requested
        return new StdOutLogger();
    }

    private static <T> UUID register(
            CommandCallback<T> cb,
            T sender,
            Runnable onExecute,
            boolean oneTime,
            long ttl,
            TimeUnit unit,
            Runnable onExpiration
    ) {
        return cb.registerCallback(sender, onExecute, oneTime, ttl, unit, onExpiration);
    }

    // --- Registration / TTL validation ---

    @Test
    void registerCallback_rejectsInvalidTtl() {
        CommandCallback<String> cb = new CommandCallback<>(logger());

        assertThrows(IllegalArgumentException.class,
                () -> register(cb, "alice", () -> {
                }, true, 0, TimeUnit.MILLISECONDS, null));

        assertThrows(IllegalArgumentException.class,
                () -> register(cb, "alice", () -> {
                        }, true,
                        TimeUnit.HOURS.toMillis(1) + 1, TimeUnit.MILLISECONDS, null));
    }

    // --- Happy path ---

    @Test
    void handleCallback_executes_whenSenderMatches_andUuidValid() throws Exception {
        CommandCallback<String> cb = new CommandCallback<>(logger());
        CountDownLatch ran = new CountDownLatch(1);

        UUID id = register(cb, "alice", ran::countDown, false, 2, TimeUnit.SECONDS, null);

        cb.handleCallback("alice", id.toString());

        assertTrue(ran.await(300, TimeUnit.MILLISECONDS), "onExecute should have run");
    }

    // --- Error / exception paths as specified by new API ---

    @Test
    void handleCallback_invalidUuid_throwsCallbackException() {
        CommandCallback<String> cb = new CommandCallback<>(logger());
        // Just need an existing callback to ensure class is initialized; use a throwaway one
        register(cb, "alice", () -> {
        }, false, 2, TimeUnit.SECONDS, null);

        CommandCallback.CallbackException ex = assertThrows(
                CommandCallback.CallbackException.class,
                () -> cb.handleCallback("alice", "not-a-uuid")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("invalid uuid"), "message should mention invalid UUID");
    }

    @Test
    void handleCallback_notFound_throwsCallbackNotFound() {
        CommandCallback<String> cb = new CommandCallback<>(logger());

        String randomUuid = UUID.randomUUID().toString();
        assertThrows(CommandCallback.CallbackNotFoundException.class,
                () -> cb.handleCallback("alice", randomUuid));
    }

    @Test
    void handleCallback_wrongSender_throwsWrongSender() throws Exception {
        CommandCallback<String> cb = new CommandCallback<>(logger());
        UUID id = register(cb, "alice", () -> {
        }, false, 2, TimeUnit.SECONDS, null);

        assertThrows(CommandCallback.WrongSenderCallbackException.class,
                () -> cb.handleCallback("bob", id.toString()));
    }

    @Test
    void handleCallback_expired_throwsExpired() throws Exception {
        CommandCallback<String> cb = new CommandCallback<>(logger());
        UUID id = register(cb, "alice", () -> {
        }, false, 10, TimeUnit.MILLISECONDS, null);

        // Let TTL pass so isExpired() becomes true even if removal hasn't happened yet
        Thread.sleep(20);

        assertThrows(CommandCallback.CallbackNotFoundException.class,
                () -> cb.handleCallback("alice", id.toString()));
    }

    @Test
    void handleCallback_afterScheduledRemoval_throwsNotFound() throws Exception {
        CommandCallback<String> cb = new CommandCallback<>(logger());
        UUID id = register(cb, "alice", () -> {
        }, false, 10, TimeUnit.MILLISECONDS, null);

        // Wait long enough for the scheduled task to remove it from the map
        Thread.sleep(20);

        assertThrows(CommandCallback.CallbackNotFoundException.class, () -> cb.handleCallback("alice", id.toString()));
    }

    // --- One-time callbacks ---

    @Test
    void oneTime_executes_only_once_serialCalls_secondCallNotFound() throws Exception {
        CommandCallback<String> cb = new CommandCallback<>(logger());
        AtomicInteger executions = new AtomicInteger();

        UUID id = register(cb, "alice", executions::incrementAndGet, true, 2, TimeUnit.SECONDS, null);

        cb.handleCallback("alice", id.toString()); // first call: should execute
        assertEquals(1, executions.get());

        // Because the implementation removes the callback on first successful execution,
        // a follow-up call will see "not found" (not AlreadyUsed)
        assertThrows(CommandCallback.CallbackNotFoundException.class,
                () -> cb.handleCallback("alice", id.toString()));
        assertEquals(1, executions.get());
    }

    @Test
    void oneTime_concurrentCalls_executeExactlyOnce_otherCallerSeesError() throws Exception {
        CommandCallback<String> cb = new CommandCallback<>(logger());
        AtomicInteger executions = new AtomicInteger();

        UUID id = register(cb, "alice", executions::incrementAndGet, true, 2, TimeUnit.SECONDS, null);

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<Throwable> task = () -> {
            try {
                barrier.await();
                cb.handleCallback("alice", id.toString());
                return null; // success
            } catch (Throwable t) {
                return t;
            }
        };

        Future<Throwable> f1 = pool.submit(task);
        Future<Throwable> f2 = pool.submit(task);

        Throwable t1 = f1.get(1, TimeUnit.SECONDS);
        Throwable t2 = f2.get(1, TimeUnit.SECONDS);

        pool.shutdownNow();

        // Exactly one succeeded (returned null), one failed with either AlreadyUsed *or* NotFound
        int successes = (t1 == null ? 1 : 0) + (t2 == null ? 1 : 0);
        assertEquals(1, successes, "exactly one execution should succeed");
        assertEquals(1, executions.get(), "onExecute must run exactly once");

        Throwable failure = (t1 == null) ? t2 : t1;
        assertNotNull(failure, "one caller should fail");
        assertTrue(
                failure instanceof CommandCallback.CallbackOneTimeAlreadyUsedException
                        || failure instanceof CommandCallback.CallbackNotFoundException,
                "second caller should see AlreadyUsed or NotFound depending on interleaving"
        );
    }

    // --- Reusable callbacks ---

    @Test
    void reusable_executesMultipleTimes_beforeExpiration_thenExpired() throws Exception {
        CommandCallback<String> cb = new CommandCallback<>(logger());
        AtomicInteger executions = new AtomicInteger();

        UUID id = register(cb, "alice", executions::incrementAndGet, false, 10, TimeUnit.MILLISECONDS, null);

        cb.handleCallback("alice", id.toString());
        cb.handleCallback("alice", id.toString());
        assertEquals(2, executions.get(), "should run multiple times before expiration");

        // After TTL elapses, invoking should throw expired (or not found if removed)
        Thread.sleep(20);
        assertThrows(CommandCallback.CallbackNotFoundException.class, () -> cb.handleCallback("alice", id.toString()));
        assertEquals(2, executions.get());
    }

    // --- Safety around exceptions inside onExecute ---

    @Test
    void onExecute_exception_is_caught_and_not_propagated() throws Exception {
        CommandCallback<String> cb = new CommandCallback<>(logger());
        RuntimeException boom = new RuntimeException("boom");

        UUID id = register(cb, "alice", () -> {
            throw boom;
        }, false, 2, TimeUnit.SECONDS, null);

        // No exception should escape handleCallback (safeRun catches & logs)
        assertDoesNotThrow(() -> cb.handleCallback("alice", id.toString()));
    }

    // --- small helper to allow multiple acceptable exception types (race-friendly assertions) ---

    private static void assertThrowsAnyOf(Class<?>[] expected, Executable e) {
        try {
            e.execute();
            fail("Expected one of " + java.util.Arrays.toString(expected) + " to be thrown");
        } catch (Throwable t) {
            for (Class<?> clazz : expected) {
                if (clazz.isInstance(t)) {
                    return; // ok
                }
            }
            // If we got here, it was an unexpected type
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            if (t instanceof Error) throw (Error) t;
            throw new AssertionError("Unexpected exception type: " + t.getClass().getName(), t);
        }
    }

    @FunctionalInterface
    private interface Executable {
        void execute() throws Exception;
    }
}
