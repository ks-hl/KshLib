package dev.kshl.kshlib.misc;

import java.util.Objects;

public class Pair<K, V> implements Cloneable {
    private final K k;
    private final V v;

    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public K getKey() {
        return k;
    }

    public V getValue() {
        return v;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Pair<?, ?> pair)) return false;
        return Objects.equals(k, pair.k) && Objects.equals(v, pair.v);
    }

    @Override
    public int hashCode() {
        return Objects.hash(k, v);
    }

    /**
     * Creates a new {@link Pair} with the same key and value
     */
    public Pair<K, V> copy() {
        return new Pair<>(getKey(), getValue());
    }
}
