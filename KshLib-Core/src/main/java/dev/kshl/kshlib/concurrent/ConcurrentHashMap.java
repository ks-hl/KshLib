package dev.kshl.kshlib.concurrent;

import java.util.HashMap;

public class ConcurrentHashMap<K, V> extends ConcurrentMap<HashMap<K, V>, K, V> {
    public ConcurrentHashMap(HashMap<K, V> map) {
        super(map);
    }

    public ConcurrentHashMap() {
        super(new HashMap<>());
    }
}
