package dev.kshl.kshlib.misc;

import java.util.HashMap;

public class CounterMap<K> extends HashMap<K, Long> {
    public synchronized void increment(K k) {
        add(k, 1);
    }

    public synchronized void decrement(K k) {
        add(k, -1);
    }

    public synchronized void add(K k, long add) {
        long count = getOrDefault(k, 0L);
        put(k, count + add);
    }
}
