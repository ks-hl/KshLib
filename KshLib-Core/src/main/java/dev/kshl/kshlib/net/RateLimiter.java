package dev.kshl.kshlib.net;

import java.util.HashMap;
import java.util.Map;

public class RateLimiter {
    private final Map<String, Bucket> buckets = new HashMap<>();
    private long lastRefill;
    private final int maxRequests;
    private final long window;

    public RateLimiter(int maxRequests, long window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    /**
     * Test if the request is within the rate limits, and deprecate the token allocation by one
     *
     * @return Whether the request is within the rate limits
     */
    public synchronized boolean check(String sender) {
        long timeSinceRefill = System.currentTimeMillis() - lastRefill;
        if (timeSinceRefill >= 10) {
            lastRefill = System.currentTimeMillis();
            double add = getMaxRequests() / (double) getWindow() * timeSinceRefill;
            buckets.values().forEach(bucket -> bucket.addTokens(add));
            buckets.values().removeIf(bucket -> bucket.getTokens() >= getMaxRequests());
        }
        return buckets.computeIfAbsent(sender, o -> new Bucket(getMaxRequests())).check();
    }

    /**
     * Applies the rate limit to all senders equally.
     *
     * @return Whether the request is within the rate limits
     */
    public boolean checkGlobal() {
        return check(null);
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public long getWindow() {
        return window;
    }

    private static class Bucket {
        private long milliTokens;

        public Bucket(long tokens) {
            this.milliTokens = tokens * 1000L;
        }

        void addTokens(double add) {
            this.milliTokens += Math.round(add * 1000);
        }

        boolean check() {
            if (this.milliTokens < 1000) return false;
            this.milliTokens -= 1000;
            return true;
        }

        long getTokens() {
            return milliTokens / 1000;
        }
    }

    public RateLimiter copy() {
        return new RateLimiter(getMaxRequests(), getWindow());
    }
}
