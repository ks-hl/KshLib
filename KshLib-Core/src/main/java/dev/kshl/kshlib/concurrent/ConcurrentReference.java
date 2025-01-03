package dev.kshl.kshlib.concurrent;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ThrowingConsumer;
import dev.kshl.kshlib.function.ThrowingFunction;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConcurrentReference<T> {
    @Nonnull
    private final T t;
    private final ReentrantLock lock = new ReentrantLock();
    private Thread currentlyHeldBy;
    private long heldSince;

    public ConcurrentReference(@Nonnull T t) {
        //noinspection ConstantValue
        if (t == null) throw new NullPointerException();
        this.t = t;
    }

    public void consume(Consumer<T> consumer, long waitLimit) throws BusyException {
        //noinspection ResultOfMethodCallIgnored
        function(map -> {
            consumer.accept(map);
            return null;
        }, waitLimit);
    }

    public void consume(Consumer<T> consumer) {
        try {
            consume(consumer, -1);
        } catch (BusyException ignored) {
        }
    }

    public void consumeForce(Consumer<T> consumer) {
        //noinspection ResultOfMethodCallIgnored
        functionForce(reference -> {
            consumer.accept(reference);
            return null;
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    @CheckReturnValue
    public <V> V functionForce(Function<T, V> function) {
        try {
            return function(function, 0, true);
        } catch (BusyException ignored) {
            return null;
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @CheckReturnValue
    public <V> V function(Function<T, V> function, long waitLimit) throws BusyException {
        return function(function, waitLimit, false);
    }

    @SuppressWarnings("UnstableApiUsage")
    @CheckReturnValue
    public <V> V function(Function<T, V> function) {
        try {
            return function(function, 5000L);
        } catch (BusyException ignored) {
            return null;
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @CheckReturnValue
    public <V, E extends Exception> V functionThrowing(ThrowingFunction<T, V, E> function, long waitLimit) throws E, BusyException {
        return functionThrowing(function, waitLimit, false);
    }

    @SuppressWarnings("UnstableApiUsage")
    @CheckReturnValue
    public <V, E extends Exception> V functionThrowing(ThrowingFunction<T, V, E> function, long waitLimit, boolean force) throws E, BusyException {
        if (!force) try {
            if (waitLimit < 0) lock.lock();
            else if (!lock.tryLock(waitLimit, TimeUnit.MILLISECONDS)) {
                if (currentlyHeldBy == null) throw new BusyException("Resource busy, no holder");
                throw new BusyException(currentlyHeldBy.getStackTrace(), currentlyHeldBy.getId(), heldSince);
            }
        } catch (InterruptedException e) {
            return null;
        }
        currentlyHeldBy = Thread.currentThread();
        heldSince = System.currentTimeMillis();
        try {
            return function.apply(t);
        } finally {
            currentlyHeldBy = null;
            heldSince = 0;
            if (!force) lock.unlock();
        }
    }


    public <E extends Exception> void consumeThrowing(ThrowingConsumer<T, E> consumer, long waitLimit) throws E, BusyException {
        //noinspection ResultOfMethodCallIgnored
        functionThrowing(map -> {
            consumer.accept(map);
            return null;
        }, waitLimit, false);
    }

    @Override
    public int hashCode() {
        return function(Object::hashCode);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ConcurrentReference<?> concurrentReference)) return false;
        return function(t -> t.equals(concurrentReference));
    }

    private <V> V function(Function<T, V> function, long waitLimit, boolean force) throws BusyException {
        return functionThrowing(function::apply, waitLimit, force);
    }

    public int getHoldCount() {
        return lock.getHoldCount();
    }
}
