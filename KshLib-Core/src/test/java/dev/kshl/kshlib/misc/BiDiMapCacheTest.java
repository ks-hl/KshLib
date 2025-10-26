package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BiDiMapCacheTest {
    BiDiMapCache<String, Integer> cache;
    long ttl;

    @BeforeEach
    void setUp() {
        ttl = 80; // ms
        cache = new BiDiMapCache<>(ttl, TimeUnit.MILLISECONDS);
    }

    @AfterEach
    void tearDown() {
        cache.shutdown();
    }

    private static void awaitTrue(Duration timeout, Runnable tick, java.util.function.BooleanSupplier cond) {
        long end = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < end) {
            if (cond.getAsBoolean()) return;
            try { Thread.sleep(5); } catch (InterruptedException ignored) {}
            if (tick != null) tick.run();
        }
        fail("Condition not met within " + timeout.toMillis() + "ms");
    }

    @Test
    void reverse_contains_getKeys_getAnyKey() {
        cache.put("a", 1);
        cache.put("b", 1);
        cache.put("c", 2);

        assertTrue(cache.containsValue(1));
        Set<String> keys1 = cache.getKeys(1);
        assertEquals(Set.of("a", "b"), keys1);
        String any = cache.getAnyKey(1);
        assertTrue(Set.of("a", "b").contains(any));

        // removing one key keeps value present if others remain
        cache.remove("a");
        assertTrue(cache.containsValue(1));
        assertEquals(Set.of("b"), cache.getKeys(1));

        // removing last key clears reverse
        cache.remove("b");
        assertFalse(cache.containsValue(1));
        assertTrue(cache.getKeys(1).isEmpty());
        assertNull(cache.getAnyKey(1));
    }

    @Test
    void clear_resets_reverse() {
        cache.put("x", 10);
        cache.put("y", 10);
        assertTrue(cache.containsValue(10));
        cache.clear();
        assertTrue(cache.isEmpty());
        assertFalse(cache.containsValue(10));
        assertTrue(cache.getKeys(10).isEmpty());
    }

    @Test
    void ttl_expires_for_both_maps() {
        cache.put("k1", 100);
        cache.put("k2", 100);
        awaitTrue(Duration.ofMillis(600), null,
                () -> !cache.containsKey("k1") && !cache.containsKey("k2") && !cache.containsValue(100));
    }

    @Test
    void getKeys_touches_extending_ttl() {
        cache.put("p", 7);
        try { Thread.sleep(ttl / 2); } catch (InterruptedException ignored) {}
        // calling getKeys should touch internal keys
        assertEquals(Set.of("p"), cache.getKeys(7));

        // after another ~half ttl, still present due to touch
        try { Thread.sleep(ttl / 2); } catch (InterruptedException ignored) {}
        assertTrue(cache.containsKey("p"));

        // then wait > ttl again; now it should expire
        awaitTrue(Duration.ofMillis(600), null, () -> !cache.containsKey("p") && !cache.containsValue(7));
    }

    @Test
    void values_views_and_forward_consistency() {
        cache.put("m", 1);
        cache.put("n", 2);
        assertTrue(cache.containsValue(1));
        assertTrue(cache.values().containsAll(List.of(1, 2)));
        assertThrows(UnsupportedOperationException.class, () -> cache.values().add(3));
        // reverse updates on overwrite
        cache.put("m", 2);
        assertFalse(cache.containsValue(1));
        assertTrue(cache.containsValue(2));
        assertEquals(Set.of("m", "n"), cache.getKeys(2));
    }
}
