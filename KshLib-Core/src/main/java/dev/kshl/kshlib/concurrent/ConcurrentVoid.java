package dev.kshl.kshlib.concurrent;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.function.ThrowingRunnable;
import dev.kshl.kshlib.function.ThrowingSupplier;

import java.util.function.Supplier;

public class ConcurrentVoid extends ConcurrentReference<Object> {

    public ConcurrentVoid() {
        super(new Object());
    }

    public void run(Runnable run) {
        consume(o -> run.run());
    }

    public <T> T get(Supplier<T> supplier) {
        return function(o -> supplier.get());
    }

    public void run(Runnable run, long wait) throws BusyException {
        consume(o -> run.run(), wait);
    }

    public <T> T get(Supplier<T> supplier, long wait) throws BusyException {
        return function(o -> supplier.get(), wait);
    }

    public <E extends Exception> void runThrowing(ThrowingRunnable<E> run, long wait) throws E, BusyException {
        consumeThrowing(o -> run.run(), wait);
    }

    public <T, E extends Exception> T get(ThrowingSupplier<T, E> supplier, long wait) throws E, BusyException {
        return functionThrowing(o -> supplier.get(), wait);
    }
}
