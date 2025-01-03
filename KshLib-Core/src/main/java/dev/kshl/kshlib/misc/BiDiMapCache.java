package dev.kshl.kshlib.misc;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class BiDiMapCache<K, V> extends MapCache<K, V> {
    private final HashMap<V, K> reverse;

    public BiDiMapCache(long timeToLive, long cleanupInterval, boolean updateWhenAccessed) {
        super(timeToLive, cleanupInterval, updateWhenAccessed);
        reverse = new HashMap<>();
    }

    @Override
    public void clear() {
        super.clear();
        reverse.clear();
    }

    @Override
    public boolean containsValue(Object value) {
        cleanup();
        //noinspection SuspiciousMethodCalls
        return reverse.containsKey(value);
    }

    public K getKey(V value) {
        K key = reverse.get(value);
        cleanup();
        if (key == null) return null;
        if (updateWhenAccessed) touch(key);
        return key;
    }

    @Override
    public V put(K key, V value) {
        reverse.put(value, key);
        return super.put(key, value);
    }

    public K computeKeyIfAbsent(V key, @Nonnull Function<? super V, ? extends K> mappingFunction) {
        K val = getKey(key);
        cleanup();
        if (val != null) return val;
        K newVal = mappingFunction.apply(key);
        put(newVal, key);
        return newVal;
    }

    protected final List<V> actuallyCleanup() {
        super.actuallyCleanup().forEach(reverse::remove);
        return null;
    }
}
