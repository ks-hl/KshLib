package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MapCacheTest {
    MapCache<String, Integer> cache;
    long ttl;

    @BeforeEach
    void setUp() {
        ttl = 80; // ms
        cache = new MapCache<>(ttl, TimeUnit.MILLISECONDS);
    }

    @AfterEach
    void tearDown() {
        cache.shutdown();
    }

    private static void awaitTrue(Duration timeout, Runnable tick, java.util.function.BooleanSupplier cond) {
        long end = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < end) {
            if (cond.getAsBoolean()) return;
            sleep(5);
            if (tick != null) tick.run();
        }
        fail("Condition not met within " + timeout.toMillis() + "ms");
    }

    @Test
    void putGetContainsSizeViews() {
        assertTrue(cache.isEmpty());
        assertNull(cache.put("a", 1));
        assertNull(cache.put("b", 2));
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsValue(2));
        assertEquals(1, cache.get("a"));

        // views are copies / unmodifiable
        var ks = cache.keySet();
        var vs = cache.values();
        var es = cache.entrySet();
        assertThrows(UnsupportedOperationException.class, () -> ks.add("x"));
        assertThrows(UnsupportedOperationException.class, () -> vs.add(99));
        assertThrows(UnsupportedOperationException.class, () -> es.clear());
    }

    @Test
    void computeIfAbsent_onlyOnce_and_nullIgnored() {
        AtomicInteger calls = new AtomicInteger();
        Integer v1 = cache.computeIfAbsent("x", k -> {
            calls.incrementAndGet();
            return 42;
        });
        Integer v2 = cache.computeIfAbsent("x", k -> {
            calls.incrementAndGet();
            return 24;
        });
        assertEquals(42, v1);
        assertEquals(42, v2);
        assertEquals(1, calls.get());

        Integer vNull = cache.computeIfAbsent("y", k -> null);
        assertNull(vNull);
        assertFalse(cache.containsKey("y"));
    }

    @Test
    void putAll_and_clear() {
        Map<String, Integer> m = Map.of("a", 1, "b", 2, "c", 3);
        cache.putAll(m);
        assertEquals(3, cache.size());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertFalse(cache.containsValue(1));
    }

    @Test
    void remove_basic() {
        cache.put("a", 1);
        cache.put("b", 2);
        assertEquals(1, cache.remove("a"));
        assertFalse(cache.containsKey("a"));
        assertEquals(1, cache.size());
    }

    @Test
    void ttl_expires_unless_touched_by_get() {
        cache.put("a", 1);

        // touch mid-way; should extend life
        sleep(ttl / 2);
        assertEquals(1, cache.get("a"));

        // after another ~half ttl, still present
        sleep(ttl / 2);
        assertTrue(cache.containsKey("a"));

        // after another ~half ttl, still present
        sleep(ttl / 2);
        assertTrue(cache.containsKey("a"));

        // wait > ttl again; should eventually be gone by cleaner
        awaitTrue(Duration.ofMillis(500), null, () -> !cache.containsKey("a", false));
    }

    @Test
    void ttl_expires_unless_touched_by_computeIfAbsent() {
        cache.put("a", 1);

        // touch mid-way; should extend life
        sleep(ttl / 2);
        assertEquals(1, cache.computeIfAbsent("a", u -> 0));

        // after another ~half ttl, still present
        sleep(ttl / 2);
        assertEquals(1, cache.computeIfAbsent("a", u -> 0));

        // after another ~half ttl, still present
        sleep(ttl / 2);
        assertEquals(1, cache.computeIfAbsent("a", u -> 0));

        // wait > ttl again; should eventually be gone by cleaner
        awaitTrue(Duration.ofMillis(500), null, () -> !cache.containsKey("a", false));
    }

    @Test
    void put_then_no_touch_expires() {
        cache.put("z", 9);
        awaitTrue(Duration.ofMillis(500), null, () -> !cache.containsKey("z", false));
    }

    @Test
    void shutdown_is_idempotent() {
        cache.shutdown();
        cache.shutdown();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
