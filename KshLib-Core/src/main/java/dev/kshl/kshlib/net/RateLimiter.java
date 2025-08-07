package dev.kshl.kshlib.net;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    public static final String GLOBAL_SENDER = "__global__";
    private final Bucket globalBucket;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long window;

    private long lastPurge = System.currentTimeMillis();

    public RateLimiter(int maxRequests, long window) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.globalBucket = new Bucket();
    }

    /**
     * Test if the request is within the rate limits, and deprecate the token allocation by one
     *
     * @return Whether the request is within the rate limits
     */
    public boolean check(String sender) {
        Objects.requireNonNull(sender, "sender must be not null");
        if (System.currentTimeMillis() - lastPurge > 1000) {
            lastPurge = System.currentTimeMillis();
            buckets.values().removeIf(Bucket::isStale);
        }
        return getBucket(sender).check();
    }

    /**
     * Applies the rate limit to all senders equally.
     *
     * @return Whether the request is within the rate limits
     */
    public boolean checkGlobal() {
        return check(GLOBAL_SENDER);
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public long getWindow() {
        return window;
    }

    class Bucket {
        private long milliTokens;
        private long lastRefill;
        private long lastUse = System.currentTimeMillis();

        public Bucket() {
            Bucket.this.reset();
        }

        void reset() {
            this.milliTokens = getMaxRequests() * 1000L;
            this.lastRefill = System.currentTimeMillis();
        }

        private void refill() {
            long time = System.currentTimeMillis();
            long timeSinceRefill = time - lastRefill;
            lastRefill = time;
            if (timeSinceRefill > 0) {
                double add = getMaxRequests() / (double) getWindow() * timeSinceRefill;
                this.milliTokens += Math.round(add * 1000);

                long milliTokenMax = getMaxRequests() * 1000L;
                if (this.milliTokens > milliTokenMax) {
                    this.milliTokens = milliTokenMax;
                }
            }
        }

        boolean check() {
            synchronized (this) {
                this.lastUse = System.currentTimeMillis();
                refill();
                if (this.milliTokens < 1000) return false;
                this.milliTokens -= 1000;
                return true;
            }
        }

        boolean isStale() {
            return System.currentTimeMillis() - lastUse > getWindow();
        }

        public long getTokens() {
            return this.milliTokens / 1000;
        }
    }

    Bucket getBucket(String sender) {
        Objects.requireNonNull(sender, "sender must be not null");
        if (isGlobal(sender)) {
            return this.globalBucket;
        }
        return buckets.computeIfAbsent(sender, s -> new Bucket());
    }

    /**
     * Clears all buckets
     */
    public void reset() {
        this.buckets.clear();
        this.globalBucket.reset();
    }

    /**
     * Clears bucket for the specified user
     */
    public void reset(String sender) {
        Objects.requireNonNull(sender, "sender must be not null");
        if (isGlobal(sender)) {
            this.globalBucket.reset();
        } else {
            this.buckets.remove(sender);
        }
    }

    private boolean isGlobal(String sender) {
        return GLOBAL_SENDER.equals(sender);
    }

    public RateLimiter copy() {
        return new RateLimiter(getMaxRequests(), getWindow());
    }
}
