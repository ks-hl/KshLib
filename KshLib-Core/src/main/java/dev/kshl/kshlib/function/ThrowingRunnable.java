package dev.kshl.kshlib.function;

@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> {
    void run() throws E;
}
