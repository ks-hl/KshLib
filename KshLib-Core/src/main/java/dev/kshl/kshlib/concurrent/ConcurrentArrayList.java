package dev.kshl.kshlib.concurrent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentArrayList<E> implements List<E> {
    final ReentrantReadWriteLock.ReadLock r;
    final ReentrantReadWriteLock.WriteLock w;
    private final ArrayList<E> list = new ArrayList<>();

    public ConcurrentArrayList() {
        this(null);
    }

    public ConcurrentArrayList(@Nullable Collection<E> contents) {
        var rw = new ReentrantReadWriteLock();
        r = rw.readLock();
        w = rw.writeLock();

        if (contents != null) this.addAll(contents);
    }

    @Override
    public int size() {
        r.lock();
        try {
            return list.size();
        } finally {
            r.unlock();
        }
    }

    @Override
    public E get(int i) {
        r.lock();
        try {
            return list.get(i);
        } finally {
            r.unlock();
        }
    }

    @Override
    public boolean add(E e) {
        w.lock();
        try {
            return list.add(e);
        } finally {
            w.unlock();
        }
    }

    @Override
    public E set(int index, E element) {
        w.lock();
        try {
            return list.set(index, element);
        } finally {
            w.unlock();
        }
    }

    @Override
    public void add(int index, E element) {
        w.lock();
        try {
            list.add(index, element);
        } finally {
            w.unlock();
        }
    }

    @Override
    public E remove(int index) {
        w.lock();
        try {
            return list.remove(index);
        } finally {
            w.unlock();
        }
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        w.lock();
        try {
            return list.addAll(c);
        } finally {
            w.unlock();
        }
    }

    @Override
    public void clear() {
        w.lock();
        try {
            list.clear();
        } finally {
            w.unlock();
        }
    }

    @Override
    @Nonnull
    public Iterator<E> iterator() {
        return snapshot().iterator();
    }

    @Override
    @Nonnull
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    @Nonnull
    public ListIterator<E> listIterator(int index) {
        return snapshot().listIterator(index);
    }

    @Override
    @Nonnull
    public List<E> subList(int fromIndex, int toIndex) {
        return snapshot().subList(fromIndex, toIndex);
    }

    public List<E> snapshot() {
        r.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(list));
        } finally {
            r.unlock();
        }
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        w.lock();
        try {
            return list.removeAll(c);
        } finally {
            w.unlock();
        }
    }

    @Override
    public String toString() {
        r.lock();
        try {
            return list.toString();
        } finally {
            r.unlock();
        }
    }

    @Override
    public int hashCode() {
        r.lock();
        try {
            return list.hashCode();
        } finally {
            r.unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof List<?>)) return false;
        r.lock();
        try {
            return list.equals(o);
        } finally {
            r.unlock();
        }
    }


}
