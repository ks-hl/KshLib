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
            buckets.values().forEach(bucket -> bucket.tokens += add);
            buckets.values().removeIf(bucket -> bucket.tokens >= getMaxRequests());
        }
        Bucket bucket = buckets.computeIfAbsent(sender, o -> new Bucket(getMaxRequests()));
        if (bucket.tokens < 1) return false;
        bucket.tokens--;
        return true;
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
        private double tokens;

        public Bucket(double tokens) {
            this.tokens = tokens;
        }
    }

    public RateLimiter copy() {
        return new RateLimiter(getMaxRequests(), getWindow());
    }
}
