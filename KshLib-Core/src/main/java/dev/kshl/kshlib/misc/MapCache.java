package dev.kshl.kshlib.misc;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class MapCache<K, V> extends AbstractMap<K, V> {
    private static final AtomicInteger cleanupThreadId = new AtomicInteger(0);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3, r -> {
        Thread t = new Thread(r, "KshLib-MapCache-Cleaner-" + cleanupThreadId.getAndIncrement());
        t.setDaemon(true);
        return t;
    });

    protected final long timeToLive;
    final HashMap<K, V> forward = new HashMap<>();

    private final ArrayDeque<Pair<Object, Long>> timeAddedQueue = new ArrayDeque<>();
    private final Map<Object, Long> lastTouch = new ConcurrentHashMap<>();

    final ReentrantReadWriteLock.ReadLock r;
    final ReentrantReadWriteLock.WriteLock w;

    private final ScheduledFuture<?> cleanupFuture;

    public MapCache(long timeToLive, TimeUnit timeUnit) {
        if (timeToLive <= 0) throw new IllegalArgumentException("timeToLive must be greater than 0");
        Objects.requireNonNull(timeUnit, "timeUnit must not be null");

        this.timeToLive = timeUnit.toMillis(timeToLive);
        if (this.timeToLive > TimeUnit.DAYS.toMillis(7)) throw new IllegalArgumentException("timeToLive must be less than 1 week");
        long interval = timeUnit.toNanos(timeToLive) / 10;
        cleanupFuture = executor.scheduleAtFixedRate(this::cleanup, interval, interval, TimeUnit.NANOSECONDS);

        var rw = new ReentrantReadWriteLock();
        r = rw.readLock();
        w = rw.writeLock();
    }

    public MapCache(long timeToLiveMillis) {
        this(timeToLiveMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public final void clear() {
        w.lock();
        try {
            forward.clear();
            timeAddedQueue.clear();
            lastTouch.clear();
            doClear();
        } finally {
            w.unlock();
        }
    }

    protected void doClear() {
    }

    @Nonnull
    @Override
    public final Set<K> keySet() {
        r.lock();
        try {
            return Set.copyOf(forward.keySet());
        } finally {
            r.unlock();
        }
    }

    @Override
    public final int size() {
        r.lock();
        try {
            return forward.size();
        } finally {
            r.unlock();
        }
    }

    @Override
    public final boolean containsKey(Object key) {
        r.lock();
        try {
            return forward.containsKey(key);
        } finally {
            r.unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        r.lock();
        try {
            return forward.containsValue(value);
        } finally {
            r.unlock();
        }
    }

    @Override
    public final V get(Object key) {
        r.lock();
        V value = null;
        try {
            return value = forward.get(key);
        } finally {
            r.unlock();

            if (value != null) {
                w.lock();
                try {
                    //noinspection SuspiciousMethodCalls
                    if (forward.containsKey(key)) {
                        touchUnderWriteLock(key);
                    }
                } finally {
                    w.unlock();
                }
            }
        }
    }

    protected boolean needTouch(Object key) {
        return System.currentTimeMillis() - lastTouch.getOrDefault(key, 0L) > 1000;
    }

    protected final void touchUnderWriteLock(Object key) {
        long expiration = System.currentTimeMillis() + timeToLive;
        timeAddedQueue.add(new Pair<>(key, expiration));
        lastTouch.put(key, System.currentTimeMillis());
    }

    @Override
    public final V put(K key, V value) {
        w.lock();
        try {
            touchUnderWriteLock(key);
            var oldValue = forward.put(key, value);
            doPut(key, value, oldValue);
            return oldValue;
        } finally {
            w.unlock();
        }
    }

    protected void doPut(K key, V value, V oldValue) {
    }


    @Override
    @OverridingMethodsMustInvokeSuper
    public V remove(Object key) {
        w.lock();
        try {
            lastTouch.remove(key);
            V v = forward.remove(key);
            doRemove(key, v);
            return v;
        } finally {
            w.unlock();
        }
    }

    protected void doRemove(Object key, V value) {
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void putAll(@Nonnull Map<? extends K, ? extends V> m) {
        w.lock();
        try {
            for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        } finally {
            w.unlock();
        }
    }

    @Override
    public final V computeIfAbsent(K key, @Nonnull Function<? super K, ? extends V> mappingFunction) {
        r.lock();
        try {
            if (forward.containsKey(key)) return forward.get(key);
        } finally {
            r.unlock();
        }
        w.lock();
        try {
            if (forward.containsKey(key)) return forward.get(key);

            V newVal = mappingFunction.apply(key);
            if (newVal == null) return null;
            put(key, newVal);
            return newVal;
        } finally {
            w.unlock();
        }
    }

    @Override
    public final @Nonnull Set<Entry<K, V>> entrySet() {
        r.lock();
        try {
            return Map.copyOf(forward).entrySet();
        } finally {
            r.unlock();
        }
    }

    @Override
    public final @Nonnull Collection<V> values() {
        r.lock();
        try {
            return List.copyOf(forward.values());
        } finally {
            r.unlock();
        }
    }

    @OverridingMethodsMustInvokeSuper
    protected final void cleanup() {
        w.lock();
        try {
            final long currentTime = System.currentTimeMillis();
            Map<Object, V> removed = new HashMap<>();

            Pair<Object, Long> element;
            while ((element = timeAddedQueue.peek()) != null) {
                if (currentTime < element.getRight()) break;

                timeAddedQueue.poll();

                if (currentTime >= lastTouch.getOrDefault(element.getLeft(), 0L) + timeToLive) {
                    removed.put(element.getLeft(), remove(element.getLeft()));
                }
            }
            doCleanUp(removed);
        } finally {
            w.unlock();
        }
    }

    protected void doCleanUp(Map<Object, V> removed) {
    }

    @Override
    public final boolean isEmpty() {
        r.lock();
        try {
            return forward.isEmpty();
        } finally {
            r.unlock();
        }
    }

    public void shutdown() {
        cleanupFuture.cancel(false);
    }
}
