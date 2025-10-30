package dev.kshl.kshlib.misc;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class ExpiringReference<T> {
    private final long ttl;
    private T value;
    private long timeSet;

    public ExpiringReference(T t, long ttl) {
        this.ttl = ttl;
        set(t);
    }

    public synchronized void set(@Nullable T value) {
        this.value = value;
        timeSet = System.currentTimeMillis();
    }

    public synchronized @Nullable T get() {
        if (value == null) return null;
        if (isExpired()) return null;
        return value;
    }

    public synchronized T modifyAndGet(UnaryOperator<T> modifier) {
        T t;
        set(t = modifier.apply(get()));
        return t;
    }

    public synchronized void modify(UnaryOperator<T> modifier) {
        modifyAndGet(modifier);
    }

    public synchronized T computeIfNull(Supplier<T> supplier) {
        T value = get();
        if (value != null) return get();
        set(value = supplier.get());
        return value;
    }

    public synchronized boolean isExpired() {
        return System.currentTimeMillis() - timeSet > ttl;
    }
}
