package dev.kshl.kshlib.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ConcurrentCollection<C extends Collection<V>, V> extends ConcurrentReference<C> {

    public ConcurrentCollection(C collection) {
        super(collection);
    }

    public void forEach(Consumer<V> consumer) {
        consume(collection -> collection.forEach(consumer));
    }

    public boolean add(V v) {
        return function(collection -> collection.add(v));
    }

    public boolean remove(V v) {
        return function(collection -> collection.remove(v));
    }

    public boolean addAll(Collection<V> add) {
        return function(collection -> collection.addAll(add));
    }

    public boolean removeAll(Collection<V> remove) {
        return function(collection -> collection.removeAll(remove));
    }

    public boolean removeIf(Predicate<V> filter) {
        return function(collection -> collection.removeIf(filter));
    }

    public boolean contains(V player) {
        return function(collection -> collection.contains(player));
    }

    public boolean isEmpty() {
        return function(Collection::isEmpty);
    }

    public void clear() {
        consume(Collection::clear);
    }

    public int size() {
        return function(Collection::size);
    }

    @Override
    public int hashCode() {
        return function(collection -> collection.stream().mapToInt(Object::hashCode).reduce(0, (a, b) -> a ^ b));
    }

    public void forEachCopied(Consumer<V> consumer) {
        List<V> copy = new ArrayList<>();
        consume(copy::addAll);
        copy.forEach(consumer);
    }
}
