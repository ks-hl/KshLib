package dev.kshl.kshlib.misc;

import java.util.Optional;
import java.util.function.Supplier;

public class LazyResolver<T> {
    private final Supplier<T> resolver;
    private boolean resolved;
    private T value;

    public LazyResolver(Supplier<T> resolver) {
        this.resolver = resolver;
    }

    public T get(boolean resolve) {
        if (resolved || !resolve) return value;

        value = resolver.get();
        resolved = true;
        return value;
    }

    public T get() {
        return get(true);
    }

    public Optional<T> getOpt(boolean resolve) {
        return Optional.ofNullable(get(resolve));
    }

    public Optional<T> getOpt() {
        return getOpt(true);
    }

    public void set(T value) {
        this.value = value;
        this.resolved = true;
    }

    public boolean isResolved() {
        return this.resolved;
    }
}
