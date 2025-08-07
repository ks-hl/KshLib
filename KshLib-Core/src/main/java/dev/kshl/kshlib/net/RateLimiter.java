package dev.kshl.kshlib.net;

import java.util.HashMap;
import java.util.Map;

public class RateLimiter {
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final int maxRequests;
    private final long window;

    private long lastRefill = System.currentTimeMillis();

    public RateLimiter(int maxRequests, long window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    /**
     * Test if the request is within the rate limits, and deprecate the token allocation by one
     *
     * @return Whether the request is within the rate limits
     */
    public boolean check(String sender) {
        synchronized (buckets) {
            refillBuckets();
            return getBucket(sender).check();
        }
    }

    private void refillBuckets() {
        synchronized (buckets) {
            long time = System.currentTimeMillis();
            long timeSinceRefill = time - lastRefill;
            if (timeSinceRefill >= 10) {
                lastRefill = time;
                double add = getMaxRequests() / (double) getWindow() * timeSinceRefill;
                buckets.values().removeIf(bucket -> bucket.addTokens(add) >= getMaxRequests());
            }
        }
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

    static class Bucket {
        private long milliTokens;

        public Bucket(long tokens) {
            this.milliTokens = tokens * 1000L;
        }

        long addTokens(double add) {
            this.milliTokens += Math.round(add * 1000);
            return this.milliTokens / 1000L;
        }

        boolean check() {
            if (this.milliTokens < 1000) return false;
            this.milliTokens -= 1000;
            return true;
        }
    }

    Bucket getBucket(String sender) {
        synchronized (buckets) {
            return buckets.computeIfAbsent(sender, o -> new Bucket(getMaxRequests()));
        }
    }

    /**
     * Clears all buckets
     */
    public void reset() {
        synchronized (buckets) {
            this.buckets.clear();
            this.lastRefill = System.currentTimeMillis();
        }
    }

    /**
     * Clears bucket for the specified user
     */
    public void reset(String sender) {
        synchronized (buckets) {
            this.buckets.remove(sender);
        }
    }

    public RateLimiter copy() {
        return new RateLimiter(getMaxRequests(), getWindow());
    }
}
