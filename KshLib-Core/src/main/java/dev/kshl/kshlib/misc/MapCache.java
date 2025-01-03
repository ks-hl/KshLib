package dev.kshl.kshlib.misc;

import javax.annotation.Nonnull;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class MapCache<K, V> extends HashMap<K, V> {

    protected final long timeToLive;
    protected final long cleanupInterval;
    protected final boolean updateWhenAccessed;
    private final HashMap<K, V> forward;
    private final HashMap<Object, Long> timeAdded;
    private long lastCleanup;

    public MapCache(long timeToLive, long cleanupInterval, boolean updateWhenAccessed) {
        forward = new HashMap<>();
        timeAdded = new HashMap<>();
        this.timeToLive = timeToLive;
        this.cleanupInterval = cleanupInterval;
        this.lastCleanup = System.currentTimeMillis();
        this.updateWhenAccessed = updateWhenAccessed;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void clear() {
        forward.clear();
        timeAdded.clear();
        this.lastCleanup = System.currentTimeMillis();
    }

    @Nonnull
    @Override
    public final Set<K> keySet() {
        cleanup();
        return forward.keySet();
    }

    @Override
    public final int size() {
        cleanup();
        return forward.size();
    }

    @Override
    public final boolean containsKey(Object key) {
        cleanup();
        return forward.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        cleanup();
        return forward.containsValue(value);
    }

    @Override
    public final V get(Object key) {
        V value = forward.get(key);
        cleanup();
        if (value == null) {
            return null;
        }

        if (updateWhenAccessed) {
            timeAdded.put(key, System.currentTimeMillis());
        }
        return value;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public V put(K key, V value) {
        cleanup();
        timeAdded.put(key, System.currentTimeMillis());
        return forward.put(key, value);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public V remove(Object key) {
        cleanup();
        timeAdded.remove(key);
        return forward.remove(key);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void putAll(@Nonnull Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public final V computeIfAbsent(K key, @Nonnull Function<? super K, ? extends V> mappingFunction) {
        V val = get(key);
        if (val != null) return val;
        V newVal = mappingFunction.apply(key);
        put(key, newVal);
        return newVal;
    }

    @Override
    public final @Nonnull Set<Entry<K, V>> entrySet() {
        cleanup();
        return forward.entrySet();
    }

    @Override
    public final @Nonnull Collection<V> values() {
        cleanup();
        return forward.values();
    }

    public final void cleanup() {
        if (System.currentTimeMillis() - lastCleanup > cleanupInterval) {
            actuallyCleanup();
            lastCleanup = System.currentTimeMillis();
        }
    }

    @OverridingMethodsMustInvokeSuper
    protected List<V> actuallyCleanup() {
        final long cutoff = System.currentTimeMillis() - timeToLive;

        ArrayList<Object> cleanup = new ArrayList<>();
        for (Entry<Object, Long> entry : timeAdded.entrySet()) {
            if (entry.getValue() < cutoff) {
                cleanup.add(entry.getKey());
            }
        }
        List<V> removed = new ArrayList<>();
        for (Object key : cleanup) {
            timeAdded.remove(key);
            //noinspection SuspiciousMethodCalls
            removed.add(forward.remove(key));
        }
        return removed;
    }

    @Override
    public final boolean isEmpty() {
        cleanup();
        return forward.isEmpty();
    }

    protected final void touch(K key) {
        timeAdded.put(key, System.currentTimeMillis());
    }
}
