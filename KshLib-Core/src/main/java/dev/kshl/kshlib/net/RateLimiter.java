package dev.kshl.kshlib.net;

import java.util.HashMap;
import java.util.Map;

public class RateLimiter {
    private final Map<String, Bucket> buckets = new HashMap<>();
    private long lastRefill;
    private final Params params;

    public RateLimiter(Params params) {
        this.params = params;
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
            double add = params.maxRequests() / (double) params.withinMillis() * timeSinceRefill;
            buckets.values().forEach(bucket -> bucket.tokens += add);
            buckets.values().removeIf(bucket -> bucket.tokens >= params.maxRequests());
        }
        Bucket bucket = buckets.computeIfAbsent(sender, o -> new Bucket(params.maxRequests()));
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

    public Params getParams() {
        return params;
    }

    public record Params(int maxRequests, long withinMillis) {
    }

    private static class Bucket {
        private double tokens;

        public Bucket(double tokens) {
            this.tokens = tokens;
        }
    }
}
