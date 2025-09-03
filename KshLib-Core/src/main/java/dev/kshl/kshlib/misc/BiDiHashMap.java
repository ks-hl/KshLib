package dev.kshl.kshlib.misc;

import jdk.jfr.Experimental;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@Experimental
public class BiDiHashMap<K, V> {
    private final HashMap<K, V> forward;
    private final HashMap<V, Set<K>> reverse;

    public BiDiHashMap() {
        forward = new HashMap<>();
        reverse = new HashMap<>();
    }

    public BiDiHashMap(Map<K, V> map) {
        this();
        putAll(map);
    }

    public synchronized void clear() {
        forward.clear();
        reverse.clear();
    }

    public synchronized boolean containsValue(Object value) {
        //noinspection SuspiciousMethodCalls
        return reverse.containsKey(value);
    }

    public synchronized boolean containsKey(Object value) {
        //noinspection SuspiciousMethodCalls
        return forward.containsKey(value);
    }

    public synchronized Set<K> getKey(V value) {
        return reverse.getOrDefault(value, Set.of());
    }

    public synchronized V put(K key, V value) {
        if (key == null) throw new NullPointerException("Key can not be null");
        if (value == null) throw new NullPointerException("Value can not be null");
        reverse.computeIfAbsent(value, v -> new HashSet<>()).add(key);
        V removed = forward.put(key, value);
        reverse.computeIfPresent(removed, (k, set) -> {
            set.remove(key);
            if (set.isEmpty()) return null;
            return set;
        });
        return removed;
    }

    public void putAll(Map<K, V> map) {
        map.forEach(this::put);
    }

    public synchronized V get(K key) {
        return forward.get(key);
    }

    public synchronized V remove(K key) {
        V removed = forward.remove(key);
        reverse.computeIfPresent(removed, (k, set) -> {
            set.remove(key);
            if (set.isEmpty()) return null;
            return set;
        });
        return removed;
    }

    public synchronized Set<K> removeValue(V value) {
        Set<K> removed = reverse.remove(value);
        removed.forEach(forward::remove);
        return removed;
    }

    public synchronized Set<K> keySet() {
        return Collections.unmodifiableSet(forward.keySet());
    }

    public synchronized Collection<V> values() {
        return Collections.unmodifiableCollection(forward.values());
    }

    public synchronized Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(forward.entrySet());
    }

    public synchronized void forEach(BiConsumer<? super K, ? super V> consumer) {
        forward.forEach(consumer);
    }
}
