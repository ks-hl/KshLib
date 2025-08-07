package dev.kshl.kshlib.net;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRateLimiter {
    private RateLimiter rateLimiter;
    private static final String USER_A = "userA";
    private static final String USER_B = "userB";

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(5, 50);
    }

    @Test
    void allowsRequestsWithinLimit() {
        assertAllowedNTimes(USER_A, 5);
    }

    @Test
    void blocksRequestsOverLimit() {
        assertAllowedNTimes(USER_A, 5);
        assertFalse(rateLimiter.check(USER_A), "6th request should be blocked");
    }

    @Test
    void allowsGlobalRequestsWithinLimit() {
        assertAllowedNTimes(RateLimiter.GLOBAL_SENDER, 5);
    }

    @Test
    void blocksGlobalRequestsOverLimit() {
        assertAllowedNTimes(RateLimiter.GLOBAL_SENDER, 5);
        assertFalse(rateLimiter.checkGlobal(), "6th global request should be blocked");
    }

    @Test
    void refillsTokensFully() throws InterruptedException {
        assertAllowedNTimes(USER_A, 5);
        assertFalse(rateLimiter.check(USER_A), "6th request should be blocked");

        // Wait enough for token refill
        Thread.sleep(60);

        assertAllowedNTimes(USER_A, 5);
        assertFalse(rateLimiter.check(USER_A), "6th request should be blocked");
    }

    @Test
    void refillsTokensGradually() throws InterruptedException {
        assertAllowedNTimes(USER_A, 1);

        Thread.sleep(5);

        assertAllowedNTimes(USER_A, 4);
        assertFalse(rateLimiter.check(USER_A), "6th request should be blocked");

        Thread.sleep(5); // Allow enough time for a single refill (5 + 5)

        assertAllowedNTimes(USER_A, 1);
        assertFalse(rateLimiter.check(USER_A), "Request should be blocked");
    }

    @Test
    void multipleUsersAreIsolated() {
        assertAllowedNTimes(USER_A, 5);
        assertAllowedNTimes(USER_B, 5);
    }

    @Test
    void refillDoesNotExceedMaxTokens() throws InterruptedException {
        assertAllowedNTimes(USER_A, 5);

        Thread.sleep(100); // 2 refill times

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.check(USER_A), "Request " + i + " should be allowed after refill");
        }

        assertFalse(rateLimiter.check(USER_A), "Extra request should still be blocked");
    }

    @Test
    void doesNotRefillImmediately() throws InterruptedException {
        assertAllowedNTimes(USER_A, 5);

        // Very short wait — should not trigger refill logic
        Thread.sleep(5);

        assertFalse(rateLimiter.check(USER_A), "Should still be blocked (no refill yet)");
    }

    @Test
    void refillIsThreadSafe() throws ExecutionException, InterruptedException {
        doRefillIsThreadSafe();
        Thread.sleep(60);
        doRefillIsThreadSafe();
    }

    private void doRefillIsThreadSafe() throws ExecutionException, InterruptedException {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String user = "user" + i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                assertAllowedNTimes(user, 5);
                assertFalse(rateLimiter.check(user), user + "'s 6th attempt should be blocked.");
                return null;
            }));
        }
        for (CompletableFuture<Void> future : futures) {
            future.get();
        }
    }

    @Test
    void resetAll() {
        assertAllowedNTimes(USER_A, 5);
        rateLimiter.reset();
        assertAllowedNTimes(USER_A, 5);
    }

    @Test
    void resetSingleUser() {
        assertAllowedNTimes(USER_A, 5);
        assertAllowedNTimes(USER_B, 5);
        rateLimiter.reset(USER_A);
        assertTrue(rateLimiter.check(USER_A));
        assertFalse(rateLimiter.check(USER_B));
    }

    @Test
    void nullSender() {
        assertThrows(NullPointerException.class, () -> rateLimiter.check(null));
    }

    private void assertAllowedNTimes(String sender, int n) {
        for (int i = 0; i < n; i++) {
            assertTrue(rateLimiter.check(sender), sender + "'s request " + i + " should be allowed");
        }
    }
}
