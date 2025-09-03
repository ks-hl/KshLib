package dev.kshl.kshlib.misc;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class MapCache<K, V> extends HashMap<K, V> {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    protected final long timeToLive;
    protected final long cleanupInterval;
    protected final boolean updateWhenAccessed;
    private final HashMap<K, V> forward = new HashMap<>();
    private final ArrayDeque<Pair<Object, Long>> timeAddedQueue = new ArrayDeque<>();
    private final Map<Object, Long> trueTimeAdded = new HashMap<>();

    public MapCache(long timeToLive, long cleanupInterval, boolean updateWhenAccessed) {
        this.timeToLive = timeToLive;
        this.cleanupInterval = cleanupInterval;
        this.updateWhenAccessed = updateWhenAccessed;
        executor.scheduleAtFixedRate(this::cleanup, cleanupInterval, cleanupInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void clear() {
        forward.clear();
        timeAddedQueue.clear();
        trueTimeAdded.clear();
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
            touch(key);
        }
        return value;
    }

    protected final void touch(Object key) {
        long expiration = System.currentTimeMillis() + timeToLive;
        timeAddedQueue.add(new Pair<>(key, expiration));
        trueTimeAdded.put(key, expiration);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public V put(K key, V value) {
        touch(key);
        return forward.put(key, value);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public V remove(Object key) {
        trueTimeAdded.remove(key);
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

    @OverridingMethodsMustInvokeSuper
    protected List<V> cleanup() {
        final long currentTime = System.currentTimeMillis();
        List<V> removed = new ArrayList<>();

        Pair<Object, Long> element;
        while ((element = timeAddedQueue.peek()) != null) {
            if (currentTime >= element.getValue()) timeAddedQueue.poll();
            else break;
            Long trueExpiration = trueTimeAdded.get(element.getKey());
            if (trueExpiration != null && currentTime >= trueExpiration) {
                removed.add(remove(element.getKey()));
            }
        }
        return removed;
    }

    @Override
    public final boolean isEmpty() {
        cleanup();
        return forward.isEmpty();
    }
}
