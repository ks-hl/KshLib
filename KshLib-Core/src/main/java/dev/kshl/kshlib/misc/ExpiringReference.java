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
    }

    public void set(@Nullable T value) {
        this.value = value;
        timeSet = System.currentTimeMillis();
    }

    public @Nullable T get() {
        synchronized (this) {
            if (value == null) return null;
            if (System.currentTimeMillis() - timeSet > ttl) {
                set(null);
            }
            return value;
        }
    }

    public T modifyAndGet(UnaryOperator<T> modifier) {
        synchronized (this) {
            T t;
            set(t = modifier.apply(get()));
            return t;
        }
    }

    public void modify(UnaryOperator<T> modifier) {
        modifyAndGet(modifier);
    }

    public T computeIfNull(Supplier<T> supplier) {
        synchronized (this) {
            T value = get();
            if (value != null) return get();
            set(value = supplier.get());
            return value;
        }
    }
}
