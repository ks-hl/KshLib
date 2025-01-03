package dev.kshl.kshlib.concurrent;

import java.util.HashSet;

public class ConcurrentHashSet<E> extends ConcurrentCollection<HashSet<E>, E> {
    public ConcurrentHashSet() {
        super(new HashSet<>());
    }
}
