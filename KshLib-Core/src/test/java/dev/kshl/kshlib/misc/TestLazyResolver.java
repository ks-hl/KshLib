package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLazyResolver {
    @Test
    public void testLazyResolver() {
        AtomicInteger hitCount = new AtomicInteger();
        LazyResolver<String> lazyResolver = new LazyResolver<>(() -> {
            if (hitCount.incrementAndGet() > 1) throw new IllegalArgumentException("Supplier called more than once");
            return "hi";
        });

        assertFalse(lazyResolver.isResolved());
        assertNull(lazyResolver.get(false));

        assertEquals("hi", lazyResolver.get());
        assertTrue(lazyResolver.isResolved());

        lazyResolver.get();
    }
}
