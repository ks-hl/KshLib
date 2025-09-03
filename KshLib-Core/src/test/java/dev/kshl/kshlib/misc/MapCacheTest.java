package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCacheTest {
    @Test
    void testExpiration() throws InterruptedException {
        MapCache<String, String> cache = new MapCache<>(20, 5, false);
        cache.put("key1", "value1");

        assertEquals("value1", cache.get("key1"));

        TimeUnit.MILLISECONDS.sleep(30); // wait past TTL
        cache.cleanup();

        assertNull(cache.get("key1"));
        assertFalse(cache.containsKey("key1"));
        assertEquals(0, cache.size());
    }

    @Test
    void testUpdateWhenAccessedExtendsLife() throws InterruptedException {
        MapCache<String, String> cache = new MapCache<>(20, 5, true);
        cache.put("key1", "value1");

        TimeUnit.MILLISECONDS.sleep(15);
        assertEquals("value1", cache.get("key1")); // access should extend TTL

        TimeUnit.MILLISECONDS.sleep(15);
        assertEquals("value1", cache.get("key1")); // still valid because of touch
    }

    @Test
    void testCleanupReturnsExpiredValues() throws InterruptedException {
        MapCache<String, String> cache = new MapCache<>(20, 5, false);
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        TimeUnit.MILLISECONDS.sleep(25);

        assertTrue(cache.isEmpty());
    }
}
