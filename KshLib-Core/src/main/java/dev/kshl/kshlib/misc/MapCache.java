package dev.kshl.kshlib.misc;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    }

    protected final long timeToLive;
    final HashMap<K, V> forward = new HashMap<>();
    private final Map<Object, Long> lastTouch = new ConcurrentHashMap<>();

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private final ScheduledFuture<?> cleanupFuture;

    public MapCache(long timeToLive, TimeUnit timeUnit) {
        if (timeToLive <= 0) throw new IllegalArgumentException("timeToLive must be greater than 0");
        Objects.requireNonNull(timeUnit, "timeUnit must not be null");

        this.timeToLive = timeUnit.toMillis(timeToLive);
        if (this.timeToLive > TimeUnit.DAYS.toMillis(7)) throw new IllegalArgumentException("timeToLive must be less than 1 week");
        long interval = timeUnit.toNanos(timeToLive) / 10;
        cleanupFuture = executor.scheduleAtFixedRate(this::cleanup, interval, interval, TimeUnit.NANOSECONDS);
    }

    public MapCache(long timeToLiveMillis) {
        this(timeToLiveMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public final void clear() {
        lockWriteLock();
        try {
            forward.clear();
            lastTouch.clear();
            doClear();
        } finally {
            writeLock.unlock();
        }
    }

    protected void doClear() {
    }

    @Nonnull
    @Override
    public final Set<K> keySet() {
        readLock.lock();
        try {
            return Set.copyOf(forward.keySet());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public final int size() {
        readLock.lock();
        try {
            return forward.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public final boolean containsKey(Object key) {
        return containsKey(key, true);
    }

    public final boolean containsKey(Object key, boolean touch) {
        readLock.lock();
        try {
            //noinspection SuspiciousMethodCalls
            return forward.containsKey(key);
        } finally {
            readLock.unlock();
            if (touch) touch(key);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        readLock.lock();
        try {
            return forward.containsValue(value);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public final V get(Object key) {
        return get(key, true);
    }

    public final V get(Object key, boolean touch) {
        readLock.lock();
        V value = null;
        try {
            //noinspection SuspiciousMethodCalls
            return value = forward.get(key);
        } finally {
            readLock.unlock();

            if (touch && value != null) touch(key);
        }
    }

    public void touch(Object key) {
        if (!needTouch(key)) return;
        if (lock.getReadHoldCount() != 0) {
            throw new IllegalStateException("Cannot touch while holding a read lock");
        }
        lockWriteLock();
        try {
            //noinspection SuspiciousMethodCalls
            if (forward.containsKey(key)) {
                touchUnderWriteLock(key);
            }
        } finally {
            writeLock.unlock();
        }
    }

    protected boolean needTouch(Object key) {
        return System.currentTimeMillis() - lastTouch.getOrDefault(key, 0L) > Math.min(1000, timeToLive / 100);
    }

    protected final void touchUnderWriteLock(Object key) {
        lastTouch.put(key, System.currentTimeMillis());
    }

    @Override
    public final V put(K key, V value) {
        lockWriteLock();
        try {
            touchUnderWriteLock(key);
            var oldValue = forward.put(key, value);
            doPut(key, value, oldValue);
            return oldValue;
        } finally {
            writeLock.unlock();
        }
    }

    protected void doPut(K key, V value, V oldValue) {
    }


    @Override
    @OverridingMethodsMustInvokeSuper
    public V remove(Object key) {
        lockWriteLock();
        try {
            lastTouch.remove(key);
            V v = forward.remove(key);
            doRemove(key, v);
            return v;
        } finally {
            writeLock.unlock();
        }
    }

    protected void doRemove(Object key, V value) {
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void putAll(@Nonnull Map<? extends K, ? extends V> m) {
        lockWriteLock();
        try {
            for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public final V computeIfAbsent(K key, @Nonnull Function<? super K, ? extends V> mappingFunction) {
        try {
            readLock.lock();
            try {
                if (forward.containsKey(key)) return forward.get(key);
            } finally {
                readLock.unlock();
            }
            lockWriteLock();
            try {
                if (forward.containsKey(key)) return forward.get(key);

                V newVal = mappingFunction.apply(key);
                if (newVal == null) return null;
                put(key, newVal);
                return newVal;
            } finally {
                writeLock.unlock();
            }
        } finally {
            touch(key);
        }
    }

    @Override
    public final @Nonnull Set<Entry<K, V>> entrySet() {
        readLock.lock();
        try {
            return Map.copyOf(forward).entrySet();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public final @Nonnull Collection<V> values() {
        readLock.lock();
        try {
            return List.copyOf(forward.values());
        } finally {
            readLock.unlock();
        }
    }

    @OverridingMethodsMustInvokeSuper
    protected final void cleanup() {
        lockWriteLock();
        try {
            final long currentTime = System.currentTimeMillis();

            Set<Object> toRemove = new HashSet<>();
            Iterator<Entry<Object, Long>> it = lastTouch.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Object, Long> entry = it.next();
                if (currentTime - entry.getValue() > timeToLive) {
                    toRemove.add(entry.getKey());
                    it.remove();
                }
            }
            if (toRemove.isEmpty()) return;

            Map<Object, V> removed = new HashMap<>();
            toRemove.forEach(k -> removed.put(k, remove(k)));

            doCleanUp(removed);
        } finally {
            writeLock.unlock();
        }
    }

    protected void doCleanUp(Map<Object, V> removed) {
    }

    @Override
    public final boolean isEmpty() {
        readLock.lock();
        try {
            return forward.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    public void shutdown() {
        cleanupFuture.cancel(false);
    }

    private void lockWriteLock() {
        if (lock.getReadHoldCount() != 0) {
            throw new IllegalStateException("Cannot lock while holding a read lock");
        }
        writeLock.lock();
    }
}
