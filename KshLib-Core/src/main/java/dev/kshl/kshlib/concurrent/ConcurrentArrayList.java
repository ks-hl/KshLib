package dev.kshl.kshlib.concurrent;

import java.util.ArrayList;

public class ConcurrentArrayList<E> extends ConcurrentCollection<ArrayList<E>, E> {
    public ConcurrentArrayList() {
        super(new ArrayList<>());
    }
}
