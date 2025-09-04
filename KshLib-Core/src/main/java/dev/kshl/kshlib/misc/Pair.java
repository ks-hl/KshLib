package dev.kshl.kshlib.misc;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class Pair<K, V> {
    private K left;
    private V right;

    public Pair(K left, V right) {
        this.left = left;
        this.right = right;
    }

    @Deprecated
    public K getKey() {
        return left;
    }

    @Deprecated
    public V getValue() {
        return right;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Pair<?, ?> pair)) return false;
        return Objects.equals(left, pair.left) && Objects.equals(right, pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    /**
     * Creates a new {@link Pair} with the same key and value
     */
    public Pair<K, V> copy() {
        return new Pair<>(getLeft(), getRight());
    }
}
