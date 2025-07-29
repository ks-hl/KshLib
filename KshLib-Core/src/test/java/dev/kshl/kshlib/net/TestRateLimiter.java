package dev.kshl.kshlib.net;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRateLimiter {
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(5,50);
    }

    @Test
    void allowsRequestsWithinLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.check("user1"), "Request " + i + " should be allowed");
        }
    }

    @Test
    void blocksRequestsOverLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.check("user2"), "Request " + i + " should be allowed");
        }
        assertFalse(rateLimiter.check("user2"), "6th request should be blocked");
    }

    @Test
    void allowsGlobalRequestsWithinLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.checkGlobal(), "Global request " + i + " should be allowed");
        }
    }

    @Test
    void blocksGlobalRequestsOverLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.checkGlobal(), "Global request " + i + " should be allowed");
        }
        assertFalse(rateLimiter.checkGlobal(), "6th global request should be blocked");
    }

    @Test
    void refillsTokensOverTime() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.check("user3"), "Request " + i + " should be allowed");
        }
        assertFalse(rateLimiter.check("user3"), "6th request should be blocked");

        // Wait enough for token refill
        Thread.sleep(60);

        assertTrue(rateLimiter.check("user3"), "Request after refill should be allowed");
    }

    @Test
    void multipleUsersAreIsolated() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.check("userA"), "userA request " + i + " should be allowed");
            assertTrue(rateLimiter.check("userB"), "userB request " + i + " should be allowed");
        }
    }

    @Test
    void refillDoesNotExceedMaxTokens() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            rateLimiter.check("user4"); // deplete
        }

        Thread.sleep(100); // 2 refill times

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.check("user4"), "Request " + i + " should be allowed after refill");
        }

        assertFalse(rateLimiter.check("user4"), "Extra request should still be blocked");
    }

    @Test
    void doesNotRefillImmediately() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.check("user5"));
        }

        // Very short wait — should not trigger refill logic
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) {
        }

        assertFalse(rateLimiter.check("user5"), "Should still be blocked (no refill yet)");
    }
}
