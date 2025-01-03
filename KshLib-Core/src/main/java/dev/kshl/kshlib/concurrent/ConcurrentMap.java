package dev.kshl.kshlib.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ConcurrentMap<M extends Map<K, V>, K, V> extends ConcurrentReference<M> {

    public ConcurrentMap(M map) {
        super(map);
    }

    public void forEach(BiConsumer<K, V> consumer) {
        consume(map -> map.forEach(consumer));
    }

    public V put(K k, V v) {
        return function(map -> map.put(k, v));
    }

    public V remove(K key) {
        return function(map -> map.remove(key));
    }

    public void putAll(Map<K, V> add) {
        consume(map -> map.putAll(add));
    }

    public boolean containsKey(K key) {
        return function(map -> map.containsKey(key));
    }

    public boolean isEmpty() {
        return function(Map::isEmpty);
    }

    public void clear() {
        consume(Map::clear);
    }

    public V get(K key) {
        return function(map -> map.get(key));
    }

    public V computeIfAbsent(K key, Function<K, V> mapper) {
        return function(map -> map.computeIfAbsent(key, mapper));
    }

    public boolean removeIfValues(Predicate<V> filter) {
        return function(map -> map.values().removeIf(filter));
    }

    public boolean removeIfKeys(Predicate<K> filter) {
        return function(map -> map.keySet().removeIf(filter));
    }

    @Override
    public int hashCode() {
        return function(map -> map.entrySet().stream().mapToInt(Object::hashCode).reduce(0, (a, b) -> a ^ b));
    }

    public void forEachCopied(BiConsumer<K, V> consumer) {
        Map<K, V> copy = new HashMap<>();
        consume(copy::putAll);
        copy.forEach(consumer);
    }
}
