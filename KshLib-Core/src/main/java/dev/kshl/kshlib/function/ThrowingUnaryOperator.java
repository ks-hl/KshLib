package dev.kshl.kshlib.function;

@FunctionalInterface
public interface ThrowingUnaryOperator<T, E extends Exception> extends ThrowingFunction<T, T, E> {
}
