package dev.kshl.kshlib.misc;

import java.sql.Time;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BiDiMapCache<K, V> extends MapCache<K, V> {
    private final Map<V, Set<K>> reverse = new HashMap<>();

    public BiDiMapCache(long timeToLive, TimeUnit timeUnit) {
        super(timeToLive, timeUnit);
    }

    @Override
    protected void doClear() {
        reverse.clear();
    }

    @Override
    public boolean containsValue(Object value) {
        r.lock();
        try {
            //noinspection SuspiciousMethodCalls
            return reverse.containsKey(value);
        } finally {
            r.unlock();
        }
    }

    public K getAnyKey(V value) {
        var keys = getKeys(value);
        if (keys.isEmpty()) return null;
        return keys.iterator().next();
    }

    public Set<K> getKeys(V value) {
        r.lock();
        Set<K> keys;
        try {
            keys = reverse.get(value);
            if (keys == null) keys = Set.of();
            keys = Set.copyOf(keys);
        } finally {
            r.unlock();
        }

        if (!keys.isEmpty()) {
            w.lock();
            try {
                for (K key : keys) {
                    if (!forward.containsKey(key)) continue;
                    touchUnderWriteLock(key);
                }
            } finally {
                w.unlock();
            }
        }
        return keys;
    }

    @Override
    protected void doPut(K key, V value, V oldValue) {
        doRemove(key, oldValue);
        reverse.computeIfAbsent(value, v -> new HashSet<>()).add(key);
    }

    @Override
    protected void doCleanUp(Map<Object, V> removed) {
        removed.forEach(this::doRemove);
    }

    @Override
    protected void doRemove(Object key, V value) {
        var keys = reverse.get(value);
        if (keys != null) {
            //noinspection SuspiciousMethodCalls
            keys.remove(key);
            if (keys.isEmpty()) reverse.remove(value);
        }
    }
}
