package dev.kshl.kshlib.misc;

import org.junit.jupiter.api.Test;

import java.util.Objects;

public class TestMapCache {
    @Test
    public void testBiDiMap() throws InterruptedException {
        BiDiMapCache<Integer, Integer> map = new BiDiMapCache<>(15, 0, true);
        int key = 0;
        map.put(key++, key);
        map.put(key++, key);
        map.put(key, key);
        //noinspection ExcessiveLambdaUsage
        map.computeIfAbsent(69, k -> 420);
        map.computeKeyIfAbsent(421, v -> 70);
        assert map.size() == 5;
        assert Objects.requireNonNull(map.get(69)) == 420;
        assert Objects.requireNonNull(map.getKey(421)) == 70;
        assert map.getKey(420) == 69;
        Thread.sleep(20);
        assert map.isEmpty();
    }

    @Test
    public void testMap() throws InterruptedException {
        MapCache<Integer, Integer> map = new MapCache<>(15, 0, true);
        int key = 0;
        map.put(key++, key);
        map.put(key++, key);
        map.put(key, key);
        //noinspection ExcessiveLambdaUsage
        map.computeIfAbsent(69, k -> 420);
        assert map.size() == 4;
        assert Objects.requireNonNull(map.get(69)) == 420;
        Thread.sleep(20);
        assert map.isEmpty();
    }
}
