package dev.kshl.kshlib.misc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class CaseInsensitiveMap<V> implements Map<CaseInsensitiveMap.LowerString, V> {
    private final HashMap<CaseInsensitiveMap.LowerString, V> map = new HashMap<>();

    public V put(String key, V value) {
        return put(new LowerString(key), value);
    }

    public boolean containsKey(String key) {
        return containsKey(new LowerString(key));
    }

    public V get(String key) {
        return get(new LowerString(key));
    }

    public V computeIfAbsent(String key, Function<String, V> mappingFunction) {
        return computeIfAbsent(new LowerString(key), lowerString -> mappingFunction.apply(lowerString.value));
    }

    public V remove(String key) {
        return remove(new LowerString(key));
    }

    public void putAllStrings(Map<? extends String, V> put) {
        put.forEach(this::put);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Nullable
    @Override
    public V put(LowerString key, V value) {
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(@Nonnull Map<? extends LowerString, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Nonnull
    @Override
    public Set<LowerString> keySet() {
        return map.keySet();
    }

    @Nonnull
    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Nonnull
    @Override
    public Set<Entry<LowerString, V>> entrySet() {
        return map.entrySet();
    }

    public static class LowerString {
        @Nonnull
        private final String value;

        public LowerString(@Nonnull String value) {
            this.value = Objects.requireNonNull(value, "Value can not be null");
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof LowerString string) {
                return value.equalsIgnoreCase(string.value);
            }
            if (other instanceof String string) {
                return value.equalsIgnoreCase(string);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return value.toLowerCase().hashCode();
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Override
    public String toString() {
        return "CaseInsensitiveMap[" + map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).reduce((a, b) -> a + "," + b).orElse("") + "]";
    }
}
