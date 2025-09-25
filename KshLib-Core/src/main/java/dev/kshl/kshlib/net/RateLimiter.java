package dev.kshl.kshlib.net;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

public class RateLimiter {
    public static final String GLOBAL_SENDER = "__global__";
    private final Bucket globalBucket;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    @Getter
    private final int maxRequests;
    @Getter
    private final long window;

    private final PurgeTimer purgeTimer = new PurgeTimer();

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
        if (purgeTimer.shouldPurge()) {
            buckets.values().removeIf(Bucket::isStale);
        }
        return getBucket(sender).check();
    }

    private static class PurgeTimer {
        private long lastPurge = System.currentTimeMillis();

        synchronized boolean shouldPurge() {
            long time = System.currentTimeMillis();
            if (time - lastPurge < 1000) return false;

            lastPurge = time;
            return true;
        }
    }

    /**
     * Applies the rate limit to all senders equally.
     *
     * @return Whether the request is within the rate limits
     */
    public boolean checkGlobal() {
        return check(GLOBAL_SENDER);
    }

    class Bucket {
        private long milliTokens;
        private long lastRefill;
        private long lastUse = System.currentTimeMillis();

        Bucket() {
            reset();
        }

        synchronized void reset() {
            this.milliTokens = getMaxRequests() * 1000L;
            this.lastRefill = System.currentTimeMillis();
            this.lastUse = this.lastRefill;
        }

        private synchronized void refill() {
            long time = System.currentTimeMillis();
            long timeSinceRefill = time - lastRefill;
            lastRefill = time;
            if (timeSinceRefill > 0) {
                double add = getMaxRequests() / (double) getWindow() * timeSinceRefill;
                this.milliTokens += Math.round(add * 1000);
                long milliTokenMax = getMaxRequests() * 1000L;
                if (this.milliTokens > milliTokenMax) this.milliTokens = milliTokenMax;
            }
        }

        synchronized boolean check() {
            this.lastUse = System.currentTimeMillis();
            refill();
            if (this.milliTokens < 1000) return false;
            this.milliTokens -= 1000;
            return true;
        }

        synchronized boolean isStale() {
            return System.currentTimeMillis() - lastUse > getWindow();
        }

        synchronized long getTokens() {
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
