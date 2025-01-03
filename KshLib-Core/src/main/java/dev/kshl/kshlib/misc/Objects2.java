package dev.kshl.kshlib.misc;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

public class Objects2 {
    /**
     * Similar to {@link Objects#requireNonNullElseGet(Object, Supplier)}, but allows the supplier to return null.
     *
     * @param value    The value to return if it's not null.
     * @param supplier The supplier which will provide an alternative output if value is null
     * @return Either value if it's nonnull, or supplier.get().
     * @deprecated Use {@link java.util.Optional#orElseGet(Supplier)} (Function)}
     */
    @Deprecated
    @Nullable
    public static <T> T requireNonNullElseGet(@Nullable T value, Supplier<? extends T> supplier) {
        if (value != null) return value;
        return Objects.requireNonNull(supplier, "supplier").get();
    }

    /**
     * Similar to {@link Objects#requireNonNullElseGet(Object, Supplier)}, but allows the supplier to return null.
     *
     * @param value    The value to return if it's not null.
     * @param supplier The supplier which will provide an alternative output if value is null
     * @return Either value if it's nonnull, or supplier.get().
     */
    @Nullable
    public static <T, E extends Exception> T requireNonNullElseGetThrows(@Nullable T value, ThrowingSupplier<? extends T, E> supplier) throws E {
        if (value != null) return value;
        return Objects.requireNonNull(supplier, "supplier").get();
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }

    /**
     * If the value is null, returns null, otherwise, apply the mapping function.
     *
     * @param value           The value
     * @param mappingFunction The function applied to the value if not null
     * @return The mapped value, or null
     * @deprecated Use {@link java.util.Optional#map(Function)}
     */
    @Deprecated
    public static <T, R> R mapIfNotNull(T value, Function<T, R> mappingFunction) {
        if (value == null) return null;
        return mappingFunction.apply(value);
    }

    /**
     * If the value is null, nothing happens, otherwise, the consumer is called with the value.
     *
     * @param value    The value
     * @param consumer What to do with the value if it's not null
     * @deprecated Use {@link java.util.Optional#ifPresent(Consumer)}
     */
    @Deprecated
    public static <T> void consumeIfNotNull(T value, Consumer<T> consumer) {
        if (value != null) consumer.accept(value);
    }

    /**
     * Hashes the provided collection in an order agnostic method, using XOR
     */
    public static <T> int hash(Collection<T> collection) {
        return hash(collection, Object::hashCode);
    }

    /**
     * Hashes the provided collection in an order agnostic method, using XOR
     *
     * @param hashingFunction The method to use to compute the hash for each entry
     */
    public static <T> int hash(Collection<T> collection, ToIntFunction<T> hashingFunction) {
        return hash_(collection.stream(), hashingFunction);
    }

    /**
     * Equivalent to {@link Objects#hash(Object...)}, but order independent
     */
    public static int hash(Object... elements) {
        return hash_(Arrays.stream(elements), Object::hashCode);
    }

    private static <T> int hash_(Stream<T> stream, ToIntFunction<T> hashingFunction) {
        return stream.mapToInt(hashingFunction).reduce(0, (a, b) -> a ^ b);
    }
}
