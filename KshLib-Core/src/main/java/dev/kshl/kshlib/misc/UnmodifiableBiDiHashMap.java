package dev.kshl.kshlib.misc;

import java.util.Map;
import java.util.Set;

public class UnmodifiableBiDiHashMap<K, V> extends BiDiHashMap<K, V> {
    private final boolean initComplete;

    public UnmodifiableBiDiHashMap() {
        super();
        initComplete = true;
    }

    public UnmodifiableBiDiHashMap(Map<K, V> map) {
        super(map);
        initComplete = true;
    }

    @Override
    public synchronized void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized V put(K key, V value) {
        if (initComplete) {
            throw new UnsupportedOperationException();
        } else {
            return super.put(key, value);
        }
    }

    @Override
    public synchronized V remove(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Set<K> removeValue(V value) {
        throw new UnsupportedOperationException();
    }

}
