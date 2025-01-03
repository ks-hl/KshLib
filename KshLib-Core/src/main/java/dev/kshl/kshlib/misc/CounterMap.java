package dev.kshl.kshlib.misc;

import java.util.HashMap;

public class CounterMap<K> extends HashMap<K, Integer> {
    public synchronized void increment(K k) {
        int count = getOrDefault(k, 0);
        put(k, count + 1);
    }
}
