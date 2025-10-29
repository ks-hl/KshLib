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

public final class ConcurrentArrayList<E> implements List<E> {
    private final ReentrantReadWriteLock.ReadLock r;
    private final ReentrantReadWriteLock.WriteLock w;
    private final ArrayList<E> list = new ArrayList<>();

    public ConcurrentArrayList() {
        this(null, true);
    }

    public ConcurrentArrayList(boolean fair) {
        this(null, fair);
    }

    public ConcurrentArrayList(@Nullable Collection<E> contents) {
        this(contents, true);
    }

    public ConcurrentArrayList(@Nullable Collection<E> contents, boolean fair) {
        var rw = new ReentrantReadWriteLock(fair);
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
    public boolean isEmpty() {
        r.lock();
        try {
            return list.isEmpty();
        } finally {
            r.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        r.lock();
        try {
            return list.contains(o);
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
    public boolean remove(Object o) {
        w.lock();
        try {
            return list.remove(o);
        } finally {
            w.unlock();
        }
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> collection) {
        r.lock();
        try {
            return list.containsAll(collection);
        } finally {
            r.unlock();
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
    public int indexOf(Object o) {
        r.lock();
        try {
            return list.indexOf(o);
        } finally {
            r.unlock();
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        r.lock();
        try {
            return list.lastIndexOf(o);
        } finally {
            r.unlock();
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
    public boolean addAll(int i, @Nonnull Collection<? extends E> collection) {
        w.lock();
        try {
            return list.addAll(i, collection);
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
    public Object[] toArray() {
        return snapshot().toArray();
    }

    @Override
    @Nonnull
    public <T> T[] toArray(@Nonnull T[] ts) {
        return snapshot().toArray(ts);
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
    public boolean retainAll(@Nonnull Collection<?> collection) {
        w.lock();
        try {
            return list.retainAll(collection);
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
